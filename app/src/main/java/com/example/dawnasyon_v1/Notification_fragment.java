package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Notification_fragment extends BaseFragment {

    private RecyclerView rvNew, rvOld;
    private TextView tvHeaderNew, tvHeaderOld, tvEmptyTitle, tvEmptyDesc;
    private NestedScrollView contentLayout;
    private LinearLayout emptyStateLayout;
    private Button btnRefreshEmpty;
    private MaterialButton btnLanguage, btnFilterImportant;

    private NotificationAdapter adapterNew;
    private NotificationAdapter adapterOld;

    // ⭐ Master list to hold data so we don't have to redownload when filtering
    private List<NotificationItem> masterNotificationList = new ArrayList<>();

    private boolean isTagalogEnabled = false;
    private boolean isFilterImportant = false; // Filter state

    public Notification_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNew = view.findViewById(R.id.rv_new);
        rvOld = view.findViewById(R.id.rv_old);
        tvHeaderNew = view.findViewById(R.id.tv_header_new);
        tvHeaderOld = view.findViewById(R.id.tv_header_old);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_empty_desc);
        contentLayout = view.findViewById(R.id.content_layout);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        btnRefreshEmpty = view.findViewById(R.id.btn_refresh_empty);
        btnLanguage = view.findViewById(R.id.btn_language);
        btnFilterImportant = view.findViewById(R.id.btn_filter_important);

        rvNew.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOld.setLayoutManager(new LinearLayoutManager(getContext()));

        adapterNew = new NotificationAdapter(new ArrayList<>());
        adapterOld = new NotificationAdapter(new ArrayList<>());
        rvNew.setAdapter(adapterNew);
        rvOld.setAdapter(adapterOld);

        if (btnRefreshEmpty != null) {
            btnRefreshEmpty.setOnClickListener(v -> fetchNotifications());
        }

        // Language Logic
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        isTagalogEnabled = prefs.getBoolean("is_tagalog", false);
        updateLanguageButtonUI();

        btnLanguage.setOnClickListener(v -> {
            btnLanguage.setEnabled(false);
            isTagalogEnabled = !isTagalogEnabled;
            prefs.edit().putBoolean("is_tagalog", isTagalogEnabled).apply();
            TranslationHelper.translateViewHierarchy(requireContext(), view);
            updateLanguageButtonUI();

            // Re-render the current view based on filter
            processAndDisplay(masterNotificationList);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && btnLanguage != null) btnLanguage.setEnabled(true);
            }, 1000);
        });

        // ⭐ Filter Logic
        btnFilterImportant.setOnClickListener(v -> {
            isFilterImportant = !isFilterImportant;

            // Update button visual
            if (isFilterImportant) {
                btnFilterImportant.setBackgroundColor(Color.parseColor("#FFF3E0")); // Orange tint
                btnFilterImportant.setTextColor(Color.parseColor("#E65100"));
                btnFilterImportant.setStrokeColorResource(android.R.color.transparent);
                tvEmptyTitle.setText(isTagalogEnabled ? "Walang Mahalagang Abiso" : "No Important Notifications");
                tvEmptyDesc.setText(isTagalogEnabled ? "Makikita mo rito ang mga relief qualifications." : "Automated qualifications will appear here.");
            } else {
                btnFilterImportant.setBackgroundColor(Color.TRANSPARENT);
                btnFilterImportant.setTextColor(Color.parseColor("#9E9E9E")); // Gray out
                btnFilterImportant.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                tvEmptyTitle.setText(isTagalogEnabled ? "Walang Abiso" : "No Notifications Yet");
                tvEmptyDesc.setText(isTagalogEnabled ? "Dito makikita ang mga abiso." : "When you get notifications, they'll show up here.");
            }

            // Instantly re-process the list!
            processAndDisplay(masterNotificationList);
        });

        fetchNotifications();
        applyTagalogTranslation(view);
    }

    private void updateLanguageButtonUI() {
        if (btnLanguage == null) return;

        if (isTagalogEnabled) {
            btnLanguage.setText("TAGALOG");
            btnLanguage.setIconResource(R.drawable.ic_check_circle);
            if (tvHeaderOld != null) tvHeaderOld.setText("Lumang abiso");
            if (tvHeaderNew != null) tvHeaderNew.setText("Bago");
            if (btnFilterImportant != null && !isFilterImportant) btnFilterImportant.setText("⭐ Mahalaga");
        } else {
            btnLanguage.setText("ENGLISH");
            btnLanguage.setIcon(null);
            if (tvHeaderOld != null) tvHeaderOld.setText("Old");
            if (tvHeaderNew != null) tvHeaderNew.setText("New");
            if (btnFilterImportant != null && !isFilterImportant) btnFilterImportant.setText("⭐ Show Important");
        }
    }

    private void fetchNotifications() {
        if (!isAdded() || getContext() == null) return;

        SupabaseJavaHelper.fetchNotifications(new SupabaseJavaHelper.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationItem> data) {
                if (!isAdded()) return;

                // Save to master list
                masterNotificationList.clear();
                if (data != null) {
                    masterNotificationList.addAll(data);
                }

                processAndDisplay(masterNotificationList);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                Log.e("NotifFrag", "Error: " + message);
                showEmptyState(true);
            }
        });
    }

    private void processAndDisplay(List<NotificationItem> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            showEmptyState(true);
            return;
        }

        List<NotificationItem> newList = new ArrayList<>();
        List<NotificationItem> oldList = new ArrayList<>();

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = dateFormatter.format(new Date());

        for (NotificationItem item : rawList) {
            try {
                String message = item.getMessage() != null ? item.getMessage().toLowerCase() : "";
                String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";

                if (message.contains("automated relief qualification") || title.contains("automated relief qualification")) {
                    item.setType(2); // Important Notification
                } else {
                    item.setType((item.getDbType() != null && item.getDbType().equalsIgnoreCase("Decline")) ? 1 : 0);
                }

                // ⭐ FILTER CHECK: If important filter is ON, skip normal and declined notifications
                if (isFilterImportant && item.getType() != 2) {
                    continue;
                }

                String rawDate = item.getCreatedAt();
                Date date = null;

                if (rawDate != null && rawDate.length() >= 19) {
                    String cleanDate = rawDate.substring(0, 19);
                    try {
                        date = parser.parse(cleanDate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (date != null) {
                    item.setTime(timeFormatter.format(date));
                    if (dateFormatter.format(date).equals(todayStr)) {
                        item.setDateCategory("New");
                        newList.add(item);
                    } else {
                        item.setDateCategory("Old");
                        oldList.add(item);
                    }
                } else {
                    item.setTime("Recent");
                    item.setDateCategory("Old");
                    oldList.add(item);
                }

            } catch (Exception e) {
                Log.e("NotifError", "Failed to process item: " + e.getMessage());
            }
        }

        showEmptyState(newList.isEmpty() && oldList.isEmpty());

        adapterNew.updateData(newList);
        adapterOld.updateData(oldList);

        tvHeaderNew.setVisibility(newList.isEmpty() ? View.GONE : View.VISIBLE);
        tvHeaderOld.setVisibility(oldList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean isEmpty) {
        if (emptyStateLayout == null || contentLayout == null) return;
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}