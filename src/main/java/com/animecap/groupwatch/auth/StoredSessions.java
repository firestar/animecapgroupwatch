package com.animecap.groupwatch.auth;

import com.animecap.groupwatch.repositories.AnimecapAPIService;

import java.util.TreeMap;

/**
 * Created by Nathaniel on 3/14/2017.
 */
public class StoredSessions {
    private static TreeMap<String, Session> sessions = new TreeMap<>();
    private static TreeMap<Long, String> sessionTemp = new TreeMap<>();

    public Session get( AnimecapAPIService animecapAPIService, String key ){
        if(contains(animecapAPIService, key)) {
            Session session = sessions.get(key);
            long accessTime = System.currentTimeMillis();
            sessionTemp.entrySet().parallelStream().forEach(e->{
                if(e.getValue().equals(key)) {
                    sessionTemp.remove(e.getKey());
                }
            });
            sessionTemp.put(accessTime, key);
            return session;
        }
        return null;
    }
    public boolean contains( AnimecapAPIService animecapAPIService, String key ){
        if(!sessions.containsKey(key)){
            Session session = animecapAPIService.sessionInfo(key);
            if(session!=null){
                long accessTime = System.currentTimeMillis();
                sessions.put(key, session);
                sessionTemp.put(accessTime, key);
            }
            return false;
        }
        return true;
    }

    public TreeMap<Long, String> getSessionTemp() {
        return sessionTemp;
    }

    public TreeMap<String, Session> getSessions() {
        return sessions;
    }
}
