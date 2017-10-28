package com.animecap.groupwatch.messages.requests;

/**
 * Created by Nathaniel on 4/8/2017.
 */
public class ChatMessage {
    private String session;
    private String group;
    private String message;

    public String getSession() {
        return session;
    }

    public String getGroup() {
        return group;
    }

    public String getMessage() {
        return message;
    }
}
