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
        // ⭐ NEW: Listener for clicking the whole card
        void onCardClick(Announcement announcement);
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

        TranslationHelper.autoTranslate(context, holder.title, item.getTitle());
        TranslationHelper.autoTranslate(context, holder.description, item.getDescription());

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

        // 3. Logic for Drives / Applications
        String type = item.getType();
        boolean isDrive = type != null && (type.equalsIgnoreCase("Donation drive") || type.equalsIgnoreCase("Ayuda Application"));

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
            TranslationHelper.autoTranslate(context, holder.btnApply, holder.btnApply.getText().toString());

            // --- B. SHOW START/END DATES ---
            String start = item.getDriveStartDate();
            String end = item.getDriveEndDate();

            if (start != null || end != null) {
                holder.layoutDates.setVisibility(View.VISIBLE);
                if (start != null) {
                    holder.tvStartDate.setVisibility(View.VISIBLE);
                    holder.tvStartDate.setText("Start: " + formatDateTime(start, false));
                } else {
                    holder.tvStartDate.setVisibility(View.GONE);
                }
                if (end != null) {
                    holder.tvEndDate.setVisibility(View.VISIBLE);
                    holder.tvEndDate.setText("End: " + formatDateTime(end, false));
                } else {
                    holder.tvEndDate.setVisibility(View.GONE);
                }
            } else {
                holder.layoutDates.setVisibility(View.GONE);
            }

            // ⭐ NEW: Make the card clickable for details
            holder.itemView.setOnClickListener(v -> listener.onCardClick(item));

        } else {
            // General posts
            holder.btnApply.setVisibility(View.GONE);
            holder.layoutDates.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null); // Disable click for general posts
        }

        // 4. Like/Bookmark Visuals
        if (item.isLiked()) {
            holder.btnHeart.setImageResource(R.drawable.ic_heart_filled_red);
            holder.btnHeart.setColorFilter(Color.RED);
        } else {
            holder.btnHeart.clearColorFilter();
            holder.btnHeart.setImageResource(R.drawable.ic_heart_outline);
        }

        if (item.isBookmarked()) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            holder.btnBookmark.setColorFilter(Color.parseColor("#F5901A"));
        } else {
            holder.btnBookmark.clearColorFilter();
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline);
        }

        // 5. Button Click Listeners
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

    private String formatDateTime(String rawDate, boolean includeTime) {
        if (rawDate == null || rawDate.isEmpty()) return "N/A";
        try {
            SimpleDateFormat inputFormat;
            if (rawDate.contains("T")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(rawDate);
            SimpleDateFormat outputFormat;
            if (includeTime) {
                outputFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
            } else {
                outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            }
            outputFormat.setTimeZone(TimeZone.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }

    public static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnHeart, btnBookmark;
        TextView title, timestamp, description, tvLikeCount;
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