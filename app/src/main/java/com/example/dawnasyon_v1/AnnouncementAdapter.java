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

    public AnnouncementAdapter(List<Announcement> announcementList) {
        this.announcementList = announcementList;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // R.layout.item_announcement_card is the reusable layout you created
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement_card, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement currentAnnouncement = announcementList.get(position);

        holder.title.setText(currentAnnouncement.getTitle());
        holder.timestamp.setText(currentAnnouncement.getTimestamp());
        holder.description.setText(currentAnnouncement.getDescription());
        holder.image.setImageResource(currentAnnouncement.getImageResId());

        // Setup click listeners for buttons if needed
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