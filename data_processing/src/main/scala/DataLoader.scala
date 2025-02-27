import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import com.typesafe.config.ConfigFactory
import model.{MongoConfig, Rating, Music}

import java.net.InetAddress

object DataLoader {
  val conf = ConfigFactory.load()

  val MUSIC_DATA_PATH = conf.getString("app.data.musicPath")
  val RATING_DATA_PATH = conf.getString("app.data.ratingPath")

  val MONGODB_RATING_COLLECTION = "Rating"

  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> conf.getString("app.spark.cores"),
      "mongo.uri" -> conf.getString("app.mongo.uri"),
      "mongo.db" -> conf.getString("app.mongo.db")
    )

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

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

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
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).dropCollection()

    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))

    mongoClient.close()

  }
}