package com.ashtonwilliamallen.nearchat;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {

    public String username;
    public String message;
    public String datetime;
    public String locChat;


    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Message(String username, String message, String datetime, String locChat ) {
        this.username = username;
        this.message = message;
        this.datetime = datetime;
        this.locChat = locChat;
    }

}