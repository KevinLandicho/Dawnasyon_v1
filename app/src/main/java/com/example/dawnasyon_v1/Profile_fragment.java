package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// Import your HouseMember class
import com.example.dawnasyon_v1.HouseMember;

public class Profile_fragment extends BaseFragment {

    private LinearLayout familyContainer;
    private ImageView ivProfilePic;

    private CardView cardPriority;
    private TextView tvPriorityLevel;
    private TextView tvPriorityScore;
    private TextView tvPriorityReason;
    private LinearLayout llBreakdownContainer;
    private ImageView ivExpandArrow;

    private boolean isExpanded = false;
    private String currentUserId = "";

    private final OkHttpClient client = new OkHttpClient();

    private static final double RISK_CREEK_LAT = 14.7025;
    private static final double RISK_CREEK_LON = 121.0535;
    private static final double RISK_FIRE_LAT = 14.7040;
    private static final double RISK_FIRE_LON = 121.0550;

    public Profile_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        ivProfilePic = view.findViewById(R.id.profile_pic);
        familyContainer = view.findViewById(R.id.ll_family_container);

        cardPriority = view.findViewById(R.id.card_priority);
        tvPriorityLevel = view.findViewById(R.id.tv_priority_level);
        tvPriorityScore = view.findViewById(R.id.tv_priority_score);
        tvPriorityReason = view.findViewById(R.id.tv_priority_reason);
        llBreakdownContainer = view.findViewById(R.id.ll_breakdown_container);
        ivExpandArrow = view.findViewById(R.id.iv_expand_arrow);

        if (cardPriority != null) cardPriority.setOnClickListener(v -> toggleBreakdown());

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

        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                currentUserId = profile.getId();

                if (detailName != null) detailName.setText(profile.getFull_name());
                if (userNameHeader != null) userNameHeader.setText(profile.getFull_name());
                if (detailContact != null) detailContact.setText(profile.getContact_number());

                String fullAddress = "";
                if (profile.getHouse_number() != null) fullAddress += profile.getHouse_number() + " ";
                if (profile.getStreet() != null) fullAddress += profile.getStreet() + ", ";
                if (profile.getBarangay() != null) fullAddress += profile.getBarangay() + ", ";
                if (profile.getCity() != null) fullAddress += profile.getCity();
                if (detailAddress != null) detailAddress.setText(fullAddress);

                String avatarName = profile.getAvatarName();
                int avatarResId = AvatarHelper.getDrawableId(getContext(), avatarName);
                if (ivProfilePic != null) ivProfilePic.setImageResource(avatarResId);

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
            } else {
                if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
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
        AuthHelper.fetchHouseholdMembers(kotlinMembers -> {
            if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

            if (isAdded()) {
                if (familyContainer != null) {
                    familyContainer.removeAllViews();

                    if (kotlinMembers == null || kotlinMembers.isEmpty()) {
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("No registered members found.");
                        familyContainer.addView(emptyView);
                    } else {
                        List<HouseMember> javaMembers = new ArrayList<>();

                        // Convert Kotlin objects to Java objects
                        for (Object kMember : kotlinMembers) {
                            if (kMember instanceof HouseholdMember) {
                                HouseholdMember km = (HouseholdMember) kMember;
                                HouseMember jm = new HouseMember();

                                jm.setMember_id(km.getMember_id());
                                jm.setHead_id(km.getHead_id());
                                jm.setFull_name(km.getFull_name());
                                jm.setRelation(km.getRelation());
                                jm.setAge(km.getAge());
                                jm.setGender(km.getGender());

                                Boolean isProxy = km.getIs_authorized_proxy();
                                jm.setIs_authorized_proxy(isProxy != null ? isProxy : false);

                                javaMembers.add(jm);
                            }
                        }

                        for (int i = 0; i < javaMembers.size(); i++) {
                            addMemberRow(javaMembers.get(i));
                            if (i < javaMembers.size() - 1) addDivider();
                        }

                        if (cardPriority != null && cardPriority.getVisibility() == View.VISIBLE) {
                            calculatePriorityWithGeoRisk(javaMembers, isVerified, profile, fullAddress);
                        }
                    }
                }
            }
            return null;
        });
    }

    private void calculatePriorityWithGeoRisk(List<HouseMember> members, boolean isVerified, Profile profile, String addressStr) {
        if (tvPriorityScore == null) return;
        final int[] score = {10};
        final StringBuilder breakdown = new StringBuilder();
        breakdown.append("• Base Score: +10 pts\n");

        int familySize = (members != null) ? members.size() : 0;
        int famPoints = Math.min(familySize * 5, 50);
        score[0] += famPoints;
        if(familySize > 0) breakdown.append("• Family Size (").append(familySize).append("): +").append(famPoints).append(" pts\n");
        if(familySize > 4) { score[0] += 20; breakdown.append("• Large Household Bonus: +20 pts\n"); }
        if(isVerified) { score[0] += 10; breakdown.append("• Verified Status: +10 pts\n"); }
        if (addressStr.toLowerCase().contains("creek") || addressStr.toLowerCase().contains("river") || addressStr.toLowerCase().contains("flood")) {
            score[0] += 10; breakdown.append("• Address Risk Keyword: +10 pts\n");
        }

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    double userLat = addresses.get(0).getLatitude();
                    double userLon = addresses.get(0).getLongitude();
                    float distToCreek = getDistance(userLat, userLon, RISK_CREEK_LAT, RISK_CREEK_LON);
                    float distToFire = getDistance(userLat, userLon, RISK_FIRE_LAT, RISK_FIRE_LON);
                    if (distToCreek < 5000) { score[0] += 30; breakdown.append("• Near Flood Zone (" + (int)distToCreek + "m): +30 pts\n"); }
                    if (distToFire < 5000) { score[0] += 40; breakdown.append("• Near Fire Alert (" + (int)distToFire + "m): +40 pts\n"); }
                    if (checkRecentEarthquake(userLat, userLon)) { score[0] += 20; breakdown.append("• Recent Earthquake Nearby: +20 pts\n"); }
                } else { breakdown.append("• Location check failed (Address not found)\n"); }
            } catch (Exception e) { e.printStackTrace(); }
            new Handler(Looper.getMainLooper()).post(() -> updatePriorityUI(score[0], breakdown.toString()));
        }).start();
    }

    private void updatePriorityUI(int rawScore, String breakdownText) {
        int score = Math.min(rawScore, 100);
        tvPriorityScore.setText("Score: " + score + "/100");
        tvPriorityReason.setText(breakdownText.trim());
        if (score >= 80) { tvPriorityLevel.setText("CRITICAL PRIORITY"); tvPriorityLevel.setTextColor(Color.RED); cardPriority.setCardBackgroundColor(Color.parseColor("#FFEBEE")); }
        else if (score >= 50) { tvPriorityLevel.setText("HIGH PRIORITY"); tvPriorityLevel.setTextColor(Color.parseColor("#E65100")); cardPriority.setCardBackgroundColor(Color.parseColor("#FFF3E0")); }
        else { tvPriorityLevel.setText("NORMAL PRIORITY"); tvPriorityLevel.setTextColor(Color.parseColor("#2E7D32")); cardPriority.setCardBackgroundColor(Color.parseColor("#E8F5E9")); }
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

    private void addMemberRow(HouseMember member) {
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

        boolean isProxy = Boolean.TRUE.equals(member.getIs_authorized_proxy());

        if (isProxy) {
            TextView tvProxy = new TextView(getContext());
            tvProxy.setText("✅ PROXY");
            tvProxy.setTextSize(10f);
            tvProxy.setTextColor(Color.parseColor("#2E7D32"));
            tvProxy.setBackgroundColor(Color.parseColor("#E8F5E9"));
            tvProxy.setPadding(12, 4, 12, 4);
            row.addView(tvProxy);
        } else {
            // ⭐ CHANGED: Only add "Assign Proxy" button if relation is NOT "Head"
            if (!"Head".equalsIgnoreCase(member.getRelation())) {
                TextView btnAssign = new TextView(getContext());
                btnAssign.setText("Assign Proxy");
                btnAssign.setTextSize(10f);
                btnAssign.setTextColor(Color.WHITE);
                btnAssign.setBackgroundColor(Color.parseColor("#FF9800"));
                btnAssign.setPadding(16, 8, 16, 8);

                btnAssign.setOnClickListener(v -> {
                    if (currentUserId.isEmpty()) return;

                    new AlertDialog.Builder(getContext())
                            .setTitle("Assign Proxy")
                            .setMessage("Set " + member.getFull_name() + " as the authorized representative? This will remove any existing proxy.")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

                                SupabaseJavaHelper.assignHouseholdProxy(currentUserId, member.getMember_id(), new SupabaseJavaHelper.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        AuthHelper.fetchUserProfile(profile -> {
                                            if(profile != null) loadFamilyMembers(true, profile, "");
                                            return null;
                                        });
                                    }
                                    @Override
                                    public void onError(String message) {
                                        if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
                row.addView(btnAssign);
            }
        }

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