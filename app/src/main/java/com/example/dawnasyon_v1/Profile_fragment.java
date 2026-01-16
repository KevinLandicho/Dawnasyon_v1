package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Profile_fragment extends BaseFragment {

    private LinearLayout familyContainer;

    // Prioritize Card Views
    private CardView cardPriority;
    private TextView tvPriorityLevel;
    private TextView tvPriorityScore;
    private TextView tvPriorityReason;
    private LinearLayout llBreakdownContainer;
    private ImageView ivExpandArrow;

    private boolean isExpanded = false;

    // Networking
    private final OkHttpClient client = new OkHttpClient();

    // Risk Zones (These match your LiveMap)
    private static final double RISK_CREEK_LAT = 14.7025;
    private static final double RISK_CREEK_LON = 121.0535;
    private static final double RISK_FIRE_LAT = 14.7040;
    private static final double RISK_FIRE_LON = 121.0550;

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

        // --- 1. Bind Common Views ---
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
        TextView badgeStatus = view.findViewById(R.id.badge_status);

        familyContainer = view.findViewById(R.id.ll_family_container);

        // Bind Priority Views
        cardPriority = view.findViewById(R.id.card_priority);
        tvPriorityLevel = view.findViewById(R.id.tv_priority_level);
        tvPriorityScore = view.findViewById(R.id.tv_priority_score);
        tvPriorityReason = view.findViewById(R.id.tv_priority_reason);
        llBreakdownContainer = view.findViewById(R.id.ll_breakdown_container);
        ivExpandArrow = view.findViewById(R.id.iv_expand_arrow);

        // Setup Expand Click
        if (cardPriority != null) {
            cardPriority.setOnClickListener(v -> toggleBreakdown());
        }

        // --- 2. Setup Menu Listeners ---
        setupMenuItem(menuHistory, R.drawable.ic_history, "Donation history");
        setupMenuItem(menuSuggestion, R.drawable.ic_suggestion, "Suggestion form");
        setupMenuItem(menuPassword, R.drawable.ic_lock, "Change password");
        setupMenuItem(menuDelete, R.drawable.ic_delete, "Delete account");
        setupMenuItem(menuLogout, R.drawable.ic_logout, "Log out");

        btnEditProfile.setOnClickListener(v -> navigateToFragment(new EditProfile_fragment()));
        btnViewQR.setOnClickListener(v -> navigateToFragment(DisplayQR_fragment.newInstance(R.drawable.ic_qrsample)));

        if (btnPinLocation != null) {
            btnPinLocation.setOnClickListener(v -> {
                String addressToPin = (detailAddress != null) ? detailAddress.getText().toString() : "Manila";
                navigateToFragment(LiveMap_fragment.newInstance(addressToPin));
                Toast.makeText(getContext(), "Locating: " + addressToPin, Toast.LENGTH_SHORT).show();
            });
        }

        menuHistory.setOnClickListener(v -> navigateToFragment(new DonationHistory_fragment()));
        menuSuggestion.setOnClickListener(v -> navigateToFragment(new SuggestionForm_fragment()));
        menuPassword.setOnClickListener(v -> navigateToFragment(new ChangePassword_fragment()));
        menuDelete.setOnClickListener(v -> navigateToFragment(new DeleteAccount_fragment()));
        menuLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        // --- 3. Load Data ---
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                if (detailName != null) detailName.setText(profile.getFull_name());
                if (userNameHeader != null) userNameHeader.setText(profile.getFull_name());
                if (detailContact != null) detailContact.setText(profile.getContact_number());

                String fullAddress = "";
                if (profile.getHouse_number() != null) fullAddress += profile.getHouse_number() + " ";
                if (profile.getStreet() != null) fullAddress += profile.getStreet() + ", ";
                if (profile.getBarangay() != null) fullAddress += profile.getBarangay() + ", ";
                if (profile.getCity() != null) fullAddress += profile.getCity();
                if (detailAddress != null) detailAddress.setText(fullAddress);

                boolean isVerified = Boolean.TRUE.equals(profile.getVerified());
                String userType = profile.getType();

                if (badgeStatus != null) {
                    badgeStatus.setVisibility(View.VISIBLE);
                    if (userType != null && (userType.equalsIgnoreCase("Foreign") || userType.equalsIgnoreCase("Overseas"))) {
                        badgeStatus.setText("🌍 OVERSEAS DONOR");
                        badgeStatus.setTextColor(Color.parseColor("#0D47A1"));
                        badgeStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                        if (cardPriority != null) cardPriority.setVisibility(View.GONE);
                    } else {
                        if (cardPriority != null) cardPriority.setVisibility(View.VISIBLE);
                        if (isVerified) {
                            badgeStatus.setText("✅ VERIFIED RESIDENT");
                            badgeStatus.setTextColor(Color.parseColor("#2E7D32"));
                            badgeStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        } else {
                            badgeStatus.setText("⚠️ NOT VERIFIED");
                            badgeStatus.setTextColor(Color.parseColor("#C62828"));
                            badgeStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            disableFeature(btnViewQR, "QR Code");
                            disableFeature(btnPinLocation, "Map Pinning");
                            disableFeature(menuSuggestion, "Suggestion Form");
                            disableFeature(menuHistory, "Donation History");
                        }
                    }
                }

                loadFamilyMembers(isVerified, profile, fullAddress);
            }
            return null;
        });
    }

    private void toggleBreakdown() {
        if (llBreakdownContainer == null) return;

        if (isExpanded) {
            llBreakdownContainer.setVisibility(View.GONE);
            ivExpandArrow.animate().rotation(0).setDuration(200).start();
        } else {
            llBreakdownContainer.setVisibility(View.VISIBLE);
            ivExpandArrow.animate().rotation(180).setDuration(200).start();
        }
        isExpanded = !isExpanded;
    }

    private void loadFamilyMembers(boolean isVerified, Profile profile, String fullAddress) {
        AuthHelper.fetchHouseholdMembers(members -> {
            if (isAdded()) {
                if (familyContainer != null) {
                    familyContainer.removeAllViews();
                    if (members == null || members.isEmpty()) {
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("No registered members found.");
                        familyContainer.addView(emptyView);
                    } else {
                        for (int i = 0; i < members.size(); i++) {
                            addMemberRow(members.get(i));
                            if (i < members.size() - 1) addDivider();
                        }
                    }
                }

                if (cardPriority != null && cardPriority.getVisibility() == View.VISIBLE) {
                    calculatePriorityWithGeoRisk(members, isVerified, profile, fullAddress);
                }
            }
            return null;
        });
    }

    // ⭐ UPDATED LOGIC: 5000m (5km) RADIUS TO COVER YOUR 3.7km DISTANCE
    private void calculatePriorityWithGeoRisk(List<HouseholdMember> members, boolean isVerified, Profile profile, String addressStr) {
        if (tvPriorityScore == null) return;

        final int[] score = {10};
        final StringBuilder breakdown = new StringBuilder();
        breakdown.append("• Base Score: +10 pts\n");

        // 1. Family Size
        int familySize = (members != null) ? members.size() : 0;
        int famPoints = Math.min(familySize * 5, 50);
        score[0] += famPoints;
        if(familySize > 0) breakdown.append("• Family Size (").append(familySize).append("): +").append(famPoints).append(" pts\n");

        if(familySize > 4) {
            score[0] += 20;
            breakdown.append("• Large Household Bonus: +20 pts\n");
        }

        // 2. Verification
        if(isVerified) {
            score[0] += 10;
            breakdown.append("• Verified Status: +10 pts\n");
        }

        // 3. Keyword Check
        if (addressStr.toLowerCase().contains("creek") || addressStr.toLowerCase().contains("river") || addressStr.toLowerCase().contains("flood")) {
            score[0] += 10;
            breakdown.append("• Address Risk Keyword: +10 pts\n");
        }

        // 4. Async Geo-Risk Check
        new Thread(() -> {
            try {
                Log.d("GEO_RISK", "Searching for: " + addressStr);

                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    double userLat = addresses.get(0).getLatitude();
                    double userLon = addresses.get(0).getLongitude();

                    Log.d("GEO_RISK", "Found Coords: " + userLat + ", " + userLon);

                    float distToCreek = getDistance(userLat, userLon, RISK_CREEK_LAT, RISK_CREEK_LON);
                    float distToFire = getDistance(userLat, userLon, RISK_FIRE_LAT, RISK_FIRE_LON);

                    Log.d("GEO_RISK", "Distance to Creek: " + distToCreek + "m");
                    Log.d("GEO_RISK", "Distance to Fire: " + distToFire + "m");

                    // ⭐ INCREASED RADIUS TO 5000 METERS (5 KM)
                    if (distToCreek < 5000) {
                        score[0] += 30;
                        breakdown.append("• Near Flood Zone (" + (int)distToCreek + "m): +30 pts\n");
                    }
                    if (distToFire < 5000) {
                        score[0] += 40;
                        breakdown.append("• Near Fire Alert (" + (int)distToFire + "m): +40 pts\n");
                    }

                    if (checkRecentEarthquake(userLat, userLon)) {
                        score[0] += 20;
                        breakdown.append("• Recent Earthquake Nearby: +20 pts\n");
                    }
                } else {
                    Log.e("GEO_RISK", "Geocoding failed for: " + addressStr);
                    breakdown.append("• Location check failed (Address not found)\n");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("GEO_RISK", "Error: " + e.getMessage());
            }

            // Update UI on Main Thread
            new Handler(Looper.getMainLooper()).post(() -> updatePriorityUI(score[0], breakdown.toString()));
        }).start();
    }

    private void updatePriorityUI(int rawScore, String breakdownText) {
        int score = Math.min(rawScore, 100);

        tvPriorityScore.setText("Score: " + score + "/100");
        tvPriorityReason.setText(breakdownText.trim());

        if (score >= 80) {
            tvPriorityLevel.setText("CRITICAL PRIORITY");
            tvPriorityLevel.setTextColor(Color.RED);
            cardPriority.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
        } else if (score >= 50) {
            tvPriorityLevel.setText("HIGH PRIORITY");
            tvPriorityLevel.setTextColor(Color.parseColor("#E65100"));
            cardPriority.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        } else {
            tvPriorityLevel.setText("NORMAL PRIORITY");
            tvPriorityLevel.setTextColor(Color.parseColor("#2E7D32"));
            cardPriority.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
        }
    }

    private float getDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    private boolean checkRecentEarthquake(double userLat, double userLon) {
        String url = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson";
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject root = new JSONObject(response.body().string());
                JSONArray features = root.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");
                    double qLon = coords.getDouble(0);
                    double qLat = coords.getDouble(1);
                    if (getDistance(userLat, userLon, qLat, qLon) < 50000) return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private void disableFeature(View view, String featureName) {
        if (view == null) return;
        view.setAlpha(0.4f);
        view.setOnClickListener(v -> Toast.makeText(getContext(), "🔒 " + featureName + " is locked.", Toast.LENGTH_SHORT).show());
    }

    private void addMemberRow(HouseholdMember member) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView indicator = new ImageView(getContext());
        indicator.setImageResource(R.drawable.ic_circle_indicator);
        indicator.setColorFilter(Color.parseColor("#F5901A"));
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(24, 24);
        imgParams.setMargins(0, 0, 24, 0);
        indicator.setLayoutParams(imgParams);
        row.addView(indicator);

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(textParams);

        TextView tvName = new TextView(getContext());
        tvName.setText(member.getFull_name());
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(Color.BLACK);
        textCol.addView(tvName);

        TextView tvRelation = new TextView(getContext());
        tvRelation.setText(member.getRelation());
        tvRelation.setTextSize(11f);
        textCol.addView(tvRelation);
        row.addView(textCol);

        TextView badge = new TextView(getContext());
        badge.setText(member.getCensusStatus() ? "✅ Registered" : "❌ Not Registered");
        badge.setTextSize(10f);
        badge.setPadding(12, 6, 12, 6);
        badge.setBackgroundColor(Color.parseColor(member.getCensusStatus() ? "#E8F5E9" : "#F5F5F5"));
        badge.setTextColor(Color.parseColor(member.getCensusStatus() ? "#2E7D32" : "#757575"));
        row.addView(badge);

        familyContainer.addView(row);
    }

    private void addDivider() {
        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        line.setBackgroundColor(Color.parseColor("#F0F0F0"));
        familyContainer.addView(line);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(getContext()).setTitle("Log Out").setMessage("Are you sure?").setPositiveButton("Yes", (d, w) -> performLogout()).setNegativeButton("Cancel", null).show();
    }

    private void performLogout() {
        AuthHelper.logoutUser();
        if(getActivity() != null) {
            getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
    }

    private void setupMenuItem(View view, int icon, String title) {
        if (view == null) return;
        ((ImageView) view.findViewById(R.id.menu_icon)).setImageResource(icon);
        ((TextView) view.findViewById(R.id.menu_title)).setText(title);
    }

    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }
}