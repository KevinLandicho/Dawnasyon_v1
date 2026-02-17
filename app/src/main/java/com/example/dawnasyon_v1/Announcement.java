package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Announcement implements Serializable {

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

    @SerializedName("affected_street")
    private String affectedStreet;

    @SerializedName("like_count")
    private int likeCount = 0;

    // Capture the nested "relief_drives" data from Supabase join
    @SerializedName("relief_drives")
    private ReliefDriveInfo driveInfo;

    // Local state tracking
    private boolean isApplied = false;
    private boolean isLiked = false;
    private boolean isBookmarked = false;

    public Announcement() {}

    // --- GETTERS ---
    public long getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getTimestamp() { return timestamp; }
    public String getCreated_at() { return timestamp; } // Alias for Home_fragment compatibility
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Long getLinkedDriveId() { return linkedDriveId; }
    public String getType() { return type; }
    public String getAffected_street() { return affectedStreet; }

    // --- HELPER METHODS FOR NESTED DATA ---
    public String getDriveStartDate() {
        return (driveInfo != null) ? driveInfo.startDate : null;
    }

    public String getDriveEndDate() {
        return (driveInfo != null) ? driveInfo.endDate : null;
    }

    // ⭐ NEW: Helper to get the relief items list
    public String getReliefItemList() {
        // If driveInfo is null or the list is null, return null so we can handle the default text in the fragment
        return (driveInfo != null) ? driveInfo.reliefItemList : null;
    }

    // --- LOCAL STATE SETTERS/GETTERS ---
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }

    // --- INNER CLASS FOR NESTED JSON ---
    public static class ReliefDriveInfo implements Serializable {
        @SerializedName("start_date")
        public String startDate;

        @SerializedName("end_date")
        public String endDate;

        // ⭐ NEW: Field to capture the list from the joined 'relief_drives' table
        @SerializedName("relief_item_list")
        public String reliefItemList;
    }
}