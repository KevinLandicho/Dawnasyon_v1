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

    @SerializedName("type")
    private String type;

    // ⭐ NEW FIELDS (These might be null from API, default to false/0)
    @SerializedName("like_count")
    private int likeCount = 0;

    // Local state tracking (Not always from API)
    private boolean isApplied = false;
    private boolean isLiked = false;
    private boolean isBookmarked = false;

    public Announcement() {}

    // Getters
    public long getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Long getLinkedDriveId() { return linkedDriveId; }
    public String getType() { return type; }

    // ⭐ New Getters & Setters
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}