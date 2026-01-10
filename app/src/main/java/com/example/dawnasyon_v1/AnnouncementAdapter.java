package com.example.dawnasyon_v1;

import android.content.Context;
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
    private OnApplyClickListener listener;
    private Context context;

    public interface OnApplyClickListener {
        void onApplyClick(Announcement announcement);
    }

    public AnnouncementAdapter(List<Announcement> announcementList, OnApplyClickListener listener) {
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
        Announcement currentAnnouncement = announcementList.get(position);

        holder.title.setText(currentAnnouncement.getTitle());
        holder.timestamp.setText(currentAnnouncement.getTimestamp());
        holder.description.setText(currentAnnouncement.getDescription());

        // LOAD IMAGE FROM URL USING GLIDE
        if (currentAnnouncement.getImageUrl() != null && !currentAnnouncement.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(currentAnnouncement.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_image_placeholder);
        }

        // ⭐ LOGIC TO HIDE APPLY BUTTON ⭐
        // Check if type is "General". Use equalsIgnoreCase to be safe.
        // We also check for null just in case the database field is empty.
        String type = currentAnnouncement.getType();

        if (type != null && type.equalsIgnoreCase("General")) {
            // If General, remove the button
            holder.btnApply.setVisibility(View.GONE);
        } else {
            // If Donation Drive (or anything else), show the button
            holder.btnApply.setVisibility(View.VISIBLE);
        }

        holder.btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApplyClick(currentAnnouncement);
            }
        });
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
        ImageView image;
        TextView title;
        TextView timestamp;
        TextView description;
        Button btnApply;

        public AnnouncementViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgAnnouncementPhoto);
            title = itemView.findViewById(R.id.txtAnnouncementTitle);
            timestamp = itemView.findViewById(R.id.txtAnnouncementTime);
            description = itemView.findViewById(R.id.txtAnnouncementDescription);
            btnApply = itemView.findViewById(R.id.btnApply);
        }
    }
}