package org.example.service;

import org.example.model.Music;
import org.example.model.MusicRecs;
import org.example.model.MusicRecs.Recommendation;
import org.example.model.Rating;
import org.example.model.Listening;
import org.example.utils.Constant;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.util.JSON;

import org.springframework.stereotype.Service;
import org.bson.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MusicService {

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Jedis jedis;

    private MongoCollection<Document> musicCollection;
    private MongoCollection<Document> rateCollection;
    private MongoCollection<Document> streamCollection;

    private MongoCollection<Document> getMusicCollection(){
        if(null == musicCollection)
            musicCollection = mongoClient.getDatabase(Constant.MONGODB_DATABASE).getCollection(Constant.MONGODB_MUSIC_COLLECTION);
        return musicCollection;
    }

    private MongoCollection<Document> getRateCollection(){
        if(null == rateCollection)
            rateCollection = mongoClient.getDatabase(Constant.MONGODB_DATABASE).getCollection(Constant.MONGODB_RATING_COLLECTION);
        return rateCollection;
    }

    private MongoCollection<Document> getStreamCollection(){
        if(null == streamCollection)
            streamCollection = mongoClient.getDatabase(Constant.MONGODB_DATABASE).getCollection(Constant.MONGODB_STREAM_RECS_COLLECTION);
        return streamCollection;
    }

    public List<Music> getMusics(List<Integer> mids, Integer uid) {
        if (mids.isEmpty()) return new ArrayList<>(); 

        FindIterable<Document> documents = getMusicCollection().find(Filters.in("mid", mids));

        Map<Integer, Music> musicMap = new LinkedHashMap<>();
        for (Integer mid : mids) {
            musicMap.put(mid, null);
        }

        for (Document document : documents) {
            Music music = documentToMusic(document);

            Document clicks = getRateCollection().find(Filters.and(
                Filters.eq("uid", uid),
                Filters.eq("mid", music.getMid())
            )).first();

            if (clicks == null || clicks.isEmpty())
                music.setClicks(0);
            else
                music.setClicks((Integer) clicks.get("weight", 0));

            musicMap.put(music.getMid(), music);
        }

        List<Music> musics = new ArrayList<>();
        for (Integer mid : mids) {
            if (musicMap.get(mid) != null) {
                musics.add(musicMap.get(mid));
            }
        }
        return musics;
    }

    private Music documentToMusic(Document document){
        Music music = null;
        try{
            music = objectMapper.readValue(JSON.serialize(document),Music.class);
        }catch (IOException e) {
            e.printStackTrace();
        }
        return music;
    }

    public List<Music> getHistoryMusics(int uid){
        String redisKey = "uid:" + uid;

        if (jedis.exists(redisKey)) {
            List<String> mids = jedis.lrange(redisKey, 0, Constant.MAX_HISTORY_SIZE - 1);
            Set<Integer> uniqueIds = mids.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toSet());
            return getMusics(new ArrayList<>(uniqueIds), uid);
        }

        return Collections.emptyList();
    }

    public List<Music> getPopularMusics(int uid){
        List<Integer> ids = new ArrayList<>();

        FindIterable<Document> documents = getRateCollection()
            .find(Filters.eq("uid", uid))
            .sort(Sorts.descending("weight"));
        for (Document document : documents) {
            if (ids.size() >= Constant.MAX_HISTORY_SIZE) 
                break;
            Rating rating = documentToRating(document);
            if (!ids.contains(rating.getMid())) {
                ids.add(rating.getMid());
            }
        }

        return getMusics(ids, uid);
    }        

    private Rating documentToRating(Document document){
        Rating rating = null;
        try{
            rating = objectMapper.readValue(JSON.serialize(document),Rating.class);
        }catch (IOException e) {
            e.printStackTrace();
        }
        return rating;
    }   
        
    public boolean musicListening(int uid, int mid){
        Listening listening = new Listening(uid, mid, 0);
        updateRedis(listening);
        if (listeningExist(listening.getUid(), listening.getMid())) {
            return updateListening(listening);
        } else {
            return newListening(listening);
        }
    }   
        
    private void updateRedis(Listening listening) {
        if (jedis.exists("uid:" + listening.getUid()) && 
            jedis.llen("uid:" + listening.getUid()) >= Constant.REDIS_MUSIC_LISTENING_QUEUE_SIZE) {
            jedis.rpop("uid:" + listening.getUid());
        }
        jedis.lpush("uid:" + listening.getUid(), String.valueOf(listening.getMid()));
    }   
        
    public boolean newListening(Listening listening) {
        try {
            Document doc = new Document("uid", listening.getUid())
                            .append("mid", listening.getMid())
                            .append("weight", 1);
            getRateCollection().insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }   
        
    public boolean listeningExist(int uid, int mid) {
        return null != findListening(uid, mid);
    }   
        
    public boolean updateListening(Listening listening) {
        BasicDBObject query = new BasicDBObject();
        query.append("uid", listening.getUid());
        query.append("mid", listening.getMid());
        
        getRateCollection().updateOne(query,
                new Document().append("$inc", new Document("weight", 1)));
        return true;
    }
    
    public Listening findListening(int uid, int mid) {
        BasicDBObject query = new BasicDBObject();
        query.append("uid", uid);
        query.append("mid", mid);
        FindIterable<Document> documents = getRateCollection().find(query);
        if (documents.first() == null)
            return null;
        return documentToListening(documents.first());
    }

    private Listening documentToListening(Document document){
        Listening listening = null;
        try{
            listening = objectMapper.readValue(JSON.serialize(document),Listening.class);
        }catch (IOException e) {
            e.printStackTrace();
        }
        return listening;
    }

    public List<Music> getRealTimeMusics(int uid){
        List<Integer> ids = findStreamRecs(uid, 20);
        List<Music> Musics = getMusics(ids, uid);
        return Musics;
    }

    // Real-time recommendation
    private List<Integer> findStreamRecs(int uid,int maxItems){
        Document streamRecs = getStreamCollection().find(new Document("uid", uid)).first();
        return parseRecs(streamRecs, maxItems);
    }

    private List<Integer> parseRecs(Document document, int maxItems) {
        List<Integer> recommendations = new ArrayList<>();
        if (null == document || document.isEmpty())
            return recommendations;
        ArrayList<Document> recs = document.get("recs", ArrayList.class);
        for (Document recDoc : recs) {
            recommendations.add(recDoc.getInteger("mid"));
        }
        recommendations.sort((a, b) -> b - a);
        return recommendations.subList(0, maxItems > recommendations.size() ? recommendations.size() : maxItems);
    }

    public String getCalculationStatus(int uid){
        Document streamRecs = getStreamCollection().find(new Document("uid", uid)).first();
        if (streamRecs == null) {
            return "waiting";
        }
        else {
            String status = jedis.get(Constant.MUSIC_RECS_STATUS + ":" + uid);
            return status;
        }
    }
}
