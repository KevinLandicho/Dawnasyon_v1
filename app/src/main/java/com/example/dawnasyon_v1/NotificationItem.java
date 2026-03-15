package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {

    // ⭐ NEW: Maps exactly to your Supabase primary key
    @SerializedName("notif_id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String description;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("type")
    private String dbType;

    @SerializedName("sender_name")
    private String senderName;

    // Transient fields (UI only)
    private transient String time;
    private transient String dateCategory;
    private transient int type;
    private transient boolean isExpanded;

    public NotificationItem() {}

    // Getters
    public long getId() { return id; } // ⭐ NEW GETTER FOR THE ADAPTER

    public String getTitle() { return title; }
    public String getDescription() { return description; }
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