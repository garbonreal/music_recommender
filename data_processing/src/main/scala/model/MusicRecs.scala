package model

// Define a music similarity vector based on LFM music feature vectors
case class MusicRecs( mid: Int, recs: Seq[Recommendation] )
