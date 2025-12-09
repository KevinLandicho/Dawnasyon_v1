package com.example.dawnasyon_v1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private List<Announcement> announcementList;

    // [NEW] 1. Define an interface for the click event
    public interface OnApplyClickListener {
        void onApplyClick(Announcement announcement);
    }

    // [NEW] 2. Add a variable to hold the listener
    private OnApplyClickListener listener;

    // [UPDATED] 3. Update constructor to accept the listener
    public AnnouncementAdapter(List<Announcement> announcementList, OnApplyClickListener listener) {
        this.announcementList = announcementList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement_card, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement currentAnnouncement = announcementList.get(position);

        // Bind data to views
        holder.title.setText(currentAnnouncement.getTitle());
        holder.timestamp.setText(currentAnnouncement.getTimestamp());
        holder.description.setText(currentAnnouncement.getDescription());
        holder.image.setImageResource(currentAnnouncement.getImageResId());

        // [NEW] 4. Set the click listener on the "Apply" button
        holder.btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if listener is not null before calling it
                if (listener != null) {
                    listener.onApplyClick(currentAnnouncement);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    // ViewHolder: References the elements in item_announcement_card.xml
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