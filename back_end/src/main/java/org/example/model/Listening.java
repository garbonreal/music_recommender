package org.example.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

public class Listening {
    private Integer uid;
    private Integer mid;
    private Integer clicks;

    public Listening(Integer uid, Integer mid, Integer clicks) {
        this.uid = uid;
        this.mid = mid;
        this.clicks = clicks;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public Integer getClicks() {
        return clicks;
    }

    public void setClicks(Integer clicks) {
        this.clicks = clicks;
    }
}
