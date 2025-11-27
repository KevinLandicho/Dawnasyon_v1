package com.example.dawnasyon_v1;

public class Announcement {
    private String title;
    private String timestamp;
    private String description;
    private int imageResId;

    public Announcement(String title, String timestamp, String description, int imageResId) {
        this.title = title;
        this.timestamp = timestamp;
        this.description = description;
        this.imageResId = imageResId;
    }

    // Getters
    public String getTitle() { return title; }
    public String getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }
}