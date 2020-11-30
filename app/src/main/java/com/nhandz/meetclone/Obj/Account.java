package com.nhandz.meetclone.Obj;

import java.io.Serializable;

public class Account implements Serializable {
    private String Username;
    private String Password;
    private String Id;
    private String Name;

    public Account(String username, String password, String id, String name) {
        Username = username;
        Password = password;
        Id = id;
        Name = name;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}
