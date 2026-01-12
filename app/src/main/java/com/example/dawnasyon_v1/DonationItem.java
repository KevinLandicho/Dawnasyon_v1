package com.example.dawnasyon_v1;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class DonationItem implements Serializable {

    @SerializedName("item_name")
    private String itemName;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("unit")
    private String unit;

    public DonationItem() {}

    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }

    // Helper to get string like "5 kg"
    public String getQtyString() {
        return quantity + " " + (unit != null ? unit : "");
    }
}