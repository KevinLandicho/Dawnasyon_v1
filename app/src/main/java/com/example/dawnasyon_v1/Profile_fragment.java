package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.material.button.MaterialButton;

import java.util.List; // Import List

public class Profile_fragment extends BaseFragment {

    // ⭐ Container for dynamic family list
    private LinearLayout familyContainer;

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

        // --- 1. Find existing views ---
        LinearLayout menuHistory = view.findViewById(R.id.menu_history);
        LinearLayout menuSuggestion = view.findViewById(R.id.menu_suggestion);
        LinearLayout menuPassword = view.findViewById(R.id.menu_password);
        LinearLayout menuDelete = view.findViewById(R.id.menu_delete);
        LinearLayout menuLogout = view.findViewById(R.id.menu_logout);

        Button btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        Button btnViewQR = view.findViewById(R.id.btn_view_qr);
        MaterialButton btnPinLocation = view.findViewById(R.id.btn_pin_location);

        TextView detailName = view.findViewById(R.id.detail_name);
        TextView detailAddress = view.findViewById(R.id.detail_address);
        TextView detailContact = view.findViewById(R.id.detail_contact);
        TextView userNameHeader = view.findViewById(R.id.user_name);

        // ⭐ Find the new dynamic container
        familyContainer = view.findViewById(R.id.ll_family_container);

        // --- 2. LOAD DATA ---

        // A. Load Profile Info (Existing)
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                if (detailName != null) detailName.setText(profile.getFull_name());
                if (userNameHeader != null) userNameHeader.setText(profile.getFull_name());
                if (detailContact != null) detailContact.setText(profile.getContact_number());

                String fullAddress = "";
                if(profile.getHouse_number() != null) fullAddress += profile.getHouse_number() + " ";
                if(profile.getStreet() != null) fullAddress += profile.getStreet() + ", ";
                if(profile.getBarangay() != null) fullAddress += profile.getBarangay() + ", ";
                if(profile.getCity() != null) fullAddress += profile.getCity();

                if (detailAddress != null) detailAddress.setText(fullAddress);
            } else if (isAdded()) {
                Log.e("ProfileFragment", "Failed to load profile data.");
            }
            return null;
        });

        // B. ⭐ Load Family Tree
        loadFamilyMembers();

        // --- 3. Setup Listeners (Existing) ---
        setupMenuItem(menuHistory, R.drawable.ic_history, "Donation history");
        setupMenuItem(menuSuggestion, R.drawable.ic_suggestion, "Suggestion form");
        setupMenuItem(menuPassword, R.drawable.ic_lock, "Change password");
        setupMenuItem(menuDelete, R.drawable.ic_delete, "Delete account");
        setupMenuItem(menuLogout, R.drawable.ic_logout, "Log out");

        btnEditProfile.setOnClickListener(v -> navigateToFragment(new EditProfile_fragment()));

        btnViewQR.setOnClickListener(v -> {
            int userQrCode = R.drawable.ic_qrsample;
            Fragment qrFragment = DisplayQR_fragment.newInstance(userQrCode);
            navigateToFragment(qrFragment);
        });

        if (btnPinLocation != null) {
            btnPinLocation.setOnClickListener(v -> {
                String addressToPin = "Barangay Hall";
                if (detailAddress != null && detailAddress.getText() != null) {
                    addressToPin = detailAddress.getText().toString();
                }
                Fragment mapFragment = LiveMap_fragment.newInstance(addressToPin);
                navigateToFragment(mapFragment);
                Toast.makeText(getContext(), "Locating: " + addressToPin, Toast.LENGTH_SHORT).show();
            });
        }

        menuHistory.setOnClickListener(v -> navigateToFragment(new DonationHistory_fragment()));
        menuSuggestion.setOnClickListener(v -> navigateToFragment(new SuggestionForm_fragment()));
        menuPassword.setOnClickListener(v -> navigateToFragment(new ChangePassword_fragment()));
        menuDelete.setOnClickListener(v -> navigateToFragment(new DeleteAccount_fragment()));
        menuLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    /**
     * ⭐ FETCHES AND DISPLAYS DYNAMIC FAMILY ROWS
     */
    private void loadFamilyMembers() {
        AuthHelper.fetchHouseholdMembers(members -> {
            if (isAdded() && familyContainer != null) {
                familyContainer.removeAllViews();

                if (members == null || members.isEmpty()) {
                    TextView emptyView = new TextView(getContext());
                    emptyView.setText("No registered members found.");
                    emptyView.setPadding(0, 8, 0, 8);
                    familyContainer.addView(emptyView);
                } else {
                    for (int i = 0; i < members.size(); i++) {
                        HouseholdMember member = members.get(i);
                        addMemberRow(member);

                        // Add separator line unless it's the last item
                        if (i < members.size() - 1) {
                            View line = new View(getContext());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 2); // 1dp height approx
                            params.setMargins(0, 12, 0, 12);
                            line.setLayoutParams(params);
                            line.setBackgroundColor(Color.parseColor("#F0F0F0"));
                            familyContainer.addView(line);
                        }
                    }
                }
            }
            return null;
        });
    }

    /**
     * Helper to create a row that looks exactly like your XML design
     */
    /**
     * Helper to create a row that looks exactly like your XML design
     */
    private void addMemberRow(HouseholdMember member) {
        // ... (Previous Circle Indicator and Name code remains the same) ...
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);

        // 1. Circle Indicator
        ImageView indicator = new ImageView(getContext());
        indicator.setImageResource(R.drawable.ic_circle_indicator);
        indicator.setColorFilter(Color.parseColor("#F5901A"));
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(24, 24);
        imgParams.setMargins(0, 0, 24, 0);
        indicator.setLayoutParams(imgParams);
        row.addView(indicator);

        // 2. Name Column
        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(textParams);

        TextView tvName = new TextView(getContext());
        tvName.setText(member.getFull_name());
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextSize(14f);
        tvName.setTextColor(Color.BLACK);
        textCol.addView(tvName);

        TextView tvRelation = new TextView(getContext());
        tvRelation.setText(member.getRelation());
        tvRelation.setTextSize(11f);
        tvRelation.setTextColor(Color.parseColor("#666666"));
        textCol.addView(tvRelation);
        row.addView(textCol);

        // 3. ⭐ REAL BADGE LOGIC (Updated)
        TextView badge = new TextView(getContext());

        // Check the database value using the helper we added in Step 1
        if (member.getCensusStatus()) {
            badge.setText("✅ Registered");
            badge.setTextColor(Color.parseColor("#2E7D32")); // Green
            badge.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green
        } else {
            badge.setText("❌ Not Registered");
            badge.setTextColor(Color.parseColor("#757575")); // Grey
            badge.setBackgroundColor(Color.parseColor("#F5F5F5")); // Light Grey
        }

        badge.setTextSize(10f);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setPadding(12, 6, 12, 6);
        row.addView(badge);

        familyContainer.addView(row);
    }

    // ... (Existing Logout and Navigation methods remain unchanged) ...
    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;
        try {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout_confirmation, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
            Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_logout);
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                performLogout();
            });
            dialog.show();
        } catch (Exception e) {
            new AlertDialog.Builder(getContext()).setTitle("Log Out").setMessage("Are you sure?").setPositiveButton("Yes", (d, w) -> performLogout()).setNegativeButton("Cancel", null).show();
        }
    }

    private void performLogout() {
        if (getActivity() == null) return;
        AuthHelper.logoutUser();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setupMenuItem(View includedView, int iconResId, String title) {
        if (includedView == null) return;
        ImageView iconView = includedView.findViewById(R.id.menu_icon);
        TextView titleView = includedView.findViewById(R.id.menu_title);
        if (iconView != null) iconView.setImageResource(iconResId);
        if (titleView != null) {
            titleView.setText(title);
            if (title.equals("Delete account")) titleView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
        }
    }
}