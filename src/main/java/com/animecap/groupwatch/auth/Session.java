package com.animecap.groupwatch.auth;

import com.animecap.groupwatch.models.Account;

import java.util.UUID;

/**
 * Created by Nathaniel on 3/14/2017.
 */
public class Session {
    public Account account;
    public UUID sessionKey;
    public Session(){}
    public Session(Account account, UUID sessionKey){
        this.account = account;
        this.sessionKey = sessionKey;
    }

    public Account getAccount() {
        return account;
    }

    public UUID getSessionKey() {
        return sessionKey;
    }
}
