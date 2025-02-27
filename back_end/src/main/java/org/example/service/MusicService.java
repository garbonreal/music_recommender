package org.example.service;

import org.example.model.Music;
import org.example.model.MusicRecs;
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
import java.util.HashMap;
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
    private MongoCollection<Document> listeningCollection;

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

    private MongoCollection<Document> getListeningCollection(){
        if(null == listeningCollection)
            listeningCollection = mongoClient.getDatabase(Constant.MONGODB_DATABASE).getCollection(Constant.MONGODB_LISTENING_COLLECTION);
        return listeningCollection;
    }

    public List<Music> getMusics(List<Integer> mids) {
        FindIterable<Document> documents = getMusicCollection().find(Filters.in("mid",mids));
        List<Music> musics = new ArrayList<>();
        for (Document document: documents) {
            musics.add(documentToMusic(document));
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

    public List<Music> getMyRateMusics(int uid){
        FindIterable<Document> documents = getRateCollection().find(Filters.eq("uid",uid));
        List<Integer> ids = new ArrayList<>();
        Map<Integer, Double> scores = new HashMap<>();
        for (Document document: documents) {
            Rating rating = documentToRating(document);
            ids.add(rating.getMid());
            scores.put(rating.getMid(), rating.getScore());
        }
        List<Music> Musics = getMusics(ids);
        for (Music Music: Musics) {
            Music.setScore(scores.getOrDefault(Music.getMid(),Music.getScore()));
        }

        return Musics;
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
                            .append("count", 1);
            getListeningCollection().insertOne(doc);
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
    
        getListeningCollection().updateOne(query,
                new Document().append("$inc", new Document("count", 1)));
        return true;
    }
    
    public Listening findListening(int uid, int mid) {
        BasicDBObject query = new BasicDBObject();
        query.append("uid", uid);
        query.append("mid", mid);
        FindIterable<Document> documents = getListeningCollection().find(query);
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
}
