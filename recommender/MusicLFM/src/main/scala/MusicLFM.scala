import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

// LFM based on rating data only requires rating data
case class MusicRating(uid: Int, mid: Int, weight: Int, score: Double )

case class MongoConfig(uri:String, db:String)

// Define a baseline recommendation object
case class Recommendation( mid: Int, score: Double )

// Define a music similarity list based on LFM music feature vectors
case class MusicRecs( mid: Int, recs: Seq[Recommendation] )

object MusicLFM {

  // Define table names and constants
  val MONGODB_RATING_COLLECTION = "Rating"

  val Music_RECS = "MusicRecs"

  val USER_MAX_RECOMMENDATION = 20

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/MusicRecommender",
      "mongo.db" -> "MusicRecommender"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("MusicLFM")

    // Build SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))


    // Data load
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MusicRating]
      .rdd
      .map( rating => ( rating.uid, rating.mid, rating.score ) )
      .cache()

    // Train a latent semantic model
    val trainData = ratingRDD.map( x => Rating(x._1, x._2, x._3) )
    val (rank, iterations, lambda) = (100, 5, 0.1)
    val model = ALS.train(trainData, rank, iterations, lambda)

    // Based on the hidden features of the music, calculate the similarity matrix and obtain the similarity list of the music
    val musicFeatures = model.productFeatures.map{
      case (mid, features) => (mid, new DoubleMatrix(features))
    }

    // Calculate the similarity between all musics, first do the Cartesian product
    val musicRecs = musicFeatures.cartesian(musicFeatures)
      .filter{
        // Filter out matches between yourself and yourself
        case (a, b) => a._1 != b._1
      }
      .map{
        case (a, b) => {
          val simScore = this.consinSim(a._2, b._2)
          ( a._1, ( b._1, simScore ) )
        }
      }
      .filter(_._2._2 > 0.4)    // Filter out those with a similarity greater than 0.6
      .groupByKey()
      .map{
        case (mid, items) => MusicRecs( mid, items.toList.sortWith(_._2 > _._2).take(500).map(x => Recommendation(x._1, x._2)) )
      }
      .toDF()

    musicRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", Music_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    spark.stop()
  }

  //Find vector cosine similarity
  def consinSim(Music1: DoubleMatrix, Music2: DoubleMatrix):Double ={
    Music1.dot(Music2) / ( Music1.norm2() * Music2.norm2() )
  }

}
