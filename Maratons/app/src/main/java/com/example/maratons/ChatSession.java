package com.example.maratons;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatSession {
    private long id;
    private long timestamp;
    private String firstMessage;

    public ChatSession() {
    }

    public ChatSession(long id, long timestamp, String firstMessage) {
        this.id = id;
        this.timestamp = timestamp;
        this.firstMessage = firstMessage;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFirstMessage() {
        return firstMessage;
    }

    public void setFirstMessage(String firstMessage) {
        this.firstMessage = firstMessage;
    }

    // Форматируем временную метку как читаемую строку даты и времени
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}