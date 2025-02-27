name := "StreamingRecommender"
version := "1.0"
scalaVersion := "2.11.8"  // Matches <scala.version>2.11.8</scala.version> from pom.xml

val sparkVersion = "2.1.1"  // Matches <spark.version>2.1.1</spark.version>
val kafkaVersion = "0.10.2.1"  // Matches <kafka.version>0.10.2.1</kafka.version>
val mongodbSparkVersion = "2.0.0"  // Matches <mongodb-spark.version>2.0.0</mongodb-spark.version>
val casbahVersion = "3.1.1"  // Matches <casbah.version>3.1.1</casbah.version>
val redisVersion = "2.9.0"  // Matches <redis.version>2.9.0</redis.version>

libraryDependencies ++= Seq(
  // Spark dependencies
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,  // Required for ALS recommendation algorithm

  // Kafka dependencies
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,

  // MongoDB dependencies
  "org.mongodb.spark" %% "mongo-spark-connector" % mongodbSparkVersion,
  "org.mongodb" %% "casbah" % casbahVersion,

  // Redis dependencies
  "redis.clients" % "jedis" % redisVersion,

  // Configuration file support
  "com.typesafe" % "config" % "1.4.2",

  // Breeze for numerical computing
  "org.scalanlp" %% "breeze" % "0.13.2",

  // JBLAS for linear algebra calculations
  "org.jblas" % "jblas" % "1.2.4"
)

// Override dependencies to avoid logging conflicts
dependencyOverrides ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.22",
  "log4j" % "log4j" % "1.2.17"
)

// Ensure correct package resolution
resolvers += "Apache Spark Repository" at "https://repo1.maven.org/maven2/"