package org.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Document(collection = "StreamRecs")
public class MusicRecs implements Serializable {
    @Id
    private String _id;

    private Integer uid;
    private List<Recommendation> recs; // Using the nested Recommendation class for mid-score pairs

    // Nested class to represent the mid-score pairs
    public static class Recommendation {
        private Integer mid;
        private Double score; // Assuming score is a Double, adjust if it's a different type

        // Getters and setters
        public Integer getMid() {
            return mid;
        }

        public void setMid(Integer mid) {
            this.mid = mid;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

    }

    // Getters and setters for the MusicRecommendation class

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public Integer Uid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public List<Recommendation> getRecs() {
        return recs;
    }

    public void setRecs(List<Recommendation> recs) {
        this.recs = recs;
    }
}
