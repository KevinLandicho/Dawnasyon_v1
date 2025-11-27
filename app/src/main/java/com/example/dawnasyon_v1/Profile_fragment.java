package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Profile_fragment extends Fragment {

    public Profile_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- 1. Find the menu item containers ---
        LinearLayout menuHistory = view.findViewById(R.id.menu_history);
        LinearLayout menuSuggestion = view.findViewById(R.id.menu_suggestion);
        LinearLayout menuPassword = view.findViewById(R.id.menu_password);
        LinearLayout menuDelete = view.findViewById(R.id.menu_delete);
        LinearLayout menuLogout = view.findViewById(R.id.menu_logout);

        // Find the top buttons
        Button btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        Button btnViewQR = view.findViewById(R.id.btn_view_qr);

        // --- 2. Fix: Find and set the user detail text views ---
        TextView detailName = view.findViewById(R.id.detail_name);
        TextView detailAddress = view.findViewById(R.id.detail_address);
        TextView detailContact = view.findViewById(R.id.detail_contact);
        TextView userNameHeader = view.findViewById(R.id.user_name);

        // Set placeholder data to make the fields visible on the device
        if (detailName != null) detailName.setText("Juan M. Dela Cruz");
        if (detailAddress != null) detailAddress.setText("123 Carmel V Compound Tandang Sora Quezon City");
        if (detailContact != null) detailContact.setText("09123456789");
        if (userNameHeader != null) userNameHeader.setText("Juan M. Dela Cruz");

        // --- 3. Initialize Icons and Titles ---
        setupMenuItem(menuHistory, R.drawable.ic_history, "Donation history");
        setupMenuItem(menuSuggestion, R.drawable.ic_suggestion, "Suggestion form");
        setupMenuItem(menuPassword, R.drawable.ic_lock, "Change password");
        setupMenuItem(menuDelete, R.drawable.ic_delete, "Delete account");
        setupMenuItem(menuLogout, R.drawable.ic_logout, "Log out");


        // --- 4. Attach Click Listeners ---

        // ⭐ NEW: Edit Profile Button now navigates to EditProfile_fragment ⭐
        btnEditProfile.setOnClickListener(v -> navigateToFragment(new EditProfile_fragment()));

        btnViewQR.setOnClickListener(v -> handleMenuClick("View QR Code"));

        // Menu Items
        menuHistory.setOnClickListener(v -> handleMenuClick("Donation History"));
        menuSuggestion.setOnClickListener(v -> handleMenuClick("Suggestion Form"));
        menuPassword.setOnClickListener(v -> handleMenuClick("Change Password"));

        menuDelete.setOnClickListener(v -> handleMenuClick("Delete Account"));
        menuLogout.setOnClickListener(v -> handleMenuClick("Log Out"));
    }

    /**
     * Helper method to set the icon and title for an included menu item (item_profile_menu.xml).
     */
    private void setupMenuItem(View includedView, int iconResId, String title) {
        ImageView iconView = includedView.findViewById(R.id.menu_icon);
        TextView titleView = includedView.findViewById(R.id.menu_title);

        if (iconView != null) {
            iconView.setImageResource(iconResId);
        }
        if (titleView != null) {
            titleView.setText(title);
            if (title.equals("Delete account")) {
                titleView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }

    /**
     * Handles the transition to a new fragment.
     * @param fragment The fragment instance to navigate to.
     */
    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment) // ⭐ IMPORTANT: Replace R.id.fragment_container with your actual host ID ⭐
                    .addToBackStack(null) // Allows the user to press the back button to return here
                    .commit();
        }
    }

    /**
     * Generic handler for menu item clicks.
     */
    private void handleMenuClick(String action) {
        Toast.makeText(getContext(), "Navigating to: " + action, Toast.LENGTH_SHORT).show();
    }
}