package com.animecap.groupwatch.messages.requests;

import com.animecap.groupwatch.api.auth.Session;

/**
 * Created by Nathaniel on 4/10/2017.
 */
public class LoadVideo {
    private String session;
    private String group;
    private long episode;

    public String getSession() {
        return session;
    }

    public String getGroup() {
        return group;
    }

    public long getEpisode() {
        return episode;
    }
}
