package com.animecap.groupwatch.models;

import com.animecap.groupwatch.relationship.WatchingRelation;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

/**
 * Created by Nathaniel on 3/14/2017.
 */
public class Account {
    public Account(){}

    public Account(String user, String pass, String salt, int level){
        this.salt = salt;
        this.user=user;
        this.pass = pass;
        this.level = level;
    }

    public Long id;

    public String user;

    @JsonIgnore
    private String pass; // hashed with sha512

    @JsonIgnore
    private String salt;

    public Set<WatchingRelation> watching;

    public int level;

    public String getPass() {
        return pass;
    }

    public String getSalt() {
        return salt;
    }
}
