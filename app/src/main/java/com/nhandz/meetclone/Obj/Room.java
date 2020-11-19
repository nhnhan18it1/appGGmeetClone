package com.nhandz.meetclone.Obj;

import java.io.Serializable;

public class Room implements Serializable {
   private String title;
   private String gId;
   private String key;

    public Room(String title, String gId, String key) {
        this.title = title;
        this.gId = gId;
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getgId() {
        return gId;
    }

    public void setgId(String gId) {
        this.gId = gId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
