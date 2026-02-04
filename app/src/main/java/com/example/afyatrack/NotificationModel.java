package com.example.afyatrack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private String type; // "profile_edited", "vaccine_due", "appointment", etc.
    private String childId;
    private long timestamp;
    private boolean isRead;
    private String metadata; // JSON string for additional data

    public NotificationModel() {
        // Default constructor required for Firebase
    }

    public NotificationModel(String id, String title, String message, String type, String childId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.childId = childId;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.metadata = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}