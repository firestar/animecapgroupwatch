package com.animecap.groupwatch.auth;

import com.animecap.groupwatch.repositories.AnimecapAPIService;

import java.util.TreeMap;

/**
 * Created by Nathaniel on 3/14/2017.
 */
public class StoredSessions {
    public class SessionTemp{
        private long accessTime;
        private String sessionKey;
        private Session session;
        public SessionTemp(long accessTime, String sessionKey, Session session){
            this.accessTime = accessTime;
            this.sessionKey = sessionKey;
            this.session = session;
        }
        public long getAccessTime() {
            return accessTime;
        }

        public void setAccessTime(long accessTime) {
            this.accessTime = accessTime;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        public Session getSession() {
            return session;
        }

        public void setSession(Session session) {
            this.session = session;
        }
    }

    private static TreeMap<String, SessionTemp> sessions = new TreeMap<String, SessionTemp>();

    public Session get( AnimecapAPIService animecapAPIService, String key ){
        if(this.contains(animecapAPIService, key)) {
            SessionTemp sessionTemp = sessions.get(key);
            long accessTime = System.currentTimeMillis();
            sessionTemp.setAccessTime(accessTime);
            return sessionTemp.getSession();
        }
        return null;
    }
    public boolean contains( AnimecapAPIService animecapAPIService, String key ){
        System.out.println("KEY: "+key);
        if(sessions.size()==0 || (sessions.size()>0 && !sessions.containsKey(key))){
            Session session = animecapAPIService.sessionInfo(key);
            if(session!=null){
                long accessTime = System.currentTimeMillis();
                sessions.put(key, new SessionTemp(accessTime, key, session));
                return true;
            }
            return false;
        }
        return true;
    }
}
