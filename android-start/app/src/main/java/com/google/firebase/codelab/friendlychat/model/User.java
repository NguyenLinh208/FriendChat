package com.google.firebase.codelab.friendlychat.model;

import android.net.Uri;

/**
 * Created by usr0200475 on 2016/06/30.
 */
public class User {

    private String id;
    private String userName;
    private String email;

    public User(){}

    public User(String id, String userName, String email) {
        this.id = id;
        this.userName = userName;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
