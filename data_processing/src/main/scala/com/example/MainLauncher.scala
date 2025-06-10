package com.example

import scala.io.StdIn.readLine
import com.example.recommender.{ALSTrainer, MusicLFM, StreamingRecommender, DataLoader} 


object MainLauncher {
  def main(args: Array[String]): Unit = {
    println("Select program：")
    println("1. StreamingRecommender")
    println("2. DataLoader")
    println("3. MusicLFM")
    println("4. ALSTrainer")
    val choice = readLine()

    choice match {
      case "1" => StreamingRecommender.main(args)
      case "2" => DataLoader.main(args)
      case "3" => MusicLFM.main(args)
      case "4" => ALSTrainer.main(args)
      case _   => println("invalid input")
    }
  }
}