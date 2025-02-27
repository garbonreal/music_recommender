import breeze.numerics.sqrt
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
// import model.utils.{MongoConfig}
import model.{MongoConfig, MusicRating}

object ALSTrainer {

  val MONGODB_RATING_COLLECTION = "Rating"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/MusicRecommender",
      "mongo.db" -> "MusicRecommender"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("OfflineRecommender")

    // Build SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    import spark.implicits._

    // Load rating data
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MusicRating]
      .rdd
      .map( rating => Rating( rating.uid, rating.mid, rating.score ) )    // 转化成rdd，并且去掉时间戳
      .cache()

    // Randomly split the data set to generate a training set and a test set
    val splits = ratingRDD.randomSplit(Array(0.8, 0.2))
    val trainingRDD = splits(0)
    val testRDD = splits(1)

    // Model parameter selection and output optimal parameters
    adjustALSParam(trainingRDD, testRDD)

    spark.close()
  }

  def adjustALSParam(trainData: RDD[Rating], testData: RDD[Rating]): Unit ={
    val result = for( rank <- Array(50, 100, 200, 300); lambda <- Array( 0.01, 0.1, 1 ))
      yield {
        val model = ALS.train(trainData, rank, 5, lambda)
        println()
        // Calculate the rmse of the model corresponding to the current parameters and return Double
        val rmse = getRMSE( model, testData )
        println(( rank, lambda, rmse ))
        ( rank, lambda, rmse )
      }
    // Console printout of optimal parameters
    println("best: "+result.minBy(_._3))
  }

  def getRMSE(model: MatrixFactorizationModel, data: RDD[Rating]): Double = {
    // Calculate prediction score
    val userProducts = data.map(item => (item.user, item.product))
    val predictRating = model.predict(userProducts)

    // Using uid and mid as foreign keys, inner join actual observed values and predicted values
    val observed = data.map( item => ( (item.user, item.product), item.rating ) )
    val predict = predictRating.map( item => ( (item.user, item.product), item.rating ) )

    // Inner join gets (uid, mid), (actual, predict)
    sqrt(
      observed.join(predict).map{
        case ( (uid, mid), (actual, pre) ) =>
          val err = actual - pre
          err * err
      }.mean()
    )
  }
}
