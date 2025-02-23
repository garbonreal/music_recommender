package org.example.service;

import org.example.model.Music;
import org.example.model.MusicRecs;
import org.example.model.Rating;
import org.example.utils.Constant;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.util.JSON;

import org.springframework.stereotype.Service;
import org.bson.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private MongoCollection<Document> musicCollection;
    private MongoCollection<Document> rateCollection;

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

}
