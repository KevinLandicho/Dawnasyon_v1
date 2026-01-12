package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class DonationHistoryItem implements Serializable {

    @SerializedName("donation_id")
    private long donationId;

    // ✅ FIX: Added Reference Number
    @SerializedName("reference_number")
    private String referenceNumber;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("status")
    private String status;

    @SerializedName("type")
    private String type;

    @SerializedName("amount")
    private Double amount;

    // ✅ FIX: Added List of Items
    @SerializedName("donation_items")
    private List<DonationItem> donationItems;

    // --- UI FIELDS ---
    private transient String formattedDate;
    private transient String displayDescription;
    private transient int imageResId;

    public DonationHistoryItem() {}

    // Getters
    public long getDonationId() { return donationId; }

    // ✅ This fixes the "cannot resolve method getReferenceNumber" error
    public String getReferenceNumber() { return referenceNumber; }

    public String getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public Double getAmount() { return amount; }

    // ✅ This fixes the "cannot resolve method getDonationItems" error
    public List<DonationItem> getDonationItems() { return donationItems; }

    public String getFormattedDate() { return formattedDate; }
    public String getDisplayDescription() { return displayDescription; }
    public int getImageResId() { return imageResId; }

    // Setters
    public void setFormattedDate(String date) { this.formattedDate = date; }
    public void setDisplayDescription(String desc) { this.displayDescription = desc; }
    public void setImageResId(int resId) { this.imageResId = resId; }
}