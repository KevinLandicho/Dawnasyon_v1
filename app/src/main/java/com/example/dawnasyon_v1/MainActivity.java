package com.example.dawnasyon_v1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends BaseActivity {

    LinearLayout homeTab, dashboardTab, notificationTab, profileTab;
    FrameLayout centerButton;
    LinearLayout[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bg_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // -----------------------------------------------------------
        // 1. FIREBASE NOTIFICATIONS SETUP
        // -----------------------------------------------------------
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d("FCM", "Token retrieved: " + token);
            saveTokenToSupabase(token);
        });

        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                .addOnCompleteListener(task -> {
                    String msg = "Subscribed to global notifications";
                    if (!task.isSuccessful()) {
                        msg = "Subscribe failed";
                    }
                    Log.d("FCM", msg);
                });

        // -----------------------------------------------------------
        // 2. SETUP UI & TABS
        // -----------------------------------------------------------
        homeTab = findViewById(R.id.bottom_bar).findViewWithTag("homeTab");
        dashboardTab = findViewById(R.id.bottom_bar).findViewWithTag("dashboardTab");
        notificationTab = findViewById(R.id.bottom_bar).findViewWithTag("notificationTab");
        profileTab = findViewById(R.id.bottom_bar).findViewWithTag("profileTab");
        centerButton = findViewById(R.id.center_button);

        tabs = new LinearLayout[]{homeTab, dashboardTab, notificationTab, profileTab};

        if (savedInstanceState == null) {
            selectTab(homeTab);
            loadFragment(new Home_fragment());
        }

        homeTab.setOnClickListener(v -> { selectTab(homeTab); loadFragment(new Home_fragment()); });
        dashboardTab.setOnClickListener(v -> { selectTab(dashboardTab); loadFragment(new Dashboard_fragment()); });
        notificationTab.setOnClickListener(v -> { selectTab(notificationTab); loadFragment(new Notification_fragment()); });
        profileTab.setOnClickListener(v -> { selectTab(profileTab); loadFragment(new Profile_fragment()); });

        centerButton.setOnClickListener(v -> {
            selectCenterButton();
            loadFragment(new AddDonation_Fragment());
        });
    }

    // -----------------------------------------------------------
    // ⭐ NEW: SECURITY CHECK (Runs every time app opens)
    // -----------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        checkSecurityTimer();
    }

    private void checkSecurityTimer() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);

        // 1. Check if user has a face registered
        String faceData = prefs.getString("face_embedding", "");
        if (faceData.isEmpty()) {
            // If you see this toast, it means you need to Logout & Login again!
            android.widget.Toast.makeText(this, "DEBUG: No Face Data found in phone memory.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        // 2. Get Timestamps
        long lastTime = prefs.getLong("last_verified_timestamp", 0);
        long currentTime = System.currentTimeMillis();

        // 3. Set Limit (24 Hours = 86400000 ms)
        // 💡 TIP: Change this to 60000 (1 min) to test it quickly!
        long timeLimit = 86400000;
        long timelim = 10000;

        // 4. Check if expired
        if (currentTime - lastTime > timelim) {
            Intent intent = new Intent(this, FaceVerifyActivity.class);
            // Clear back stack so they can't press "Back" to return here
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    // -----------------------------------------------------------
    // 3. HELPERS
    // -----------------------------------------------------------
    private void saveTokenToSupabase(String token) {
        SupabaseManager.saveFcmToken(token);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void selectTab(LinearLayout selectedTab) {
        resetTabs();
        resetCenterButton();

        ImageView icon = (ImageView) selectedTab.getChildAt(0);
        TextView text = (TextView) selectedTab.getChildAt(1);
        text.setTextColor(getResources().getColor(android.R.color.white));

        if (selectedTab == homeTab) icon.setImageResource(R.drawable.ic_home);
        else if (selectedTab == dashboardTab) icon.setImageResource(R.drawable.ic_dashboard);
        else if (selectedTab == notificationTab) icon.setImageResource(R.drawable.ic_notifications);
        else if (selectedTab == profileTab) icon.setImageResource(R.drawable.ic_profile);
    }

    private void selectCenterButton() {
        resetTabs();
        resetCenterButton();
        ImageView icon = centerButton.findViewById(R.id.ic_add_icon);
        icon.setImageResource(R.drawable.ic_add);
    }

    private void resetTabs() {
        resetTab(homeTab, R.drawable.ic_home_notselected);
        resetTab(dashboardTab, R.drawable.ic_dashboard_notselected);
        resetTab(notificationTab, R.drawable.ic_notifications_notselected);
        resetTab(profileTab, R.drawable.ic_profile_notselected);
    }

    private void resetCenterButton() {
        ImageView icon = centerButton.findViewById(R.id.ic_add_icon);
        icon.setImageResource(R.drawable.ic_add_notselected);
    }

    private void resetTab(LinearLayout tab, int iconRes) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);
        icon.setImageResource(iconRes);
        text.setTextColor(getResources().getColor(R.color.unselected));
    }
}