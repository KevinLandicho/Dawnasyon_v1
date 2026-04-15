package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {

    // ⭐ Maps exactly to your Supabase primary key (String for UUIDs)
    @SerializedName("notif_id")
    private String id;

    @SerializedName("title")
    private String title;

    // ⭐ Only ONE variable mapped to "message" now!
    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("type")
    private String dbType;

    @SerializedName("sender_name")
    private String senderName;

    // Transient fields (UI only, ignored by database)
    private transient String time;
    private transient String dateCategory;
    private transient int type;
    private transient boolean isExpanded;

    public NotificationItem() {}

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; } // ⭐ Use getMessage everywhere now!
    public String getTime() { return time; }
    public String getDateCategory() { return dateCategory; }
    public int getType() { return type; }
    public String getCreatedAt() { return createdAt; }
    public String getDbType() { return dbType; }
    public String getSenderName() { return senderName; }

    // Setters
    public void setTime(String time) { this.time = time; }
    public void setDateCategory(String category) { this.dateCategory = category; }
    public void setType(int type) { this.type = type; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}