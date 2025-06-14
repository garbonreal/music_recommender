package com.example.recommender

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.example.model.{MongoConfig, Recommendation, MusicRecs}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.typesafe.config.ConfigFactory
import redis.clients.jedis.Jedis

// Define connection helper object, serialization
object ConnHelper extends Serializable{
  val conf = ConfigFactory.load()
  lazy val jedis = new Jedis("localhost")
  lazy val mongoClient = MongoClient(MongoClientURI(conf.getString("app.mongo.uri")) )
}

object StreamingRecommender {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_MUSICS_NUM = 20
  val MONGODB_STREAM_RECS_COLLECTION = "StreamRecs"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_MUSIC_RECS_COLLECTION = "MusicRecs"

  def main(args: Array[String]): Unit = {
    
    val conf = ConfigFactory.load()

    val config = Map(
      "spark.cores" -> conf.getString("app.spark.cores"),
      "mongo.uri" -> conf.getString("app.mongo.uri"),
      "mongo.db" -> conf.getString("app.mongo.db"),
      "kafka.topic" -> conf.getString("app.kafka.topic")
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StreamingRecommender")

    // Build SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    // Get streaming context
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(2))    // batch duration

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // Load the music similarity matrix data and broadcast it
    val simMusicMatrix = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MUSIC_RECS_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MusicRecs]
      .rdd
      .map{ musicRecs =>
        (musicRecs.mid, musicRecs.recs.map( x=> (x.mid, x.score) ).toMap )
      }.collectAsMap()

    val simMusicMatrixBroadCast = sc.broadcast(simMusicMatrix)

    // Define kafka connection parameters
    val kafkaParam = Map(
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "MusicRecommender",
      "auto.offset.reset" -> "latest"
    )

// Create a DStream through kafka
    val kafkaStream = KafkaUtils.createDirectStream[String, String](ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(config("kafka.topic")), kafkaParam)
    )

    // Convert the original data UID|MID|TIMESTAMP into a scoring stream
    val ratingStream = kafkaStream.map {
      msg =>
        val attr = msg.value().split("MUSIC_LISTENING_PREFIX:").last.trim.split("\\|")
        (attr(0).toInt, attr(1).toInt, attr(2).toInt)
    }

    // Continue to do streaming processing, the core real-time algorithm part
    ratingStream.foreachRDD{
      rdds => rdds.foreach{
        case (uid, mid, timestamp) => {
          println("rating data coming! >>>>>>>>>>>>>>>>")

          ConnHelper.jedis.set("calculation_status:" + uid, "waiting");

          // 1. Get the most recent K ratings of the current user from redis and save them as Array[(mid, score)]
          val userRecentlyRatings = getUserRecentlyRating( MAX_USER_RATINGS_NUM, uid, ConnHelper.jedis )

          // 2. Take the N most similar movies to the current movie from the similarity matrix as a candidate list, Array[mid]
          val candidateMusics = getTopSimMusics( MAX_SIM_MUSICS_NUM, mid, uid, simMusicMatrixBroadCast.value )

          // 3. For each candidate movie, calculate the recommendation priority and obtain the current user’s real-time recommendation list, Array[(mid, score)]
          val streamRecs = computeMusicScores( candidateMusics, userRecentlyRatings, simMusicMatrixBroadCast.value )

          // 4. Save recommendation data to mongodb
          saveDataToMongoDB( uid, streamRecs )

          ConnHelper.jedis.set("calculation_status:" + uid, "done");
        }
      }
    }
    // Start receiving and processing data
    ssc.start()

    println(">>>>>>>>>>>>>>> streaming started!")

    ssc.awaitTermination()

  }

  // The redis operation returns a java class. In order to use the map operation, a conversion class needs to be introduced.
  import scala.collection.JavaConversions._

  def getUserRecentlyRating(num: Int, uid: Int, jedis: Jedis): Array[Int] = {
    // 从redis读取数据，用户评分数据保存在 uid:UID 为key的队列里，value是 MID:SCORE
    jedis.lrange("uid:" + uid, 0, num-1)
      .map{
        item => item.trim.toInt
      }
      .toArray
  }


  def getTopSimMusics(num: Int, mid: Int, uid: Int, simMusics: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]])
                     (implicit mongoConfig: MongoConfig): Array[Int] ={
    val allSimMusics = simMusics.getOrElse(mid, Map()).toArray

    if (allSimMusics.isEmpty) {
      println(s"Warning: mid=$mid not found in simMusics, returning empty result.")
      return Array.empty[Int]
    }

    val ratingExist = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)
      .find( MongoDBObject("uid" -> uid) )
      .toArray
      .map{
        item => item.get("mid").toString.toInt
      }

    allSimMusics.filter( x=> ! ratingExist.contains(x._1) )
      .sortWith(_._2>_._2)
      .take(num)
      .map(x=>x._1)
  }

  def computeMusicScores(candidateMusics: Array[Int],
                         userRecentlyRatings: Array[Int],
                         simMusics: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]]): Array[(Int, Double)] ={
    val scores = scala.collection.mutable.ArrayBuffer[(Int, Double)]()
    val increMap = scala.collection.mutable.HashMap[Int, Int]()
    val decreMap = scala.collection.mutable.HashMap[Int, Int]()

    for( candidateMusic <- candidateMusics; userRecentlyRating <- userRecentlyRatings){
      val simScore = getMusicsSimScore( candidateMusic, userRecentlyRating, simMusics )
      print(simScore)

      scores += ( (candidateMusic, simScore) )
      increMap(candidateMusic) = increMap.getOrDefault(candidateMusic, 0) + 1
    }

    scores.groupBy(_._1).map{

      case (mid, scoreList) =>
        ( mid, scoreList.map(_._2).sum / scoreList.length + log(increMap.getOrDefault(mid, 1)) - log(decreMap.getOrDefault(mid, 1)) )
    }.toArray.sortWith(_._2>_._2)
  }

  def getMusicsSimScore(mid1: Int, mid2: Int, simMusics: scala.collection.Map[Int,
    scala.collection.immutable.Map[Int, Double]]): Double ={

    simMusics.get(mid1) match {
      case Some(sims) => sims.get(mid2) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  def log(m: Int): Double ={
    val N = 10
    math.log(m)/ math.log(N)
  }

  def saveDataToMongoDB(uid: Int, streamRecs: Array[(Int, Double)])(implicit mongoConfig: MongoConfig): Unit ={
    val streamRecsCollection = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_STREAM_RECS_COLLECTION)

    streamRecsCollection.findAndRemove( MongoDBObject("uid" -> uid) )
    streamRecsCollection.insert( MongoDBObject( "uid"->uid,
      "recs"-> streamRecs.map(x=>MongoDBObject( "mid"->x._1, "score"->x._2 )) ) )
  }
}
