package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
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
    private TextView tvHeaderNew, tvHeaderOld, tvTitle;
    private NestedScrollView contentLayout;
    private LinearLayout emptyStateLayout;
    private Button btnRefreshEmpty;
    private MaterialButton btnLanguage;

    private NotificationAdapter adapterNew;
    private NotificationAdapter adapterOld;

    private boolean isTagalogEnabled = false;

    public Notification_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        rvNew = view.findViewById(R.id.rv_new);
        rvOld = view.findViewById(R.id.rv_old);
        tvHeaderNew = view.findViewById(R.id.tv_header_new);
        tvHeaderOld = view.findViewById(R.id.tv_header_old);
        tvTitle = view.findViewById(R.id.tv_title);
        contentLayout = view.findViewById(R.id.content_layout);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        btnRefreshEmpty = view.findViewById(R.id.btn_refresh_empty);
        btnLanguage = view.findViewById(R.id.btn_language);

        // 2. Setup Recycler
        rvNew.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOld.setLayoutManager(new LinearLayoutManager(getContext()));

        adapterNew = new NotificationAdapter(new ArrayList<>());
        adapterOld = new NotificationAdapter(new ArrayList<>());
        rvNew.setAdapter(adapterNew);
        rvOld.setAdapter(adapterOld);

        if (btnRefreshEmpty != null) {
            btnRefreshEmpty.setOnClickListener(v -> fetchNotifications());
        }

        // --- ⭐ 3. LANGUAGE TOGGLE LOGIC ---
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        isTagalogEnabled = prefs.getBoolean("is_tagalog", false);
        updateLanguageButtonUI();

        btnLanguage.setOnClickListener(v -> {
            btnLanguage.setEnabled(false);

            isTagalogEnabled = !isTagalogEnabled;
            prefs.edit().putBoolean("is_tagalog", isTagalogEnabled).apply();

            TranslationHelper.translateViewHierarchy(requireContext(), view);

            // Force update UI text
            updateLanguageButtonUI();

            // Fetch notifications again to translate the dynamic list items!
            fetchNotifications();

            String msg = isTagalogEnabled ? "Tagalog Mode ON" : "English Mode ON";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && btnLanguage != null) {
                    btnLanguage.setEnabled(true);
                }
            }, 1000);
        });
        // -----------------------------------

        // Start fresh every time to avoid cache lag
        showEmptyState(true);
        fetchNotifications();

        applyTagalogTranslation(view);
    }

    // ⭐ HELPER: Updates the button appearance and translates static headers manually
    private void updateLanguageButtonUI() {
        if (btnLanguage == null) return;

        if (isTagalogEnabled) {
            btnLanguage.setText("TAGALOG");
            btnLanguage.setIconResource(R.drawable.ic_check_circle);

            // ⭐ Manual Header Translations
            if (tvHeaderOld != null) tvHeaderOld.setText("Lumang abiso");
            if (tvHeaderNew != null) tvHeaderNew.setText("Bago");

        } else {
            btnLanguage.setText("ENGLISH");
            btnLanguage.setIcon(null);

            // Revert back to English
            if (tvHeaderOld != null) tvHeaderOld.setText("Old");
            if (tvHeaderNew != null) tvHeaderNew.setText("New");
        }
    }

    private void fetchNotifications() {
        if (!isAdded() || getContext() == null) return;

        SupabaseJavaHelper.fetchNotifications(new SupabaseJavaHelper.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationItem> data) {
                if (!isAdded()) return;

                if (data != null && !data.isEmpty()) {
                    processAndDisplay(data);
                } else {
                    showEmptyState(true);
                }
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
                if (item.getCreatedAt() != null) {
                    Date date = parser.parse(item.getCreatedAt());
                    if (date != null) {
                        item.setTime(timeFormatter.format(date));
                        if (dateFormatter.format(date).equals(todayStr)) {
                            item.setDateCategory("New");
                            newList.add(item);
                        } else {
                            item.setDateCategory("Old");
                            oldList.add(item);
                        }
                    }
                }
                item.setType((item.getDbType() != null && item.getDbType().equalsIgnoreCase("Decline")) ? 1 : 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showEmptyState(false);

        adapterNew.updateData(newList);
        adapterOld.updateData(oldList);

        // Update Header Visibility
        tvHeaderNew.setVisibility(newList.isEmpty() ? View.GONE : View.VISIBLE);
        tvHeaderOld.setVisibility(oldList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean isEmpty) {
        if (emptyStateLayout == null || contentLayout == null) return;
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}