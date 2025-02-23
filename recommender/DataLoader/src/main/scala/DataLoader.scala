import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.net.InetAddress

case class Music(mid: Int, name: String, url: String)

/**
 * Rating
 *
 * 2	51	13883
 */
case class Rating(uid: Int, mid: Int, weight: Int, score: Double )

/**
 *
 * @param uri MongoDB connection
 * @param db  MongoDB database
 */
case class MongoConfig(uri:String, db:String)

object DataLoader {
  val MUSIC_DATA_PATH = "D:\\Javacode\\MusicRecommender\\recommender\\DataLoader\\src\\main\\resources\\artists.dat"
  val RATING_DATA_PATH = "D:\\javacode\\MusicRecommender\\recommender\\DataLoader\\src\\main\\resources\\user_artists.dat"

  val MONGODB_RATING_COLLECTION = "Rating"

  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/MusicRecommender",
      "mongo.db" -> "MusicRecommender"
    )

    // Build sparkConf
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("DataLoader")

    // Build SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    // data load
    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)

    // mean
    val meanValue = ratingRDD.map(attr => attr.split(",")(2).toInt).reduce(_ + _) / ratingRDD.count()
    // std
    val stdDevValue = math.sqrt(ratingRDD.map(attr => math.pow(attr.split(",")(2).toInt - meanValue, 2)).reduce(_ + _) / ratingRDD.count())

    val ratingDF = ratingRDD.map(item => {
      val attr = item.split(",")
      val zScore = (attr(2).toDouble - meanValue) / stdDevValue
      Rating(attr(0).toInt, attr(1).toInt, attr(2).toInt, zScore)
    }).toDF()

    val musicRDD = spark.sparkContext.textFile(MUSIC_DATA_PATH)

    val musicDF = musicRDD.map(
      item => {
        val attr = item.split(",")
        Music(attr(0).toInt, attr(1).trim, attr(2).trim)
      }
    ).toDF()

    musicDF.createOrReplaceTempView("music")
    val musicsInfoDF = spark.sql("select music.mid, music.name, music.url from music")

    // 定义MongoDB连接配置的隐式参数
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // 将数据保存到MongoDB
    storeDFInMongoDB(musicsInfoDF, "MusicInfo")
    storeDataInMongoDB(ratingDF)

    spark.stop()
  }

  def storeDFInMongoDB(df: DataFrame, collection_name: String)(implicit mongoConfig: MongoConfig): Unit = {
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }

  def storeDataInMongoDB(ratingDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {
    // 新建一个mongodb的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    // 如果mongodb中已经有相应的数据库，先删除
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).dropCollection()

    // 将DF数据写入对应的mongodb表中
    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //对数据表建索引
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))

    mongoClient.close()

  }
}