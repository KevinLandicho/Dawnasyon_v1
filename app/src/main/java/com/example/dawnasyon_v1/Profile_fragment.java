package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

public class Profile_fragment extends BaseFragment {

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

        // ⭐ Check if you added the Language button in XML


        // Find the top buttons
        Button btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        Button btnViewQR = view.findViewById(R.id.btn_view_qr);

        // --- 2. Fix: Find and set the user detail text views ---
        TextView detailName = view.findViewById(R.id.detail_name);
        TextView detailAddress = view.findViewById(R.id.detail_address);
        TextView detailContact = view.findViewById(R.id.detail_contact);
        TextView userNameHeader = view.findViewById(R.id.user_name);

        // Set placeholder data
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

        // Initialize Language Button if it exists


        // --- 4. Attach Click Listeners ---

        // Edit Profile
        btnEditProfile.setOnClickListener(v -> navigateToFragment(new EditProfile_fragment()));

        // View QR
        btnViewQR.setOnClickListener(v -> {
            int userQrCode = R.drawable.ic_qrsample;
            Fragment qrFragment = DisplayQR_fragment.newInstance(userQrCode);
            navigateToFragment(qrFragment);
        });

        // Menus
        menuHistory.setOnClickListener(v -> navigateToFragment(new DonationHistory_fragment()));
        menuSuggestion.setOnClickListener(v -> navigateToFragment(new SuggestionForm_fragment()));
        menuPassword.setOnClickListener(v -> navigateToFragment(new ChangePassword_fragment()));
        menuDelete.setOnClickListener(v -> navigateToFragment(new DeleteAccount_fragment()));

        // ⭐ LOGOUT LISTENER (Shows Confirmation Dialog) ⭐
        menuLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        // ⭐ LANGUAGE LISTENER (Toggles Language) ⭐


    }

    /**
     * Shows the "Are you sure you want to log out?" dialog.
     */
    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;

        // Inflate the custom dialog layout (reusing the one we made earlier)
        // If you don't have dialog_logout_confirmation.xml, use a standard AlertDialog below
        try {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout_confirmation, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
            Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_logout);

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                performLogout(); // Call the actual logout logic
            });

            dialog.show();
        } catch (Exception e) {
            // Fallback if custom layout is missing
            new AlertDialog.Builder(getContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> performLogout())
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    /**
     * CLEARS SESSION AND GOES TO LOGIN
     */
    private void performLogout() {
        if (getActivity() == null) return;

        // 1. Clear SharedPreferences "UserSession"
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Wipes the "isLoggedIn" flag
        editor.apply();

        Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // 2. Navigate back to LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        // Clear the back stack so user can't press "Back" to return to Profile
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

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

    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}