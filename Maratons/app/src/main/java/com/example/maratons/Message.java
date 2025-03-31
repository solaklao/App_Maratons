package com.example.maratons;

public class Message {
    private String message;
    private boolean isReceived;
    private long timestamp;

    public Message(String message, boolean isReceived) {
        this.message = message;
        this.isReceived = isReceived;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public void setReceived(boolean received) {
        isReceived = received;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}