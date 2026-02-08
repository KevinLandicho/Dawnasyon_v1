package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private List<Announcement> announcementList;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onApplyClick(Announcement announcement);
        void onLikeClick(Announcement announcement, int position);
        void onBookmarkClick(Announcement announcement, int position);
    }

    public AnnouncementAdapter(List<Announcement> announcementList, OnItemClickListener listener) {
        this.announcementList = announcementList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_announcement_card, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement item = announcementList.get(position);

        // 1. Basic Text Data
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        holder.tvLikeCount.setText(item.getLikeCount() + " likes");

        // ⭐ NEW: Make the "Created At" timestamp readable
        // Example: "Feb 08, 2026 • 10:30 AM"
        holder.timestamp.setText(formatDateTime(item.getCreated_at(), true));

        // 2. Image Logic
        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
        }

        // 3. "Donation drive" Specific Logic
        String type = item.getType();
        boolean isDrive = type != null && type.equalsIgnoreCase("Donation drive");

        if (isDrive) {
            // --- A. SHOW APPLY BUTTON ---
            holder.btnApply.setVisibility(View.VISIBLE);

            if (item.isApplied()) {
                holder.btnApply.setText("Applied");
                holder.btnApply.setBackgroundColor(Color.GRAY);
                holder.btnApply.setEnabled(false);
            } else {
                holder.btnApply.setText("Apply Now");
                holder.btnApply.setBackgroundColor(Color.parseColor("#F5901A"));
                holder.btnApply.setEnabled(true);
            }

            // --- B. SHOW START/END DATES (Readable) ---
            String start = item.getDriveStartDate(); // Raw Date (yyyy-mm-dd)
            String end = item.getDriveEndDate();     // Raw Date (yyyy-mm-dd)

            if (start != null || end != null) {
                holder.layoutDates.setVisibility(View.VISIBLE);

                if (start != null) {
                    holder.tvStartDate.setVisibility(View.VISIBLE);
                    // Format: "Feb 08, 2026"
                    holder.tvStartDate.setText("Start: " + formatDateTime(start, false));
                } else {
                    holder.tvStartDate.setVisibility(View.GONE);
                }

                if (end != null) {
                    holder.tvEndDate.setVisibility(View.VISIBLE);
                    // Format: "Feb 10, 2026"
                    holder.tvEndDate.setText("End: " + formatDateTime(end, false));
                } else {
                    holder.tvEndDate.setVisibility(View.GONE);
                }
            } else {
                holder.layoutDates.setVisibility(View.GONE);
            }

        } else {
            // Hide drive-specific elements for General posts
            holder.btnApply.setVisibility(View.GONE);
            holder.layoutDates.setVisibility(View.GONE);
        }

        // 4. Like Button Visuals
        if (item.isLiked()) {
            holder.btnHeart.setImageResource(R.drawable.ic_heart_filled_red);
            holder.btnHeart.setColorFilter(Color.RED);
        } else {
            holder.btnHeart.clearColorFilter();
            holder.btnHeart.setImageResource(R.drawable.ic_heart_outline);
        }

        // 5. Bookmark Button Visuals
        if (item.isBookmarked()) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            holder.btnBookmark.setColorFilter(Color.parseColor("#F5901A"));
        } else {
            holder.btnBookmark.clearColorFilter();
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline);
        }

        // 6. Click Listeners
        holder.btnApply.setOnClickListener(v -> listener.onApplyClick(item));
        holder.btnHeart.setOnClickListener(v -> listener.onLikeClick(item, position));
        holder.btnBookmark.setOnClickListener(v -> listener.onBookmarkClick(item, position));
    }

    @Override
    public int getItemCount() {
        return announcementList != null ? announcementList.size() : 0;
    }

    public void updateData(List<Announcement> newAnnouncements) {
        this.announcementList = newAnnouncements;
        notifyDataSetChanged();
    }

    // ⭐ HELPER: Converts ISO Date (2026-02-08...) to Readable Text
    private String formatDateTime(String rawDate, boolean includeTime) {
        if (rawDate == null || rawDate.isEmpty()) return "N/A";

        try {
            // Supabase often returns: "2026-02-08T14:30:00+00:00" OR "2026-02-08"
            // We try parsing the full timestamp first
            SimpleDateFormat inputFormat;
            if (rawDate.contains("T")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Supabase is usually UTC

            Date date = inputFormat.parse(rawDate);

            // Output Format
            SimpleDateFormat outputFormat;
            if (includeTime) {
                // Example: Feb 08, 2026 • 2:30 PM
                outputFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
            } else {
                // Example: Feb 08, 2026
                outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            }
            outputFormat.setTimeZone(TimeZone.getDefault()); // Convert to user's local time

            return outputFormat.format(date);

        } catch (Exception e) {
            // If parsing fails, just return the raw string so it's not empty
            return rawDate;
        }
    }

    public static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnHeart, btnBookmark;
        TextView title, timestamp, description, tvLikeCount;

        // Date Views
        LinearLayout layoutDates;
        TextView tvStartDate, tvEndDate;

        Button btnApply;

        public AnnouncementViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgAnnouncementPhoto);
            title = itemView.findViewById(R.id.txtAnnouncementTitle);
            timestamp = itemView.findViewById(R.id.txtAnnouncementTime);
            description = itemView.findViewById(R.id.txtAnnouncementDescription);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);

            btnApply = itemView.findViewById(R.id.btnApply);
            btnHeart = itemView.findViewById(R.id.btnLike);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);

            layoutDates = itemView.findViewById(R.id.layout_dates);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
        }
    }
}