package com.example.dawnasyon_v1;

public class DonationHistoryItem {
    private String donorName;
    private String date;
    private String description; // Short description like "Relief Packs"
    private String status; // e.g., "Received", "Pending"
    private int imageResId; // For the avatar

    public DonationHistoryItem(String donorName, String date, String description, String status, int imageResId) {
        this.donorName = donorName;
        this.date = date;
        this.description = description;
        this.status = status;
        this.imageResId = imageResId;
    }

    public String getDonorName() { return donorName; }
    public String getDate() { return date; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public int getImageResId() { return imageResId; }
}