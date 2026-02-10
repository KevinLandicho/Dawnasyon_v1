package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);
        Context context = holder.itemView.getContext();

        // ✅ 1. SET THE TEXT
        holder.tvTitle.setText(item.getTitle());
        holder.tvTime.setText(item.getTime());
        holder.tvDescShort.setText(item.getDescription());

        // Populate the "Full" description for the detail view logic later
        if (holder.tvDescFull != null) {
            holder.tvDescFull.setText(item.getDescription());
        }

        // ⭐ NEW: Auto-Translate if "Tagalog Mode" is enabled
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isTagalog = prefs.getBoolean("is_tagalog", false);

        if (isTagalog) {
            TranslationHelper.autoTranslate(context, holder.tvTitle, item.getTitle());
            TranslationHelper.autoTranslate(context, holder.tvDescShort, item.getDescription());

            if (holder.tvDescFull != null) {
                TranslationHelper.autoTranslate(context, holder.tvDescFull, item.getDescription());
            }
        }

        // ✅ 2. SET THE ICON
        // If type is 1 (Decline/Alert), show red icon. Otherwise, show standard blue/green.
        if (item.getType() == 1) {
            holder.imgIcon.setImageResource(R.drawable.ic_danger);
            holder.tvTitle.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_notifications);
            holder.tvTitle.setTextColor(Color.BLACK);
        }

        // ✅ 3. HANDLE CLICK (Navigate to Detail Fragment)
        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) v.getContext();

                // Create the detail fragment with the item data
                NotificationDetail_fragment detailFragment = NotificationDetail_fragment.newInstance(item);

                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
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
        ImageView imgIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            tvDescShort = itemView.findViewById(R.id.tv_notif_desc_short);

            // These might be null depending on your exact XML, so we check them safely
            tvDescFull = itemView.findViewById(R.id.tv_notif_desc_full);
            imgIcon = itemView.findViewById(R.id.img_notif_icon);
        }
    }
}