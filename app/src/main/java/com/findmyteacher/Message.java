package com.findmyteacher;

import com.google.firebase.firestore.PropertyName;

public class Message {

    @PropertyName("senderId")
    private String senderId;

    @PropertyName("text")
    private String text;

    @PropertyName("timestamp")
    private long timestamp;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    @PropertyName("senderId")
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @PropertyName("text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @PropertyName("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
