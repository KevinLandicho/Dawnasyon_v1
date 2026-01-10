package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;

public class Announcement {

    @SerializedName("post_id")
    private long postId;

    @SerializedName("title")
    private String title;

    @SerializedName("created_at")
    private String timestamp;

    @SerializedName("body")
    private String description;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("linked_drive_id")
    private Long linkedDriveId;

    // ✅ ADDED ANNOTATION HERE so Gson finds the column
    @SerializedName("type")
    private String type;

    public Announcement() {}

    public Announcement(String title, String timestamp, String description, String imageUrl, String type) {
        this.title = title;
        this.timestamp = timestamp;
        this.description = description;
        this.imageUrl = imageUrl;
        this.type = type;
    }

    // Getters
    public long getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Long getLinkedDriveId() { return linkedDriveId; }
    public String getType() { return type; }
}