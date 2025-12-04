package com.example.dawnasyon_v1;

public class NotificationItem {
    private String title;
    private String description;
    private String time;
    private String dateCategory; // e.g., "Earlier", "Yesterday", "Today"
    private int type; // 0 for Relief Operation, 1 for Decline (affects icon)
    private boolean isExpanded; // To track if the item is open or closed

    public NotificationItem(String title, String description, String time, String dateCategory, int type) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.dateCategory = dateCategory;
        this.type = type;
        this.isExpanded = false;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTime() { return time; }
    public String getDateCategory() { return dateCategory; }
    public int getType() { return type; }
    public boolean isExpanded() { return isExpanded; }

    // Setter for expansion
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}