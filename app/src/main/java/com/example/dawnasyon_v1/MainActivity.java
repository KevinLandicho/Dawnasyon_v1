package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    LinearLayout homeTab, dashboardTab, notificationTab, profileTab;
    FrameLayout centerButton; // center button is a FrameLayout
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

        // Initialize tabs
        homeTab = findViewById(R.id.bottom_bar).findViewWithTag("homeTab");
        dashboardTab = findViewById(R.id.bottom_bar).findViewWithTag("dashboardTab");
        notificationTab = findViewById(R.id.bottom_bar).findViewWithTag("notificationTab");
        profileTab = findViewById(R.id.bottom_bar).findViewWithTag("profileTab");
        centerButton = findViewById(R.id.center_button);

        // Store all tabs for easy reset
        tabs = new LinearLayout[]{homeTab, dashboardTab, notificationTab, profileTab};

        // Load default fragment
        if (savedInstanceState == null) {
            selectTab(homeTab); // home selected by default
            loadFragment(new Home_fragment());
        }

        // Set click listeners
        homeTab.setOnClickListener(v -> { selectTab(homeTab); loadFragment(new Home_fragment()); });
        dashboardTab.setOnClickListener(v -> { selectTab(dashboardTab); loadFragment(new Dashboard_fragment()); });
        notificationTab.setOnClickListener(v -> { selectTab(notificationTab); loadFragment(new Notification_fragment()); });
        profileTab.setOnClickListener(v -> { selectTab(profileTab); loadFragment(new Profile_fragment()); });

        // Center diamond button
        centerButton.setOnClickListener(v -> {
            selectCenterButton(); // highlight the center button
            loadFragment(new AddDonation_Fragment());
        });
    }

    // Fragment loader
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    // Highlight bottom tab
    private void selectTab(LinearLayout selectedTab) {
        // Reset all tabs and center button
        resetTabs();
        resetCenterButton();

        // Highlight selected tab
        ImageView icon = (ImageView) selectedTab.getChildAt(0);
        TextView text = (TextView) selectedTab.getChildAt(1);
        text.setTextColor(getResources().getColor(android.R.color.white));

        if (selectedTab == homeTab) icon.setImageResource(R.drawable.ic_home);
        else if (selectedTab == dashboardTab) icon.setImageResource(R.drawable.ic_dashboard);
        else if (selectedTab == notificationTab) icon.setImageResource(R.drawable.ic_notifications);
        else if (selectedTab == profileTab) icon.setImageResource(R.drawable.ic_profile);
    }

    // Highlight center button
    private void selectCenterButton() {
        resetTabs();
        resetCenterButton();

        ImageView icon = centerButton.findViewById(R.id.ic_add_icon); // give your ImageView in XML an id
        icon.setImageResource(R.drawable.ic_add);
    }

    // Reset bottom tabs
    private void resetTabs() {
        resetTab(homeTab, R.drawable.ic_home_notselected);
        resetTab(dashboardTab, R.drawable.ic_dashboard_notselected);
        resetTab(notificationTab, R.drawable.ic_notifications_notselected);
        resetTab(profileTab, R.drawable.ic_profile_notselected);
    }

    // Reset center button
    private void resetCenterButton() {
        ImageView icon = centerButton.findViewById(R.id.ic_add_icon); // same id as above
        icon.setImageResource(R.drawable.ic_add_notselected);
    }

    // Reset individual tab
    private void resetTab(LinearLayout tab, int iconRes) {
        ImageView icon = (ImageView) tab.getChildAt(0);
        TextView text = (TextView) tab.getChildAt(1);
        icon.setImageResource(iconRes);
        text.setTextColor(getResources().getColor(R.color.unselected));
    }
}
