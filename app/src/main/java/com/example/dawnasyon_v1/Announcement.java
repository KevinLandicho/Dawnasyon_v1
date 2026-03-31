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

    // ⭐ THE FIX: Mapped the status column from Supabase!
    @SerializedName("status")
    private String status;

    @SerializedName("like_count")
    private int likeCount = 0;

    @SerializedName("bookmark_count")
    private int bookmarkCount = 0;

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
    public String getCreated_at() { return timestamp; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Long getLinkedDriveId() { return linkedDriveId; }
    public String getType() { return type; }
    public String getAffected_street() { return affectedStreet; }

    public String getStatus() { return status; }

    // --- HELPER METHODS FOR NESTED DATA ---
    public String getDriveStartDate() {
        return (driveInfo != null) ? driveInfo.startDate : null;
    }

    public String getDriveEndDate() {
        return (driveInfo != null) ? driveInfo.endDate : null;
    }

    public String getReliefItemList() {
        return (driveInfo != null) ? driveInfo.reliefItemList : null;
    }

    public boolean isDriveFull() {
        if (driveInfo == null) return false;
        return driveInfo.currentApplications >= driveInfo.applicationLimit;
    }

    public String getSlotsRemaining() {
        if (driveInfo == null) return null;
        int remaining = driveInfo.applicationLimit - driveInfo.currentApplications;

        if (remaining <= 0) {
            return "No slots remaining";
        }
        return remaining + " slots remaining";
    }

    // --- LOCAL STATE SETTERS/GETTERS ---
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getBookmarkCount() { return bookmarkCount; }
    public void setBookmarkCount(int bookmarkCount) { this.bookmarkCount = bookmarkCount; }

    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { this.isLiked = liked; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { this.isBookmarked = bookmarked; }

    // --- INNER CLASS FOR NESTED JSON ---
    public static class ReliefDriveInfo implements Serializable {
        @SerializedName("start_date")
        public String startDate;

        @SerializedName("end_date")
        public String endDate;

        @SerializedName("relief_item_list")
        public String reliefItemList;

        @SerializedName("application_limit")
        public int applicationLimit;

        @SerializedName("current_applications")
        public int currentApplications;
    }
}