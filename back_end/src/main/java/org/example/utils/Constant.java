package org.example.utils;


public class Constant {

    //************** FOR MONGODB ****************

    public static String MONGODB_DATABASE = "MusicRecommender";

    public static String MONGODB_MUSIC_COLLECTION = "MusicInfo";

    public static String MONGODB_RATING_COLLECTION = "Rating";

    public static String MONGODB_LISTENING_COLLECTION = "Listening";

    public static String MONGODB_STREAM_RECS_COLLECTION = "StreamRecs";

    public static int MAX_HISTORY_SIZE = 20;

    //************** FOR MUSIC LISTENING ******************

    public static String MUSIC_LISTENING_PREFIX = "MUSIC_LISTENING_PREFIX";

    public static String MUSIC_RECS_STATUS = "calculation_status:";

    public static int REDIS_MUSIC_LISTENING_QUEUE_SIZE = 40;
}
