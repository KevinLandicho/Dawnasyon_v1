package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Announcement implements Serializable {

    @SerializedName("post_id")
    private long postId;

    @SerializedName("title")
    private String title;

    // This field holds the 'created_at' data from Supabase
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

    @SerializedName("affected_street")
    private String affectedStreet;

    @SerializedName("like_count")
    private int likeCount = 0;

    // Local state tracking
    private boolean isApplied = false;
    private boolean isLiked = false;
    private boolean isBookmarked = false;

    public Announcement() {}

    // --- GETTERS ---
    public long getPostId() { return postId; }
    public String getTitle() { return title; }

    // Existing getter
    public String getTimestamp() { return timestamp; }

    // ⭐ NEW: This fixes the error in Home_fragment!
    public String getCreated_at() { return timestamp; }

    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Long getLinkedDriveId() { return linkedDriveId; }
    public String getType() { return type; }

    public String getAffected_street() { return affectedStreet; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}