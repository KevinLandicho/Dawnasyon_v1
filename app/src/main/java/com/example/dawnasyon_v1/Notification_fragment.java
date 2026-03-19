package com.example.dawnasyon_v1;

import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Notification_fragment extends BaseFragment {

    private RecyclerView rvNew;
    private RecyclerView rvOld;
    private Button btnSeePrevious;
    private TextView tvHeaderNew, tvHeaderOld;
    private NestedScrollView contentLayout;

    private LinearLayout emptyStateLayout;
    private Button btnRefreshEmpty;

    // Declare adapters at the top so we don't recreate them
    private NotificationAdapter adapterNew;
    private NotificationAdapter adapterOld;

    // Static cache for instant loading between tabs
    private static List<NotificationItem> cachedNotificationList = null;

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

        contentLayout = view.findViewById(R.id.content_layout);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        btnRefreshEmpty = view.findViewById(R.id.btn_refresh_empty);

        rvNew.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOld.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapters ONCE with empty lists
        adapterNew = new NotificationAdapter(new ArrayList<>());
        adapterOld = new NotificationAdapter(new ArrayList<>());
        rvNew.setAdapter(adapterNew);
        rvOld.setAdapter(adapterOld);

        if (btnRefreshEmpty != null) {
            btnRefreshEmpty.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
                showEmptyState(true); // Show placeholder while refreshing
                fetchNotifications(true);
            });
        }

        // ⭐ FIXED LOGIC: Show placeholder if we don't have cached data yet!
        if (cachedNotificationList != null && !cachedNotificationList.isEmpty()) {
            processAndDisplay(cachedNotificationList); // Loads instantly
            fetchNotifications(false); // Silently checks for updates in background
        } else {
            showEmptyState(true); // Keep placeholder visible!
            fetchNotifications(true);
        }

        applyTagalogTranslation(view);
    }

    private void fetchNotifications(boolean forceUpdate) {
        if (getContext() == null) return;

        SupabaseJavaHelper.fetchNotifications(new SupabaseJavaHelper.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationItem> data) {
                if (!isAdded()) return;

                if (forceUpdate || hasNewNotifications(cachedNotificationList, data)) {
                    cachedNotificationList = data;
                    processAndDisplay(data);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                Log.e("NotifFrag", "Error: " + message);

                if (cachedNotificationList == null || cachedNotificationList.isEmpty()) {
                    showEmptyState(true);
                }
            }
        });
    }

    private boolean hasNewNotifications(List<NotificationItem> oldList, List<NotificationItem> newList) {
        if (oldList == null || newList == null) return true;
        if (oldList.size() != newList.size()) return true;
        if (oldList.isEmpty() && newList.isEmpty()) return false;

        String oldFirstItemDate = oldList.get(0).getCreatedAt();
        String newFirstItemDate = newList.get(0).getCreatedAt();

        return oldFirstItemDate != null && !oldFirstItemDate.equals(newFirstItemDate);
    }

    private void processAndDisplay(List<NotificationItem> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            showEmptyState(true);
            return;
        }

        // ⭐ FIXED: Removed the Background Thread. Processing it directly on the main thread
        // removes the visual "stutter" and makes it feel significantly faster.
        List<NotificationItem> newList = new ArrayList<>();
        List<NotificationItem> oldList = new ArrayList<>();

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String todayStr = dateFormatter.format(new Date());

        for (NotificationItem item : rawList) {
            try {
                if (item.getCreatedAt() != null) {
                    Date date = parser.parse(item.getCreatedAt());
                    if (date != null) {
                        item.setTime(timeFormatter.format(date));
                        String itemDateStr = dateFormatter.format(date);

                        if (itemDateStr.equals(todayStr)) {
                            item.setDateCategory("New");
                            newList.add(item);
                        } else {
                            item.setDateCategory("Old");
                            oldList.add(item);
                        }
                    }
                }

                if (item.getDbType() != null && item.getDbType().equalsIgnoreCase("Decline")) {
                    item.setType(1);
                } else {
                    item.setType(0);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showEmptyState(false);

        // Safely update the adapters without destroying them
        adapterNew.updateData(newList);
        adapterOld.updateData(oldList);

        tvHeaderNew.setVisibility(newList.isEmpty() ? View.GONE : View.VISIBLE);

        if (oldList.isEmpty()) {
            tvHeaderOld.setVisibility(View.GONE);
            btnSeePrevious.setVisibility(View.GONE);
        } else {
            tvHeaderOld.setVisibility(View.VISIBLE);
            btnSeePrevious.setVisibility(oldList.size() > 5 ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (emptyStateLayout == null || contentLayout == null) return;

        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
        }
    }
}