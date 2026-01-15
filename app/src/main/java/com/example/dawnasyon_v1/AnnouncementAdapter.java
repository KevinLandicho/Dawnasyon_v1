package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private List<Announcement> announcementList;
    private OnItemClickListener listener; // ⭐ Changed to generic listener
    private Context context;

    // ⭐ Expanded Interface
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

        holder.title.setText(item.getTitle());
        holder.timestamp.setText(item.getTimestamp());
        holder.description.setText(item.getDescription());
        holder.tvLikeCount.setText(item.getLikeCount() + " likes"); // ⭐ Show Count

        // Image Loading
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_image_placeholder);
        }

        // ⭐ APPLY BUTTON LOGIC
        String type = item.getType();
        if (type != null && type.equalsIgnoreCase("General")) {
            holder.btnApply.setVisibility(View.GONE);
        } else {
            holder.btnApply.setVisibility(View.VISIBLE);

            if (item.isApplied()) {
                // Gray out if applied
                holder.btnApply.setText("Applied");
                holder.btnApply.setBackgroundColor(Color.GRAY);
                holder.btnApply.setEnabled(false);
            } else {
                // Orange if available
                holder.btnApply.setText("Apply Now");
                holder.btnApply.setBackgroundColor(Color.parseColor("#F5901A"));
                holder.btnApply.setEnabled(true);
            }
        }

        // ⭐ LIKE BUTTON VISUALS
        if (item.isLiked()) {
            holder.btnHeart.setImageResource(R.drawable.ic_heart_filled_red); // Ensure you have this drawable
            holder.btnHeart.setColorFilter(Color.RED);
        } else {
            holder.btnHeart.clearColorFilter();
            holder.btnHeart.setImageResource(R.drawable.ic_heart_outline);

        }

        // ⭐ BOOKMARK BUTTON VISUALS
        if (item.isBookmarked()) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            holder.btnBookmark.setColorFilter(Color.parseColor("#F5901A"));
        } else {
            holder.btnBookmark.clearColorFilter();
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline);

        }

        // Click Listeners
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

    public static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnHeart, btnBookmark;
        TextView title, timestamp, description, tvLikeCount;
        Button btnApply;

        public AnnouncementViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgAnnouncementPhoto);
            title = itemView.findViewById(R.id.txtAnnouncementTitle);
            timestamp = itemView.findViewById(R.id.txtAnnouncementTime);
            description = itemView.findViewById(R.id.txtAnnouncementDescription);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count); // Ensure this ID exists in XML

            btnApply = itemView.findViewById(R.id.btnApply);
            btnHeart = itemView.findViewById(R.id.btnLike);         // Ensure this ID exists in XML
            btnBookmark = itemView.findViewById(R.id.btnBookmark);   // Ensure this ID exists in XML
        }
    }
}