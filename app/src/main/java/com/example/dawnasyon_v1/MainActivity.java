package com.example.dawnasyon_v1;

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

        // A. Get Individual Device Token (For specific user notifications)
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d("FCM", "Token retrieved: " + token);
            saveTokenToSupabase(token);
        });

        // B. [NEW] Subscribe to "all_users" Topic (For "Notify All" feature)
        // This makes sure this user receives broadcasts sent to "all_users"
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

        // Load default fragment
        if (savedInstanceState == null) {
            selectTab(homeTab);
            loadFragment(new Home_fragment());
        }

        // Set click listeners
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
    // 3. HELPER FUNCTION: Save Token
    // -----------------------------------------------------------
    private void saveTokenToSupabase(String token) {
        SupabaseManager.saveFcmToken(token);
    }

    // -----------------------------------------------------------
    // 4. UI HELPERS (Fragment & Tabs)
    // -----------------------------------------------------------
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