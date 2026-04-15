package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notificationList;
    private static final String SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi";

    public NotificationAdapter(List<NotificationItem> notificationList) {
        this.notificationList = notificationList;
    }

    public void updateData(List<NotificationItem> newList) {
        this.notificationList.clear();
        this.notificationList.addAll(newList);
        notifyDataSetChanged();
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

        holder.tvTitle.setText(item.getTitle());
        holder.tvTime.setText(item.getTime());
        holder.tvDescShort.setText(item.getMessage());
        if (holder.tvDescFull != null) holder.tvDescFull.setText(item.getMessage());

        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("is_tagalog", false)) {
            TranslationHelper.autoTranslate(context, holder.tvTitle, item.getTitle());
            TranslationHelper.autoTranslate(context, holder.tvDescShort, item.getMessage());
        }

        // ⭐ STYLING LOGIC
        if (item.getType() == 2) {
            // IMPORTANT: Automated Qualification
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0")); // Light Orange
            holder.imgIcon.setImageResource(R.drawable.ic_check_circle);
            holder.imgIcon.setColorFilter(Color.parseColor("#F5901A"));
            holder.tvTitle.setTextColor(Color.parseColor("#E65100"));
            if (holder.btnReapply != null) holder.btnReapply.setVisibility(View.GONE);

        } else if (item.getType() == 1 || item.getTitle().toLowerCase().contains("decline")) {
            // DANGER: Decline
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.imgIcon.setImageResource(R.drawable.ic_danger);
            holder.imgIcon.setColorFilter(null);
            holder.tvTitle.setTextColor(Color.parseColor("#D32F2F"));

            if (holder.btnReapply != null && (item.getTitle().toLowerCase().contains("account") || item.getTitle().toLowerCase().contains("registration"))) {
                holder.btnReapply.setVisibility(View.VISIBLE);
                holder.btnReapply.setOnClickListener(v -> handleReapply(context, holder, item));
            }
        } else {
            // NORMAL
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.imgIcon.setImageResource(R.drawable.ic_notifications);
            holder.imgIcon.setColorFilter(null);
            holder.tvTitle.setTextColor(Color.BLACK);
            if (holder.btnReapply != null) holder.btnReapply.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                NotificationDetail_fragment detailFragment = NotificationDetail_fragment.newInstance(item);
                ((androidx.fragment.app.FragmentActivity) v.getContext()).getSupportFragmentManager()
                        .beginTransaction().replace(R.id.fragment_container, detailFragment).addToBackStack(null).commit();
            }
        });
    }

    private void handleReapply(Context context, NotificationViewHolder holder, NotificationItem item) {
        if (!(context instanceof androidx.fragment.app.FragmentActivity)) return;
        ReapplyIdDialogFragment dialog = new ReapplyIdDialogFragment();
        dialog.setOnConfirmListener(imageBytes -> {
            if (imageBytes != null) {
                holder.btnReapply.setEnabled(false);
                holder.btnReapply.setText("Uploading...");
                processReapplication(context, holder, item, imageBytes);
            }
        });
        dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "ReapplyIdDialog");
    }

    private void processReapplication(Context context, NotificationViewHolder holder, NotificationItem item, byte[] imageBytes) {
        SupabaseJavaHelper.fetchUserProfile(context, new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                if (profile == null) { resetButton(holder, "Reapply Account"); return; }
                String userId = profile.getId();
                new Thread(() -> {
                    try {
                        String filename = "reapply_" + System.currentTimeMillis() + ".jpg";
                        RequestBody imgBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                        Request imgRequest = new Request.Builder()
                                .url(SUPABASE_URL + "/storage/v1/object/images/" + userId + "/" + filename)
                                .addHeader("apikey", SUPABASE_KEY).addHeader("Authorization", "Bearer " + SUPABASE_KEY).post(imgBody).build();

                        try (Response imgResponse = new OkHttpClient().newCall(imgRequest).execute()) {
                            if (imgResponse.isSuccessful()) {
                                String url = SUPABASE_URL + "/storage/v1/object/public/images/" + userId + "/" + filename;
                                JSONObject json = new JSONObject();
                                json.put("id_image_url", url);
                                json.put("account_status", "Pending");
                                json.put("is_verified", false);

                                Request dbRequest = new Request.Builder()
                                        .url(SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId)
                                        .addHeader("apikey", SUPABASE_KEY).addHeader("Authorization", "Bearer " + SUPABASE_KEY).patch(RequestBody.create(json.toString(), MediaType.parse("application/json"))).build();

                                if (new OkHttpClient().newCall(dbRequest).execute().isSuccessful()) {
                                    // Delete Notif
                                    Request del = new Request.Builder().url(SUPABASE_URL + "/rest/v1/notifications?notif_id=eq." + item.getId())
                                            .addHeader("apikey", SUPABASE_KEY).addHeader("Authorization", "Bearer " + SUPABASE_KEY).delete().build();
                                    new OkHttpClient().newCall(del).execute().close();

                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        Toast.makeText(context, "Reapplied successfully!", Toast.LENGTH_SHORT).show();
                                        notificationList.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                    });
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
            @Override public void onError(String message) { resetButton(holder, "Retry"); }
        });
    }

    private void resetButton(NotificationViewHolder holder, String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            holder.btnReapply.setEnabled(true);
            holder.btnReapply.setText(text);
        });
    }

    @Override public int getItemCount() { return notificationList.size(); }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvDescShort, tvDescFull;
        ImageView imgIcon;
        Button btnReapply;
        public NotificationViewHolder(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_notif_title);
            tvTime = v.findViewById(R.id.tv_notif_time);
            tvDescShort = v.findViewById(R.id.tv_notif_desc_short);
            tvDescFull = v.findViewById(R.id.tv_notif_desc_full);
            imgIcon = v.findViewById(R.id.img_notif_icon);
            btnReapply = v.findViewById(R.id.btn_reapply);
        }
    }
}