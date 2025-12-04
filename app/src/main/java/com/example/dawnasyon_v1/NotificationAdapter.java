package com.example.dawnasyon_v1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notificationList;

    public NotificationAdapter(List<NotificationItem> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }


    // Inside NotificationAdapter.java

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);

        // ... (Keep your existing text setting code) ...

        // REMOVE old expansion logic
        // holder.layoutExpanded.setVisibility(...);

        // NEW Click Listener: Navigate to Detail Fragment
        holder.itemView.setOnClickListener(v -> {
            // Get the FragmentManager from the context (assuming Context is Activity)
            if (v.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) v.getContext();

                NotificationDetail_fragment detailFragment = NotificationDetail_fragment.newInstance(item);

                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment) // Ensure this ID matches your MainActivity's container
                        .addToBackStack(null)
                        .commit();
            }
        });
    }
    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvDescShort, tvDescFull;
        ImageView imgIcon, imgAttachment;
        LinearLayout layoutExpanded;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            tvDescShort = itemView.findViewById(R.id.tv_notif_desc_short);
            tvDescFull = itemView.findViewById(R.id.tv_notif_desc_full);
            imgIcon = itemView.findViewById(R.id.img_notif_icon);
            imgAttachment = itemView.findViewById(R.id.img_notif_attachment);
            layoutExpanded = itemView.findViewById(R.id.layout_expanded);
        }
    }
}