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

    // ⭐ Supabase Config
    private static final String SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi";

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

        holder.tvTitle.setText(item.getTitle());
        holder.tvTime.setText(item.getTime());
        holder.tvDescShort.setText(item.getDescription());

        if (holder.tvDescFull != null) {
            holder.tvDescFull.setText(item.getDescription());
        }

        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isTagalog = prefs.getBoolean("is_tagalog", false);

        if (isTagalog) {
            TranslationHelper.autoTranslate(context, holder.tvTitle, item.getTitle());
            TranslationHelper.autoTranslate(context, holder.tvDescShort, item.getDescription());
            if (holder.tvDescFull != null) {
                TranslationHelper.autoTranslate(context, holder.tvDescFull, item.getDescription());
            }
        }

        String titleLower = item.getTitle().toLowerCase();

        if (item.getType() == 1 || titleLower.contains("decline") || titleLower.contains("reject")) {
            holder.imgIcon.setImageResource(R.drawable.ic_danger);
            holder.tvTitle.setTextColor(Color.parseColor("#D32F2F"));

            if (holder.btnReapply != null && (titleLower.contains("account") || titleLower.contains("registration") || titleLower.contains("decline"))) {
                holder.btnReapply.setVisibility(View.VISIBLE);

                holder.btnReapply.setOnClickListener(v -> {
                    if (context instanceof androidx.fragment.app.FragmentActivity) {
                        androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;

                        ReapplyIdDialogFragment dialog = new ReapplyIdDialogFragment();
                        dialog.setOnConfirmListener(imageBytes -> {
                            if (imageBytes != null) {
                                Toast.makeText(context, "Uploading new ID and reapplying...", Toast.LENGTH_SHORT).show();
                                holder.btnReapply.setEnabled(false);
                                holder.btnReapply.setText("Uploading...");

                                // Pass the item to the processor so we know what to delete
                                processReapplication(context, holder, item, imageBytes);
                            }
                        });
                        dialog.show(activity.getSupportFragmentManager(), "ReapplyIdDialog");
                    }
                });
            } else if (holder.btnReapply != null) {
                holder.btnReapply.setVisibility(View.GONE);
            }

        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_notifications);
            holder.tvTitle.setTextColor(Color.BLACK);

            if (holder.btnReapply != null) {
                holder.btnReapply.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) v.getContext();
                NotificationDetail_fragment detailFragment = NotificationDetail_fragment.newInstance(item);
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void processReapplication(Context context, NotificationViewHolder holder, NotificationItem item, byte[] imageBytes) {
        SupabaseJavaHelper.fetchUserProfile(context, new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                if (profile == null) {
                    resetButton(holder, "Reapply Account");
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userId = profile.getId();
                OkHttpClient client = new OkHttpClient();

                new Thread(() -> {
                    try {
                        // 1. UPLOAD IMAGE
                        String filename = "reapply_" + System.currentTimeMillis() + ".jpg";
                        RequestBody imgBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                        Request imgRequest = new Request.Builder()
                                .url(SUPABASE_URL + "/storage/v1/object/images/" + userId + "/" + filename)
                                .addHeader("apikey", SUPABASE_KEY)
                                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                                .post(imgBody)
                                .build();

                        try (Response imgResponse = client.newCall(imgRequest).execute()) {
                            if (imgResponse.isSuccessful()) {
                                String uploadedImageUrl = SUPABASE_URL + "/storage/v1/object/public/images/" + userId + "/" + filename;

                                // 2. UPDATE PROFILE
                                JSONObject json = new JSONObject();
                                json.put("id_image_url", uploadedImageUrl);
                                json.put("account_status", "Pending");
                                json.put("is_verified", false);
                                json.put("rejection_reason", "");

                                RequestBody dbBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                                Request dbRequest = new Request.Builder()
                                        .url(SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId)
                                        .addHeader("apikey", SUPABASE_KEY)
                                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                                        .addHeader("Prefer", "return=minimal")
                                        .patch(dbBody)
                                        .build();

                                try (Response dbResponse = client.newCall(dbRequest).execute()) {
                                    if (dbResponse.isSuccessful()) {

                                        // ⭐ 3. DELETE NOTIFICATION FROM DATABASE
                                        // Note: Make sure NotificationItem has a getId() method that returns the `notif_id`
                                        String notifId = String.valueOf(item.getId());

                                        Request deleteRequest = new Request.Builder()
                                                .url(SUPABASE_URL + "/rest/v1/notifications?notif_id=eq." + notifId)
                                                .addHeader("apikey", SUPABASE_KEY)
                                                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                                                .delete()
                                                .build();

                                        client.newCall(deleteRequest).execute().close(); // Execute deletion

                                        // 4. UPDATE UI
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            Toast.makeText(context, "Successfully reapplied! Waiting for approval.", Toast.LENGTH_LONG).show();

                                            // Remove from Recycler View visually
                                            int currentPosition = holder.getAdapterPosition();
                                            if (currentPosition != RecyclerView.NO_POSITION) {
                                                notificationList.remove(currentPosition);
                                                notifyItemRemoved(currentPosition);
                                            }
                                        });

                                    } else {
                                        showError(context, holder, "Failed to update profile data.");
                                    }
                                }
                            } else {
                                showError(context, holder, "Failed to upload image.");
                            }
                        }
                    } catch (Exception e) {
                        showError(context, holder, e.getMessage());
                    }
                }).start();
            }

            @Override
            public void onError(String message) {
                resetButton(holder, "Reapply Account");
                Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showError(Context context, NotificationViewHolder holder, String errorMsg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            resetButton(holder, "Retry Reapply");
            Toast.makeText(context, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
        });
    }

    private void resetButton(NotificationViewHolder holder, String text) {
        holder.btnReapply.setEnabled(true);
        holder.btnReapply.setText(text);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvDescShort, tvDescFull;
        ImageView imgIcon;
        Button btnReapply;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            tvDescShort = itemView.findViewById(R.id.tv_notif_desc_short);
            tvDescFull = itemView.findViewById(R.id.tv_notif_desc_full);
            imgIcon = itemView.findViewById(R.id.img_notif_icon);
            btnReapply = itemView.findViewById(R.id.btn_reapply);
        }
    }
}