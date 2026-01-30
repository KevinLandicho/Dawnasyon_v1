package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Map;

public class DonationOptions_fragments extends BaseFragment {

    private LinearLayout categoryContainer;
    private String currentUserType = "Resident"; // Default to allow access

    // Thresholds for status
    private static final int THRESHOLD_CRITICAL = 50;
    private static final int THRESHOLD_HIGH = 100;

    public DonationOptions_fragments() {
        // Required empty public constructor
    }

    public static DonationOptions_fragments newInstance(String p1, String p2) {
        DonationOptions_fragments fragment = new DonationOptions_fragments();
        Bundle args = new Bundle();
        args.putString("param1", p1);
        args.putString("param2", p2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_donation_options, container, false);

        categoryContainer = view.findViewById(R.id.categoryContainer);

        // 1. GET USER TYPE
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            currentUserType = prefs.getString("user_type", "Resident");
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // System Back Button Handler
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AddDonation_Fragment())
                        .commit();
            }
        });

        // ⭐ FETCH REAL STOCK DATA (With Timeout Protection)
        fetchStockAndLoadCategories();
    }

    private void fetchStockAndLoadCategories() {
        // Use getContext() to avoid crashes if screen closes
        Context context = getContext();
        if (context == null) return;

        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        // This function now has a 5-second timeout inside SupabaseJavaHelper
        SupabaseJavaHelper.fetchDashboardData(context, "all", new SupabaseJavaHelper.DashboardCallback() {
            @Override
            public void onDataLoaded(Map<String, Integer> inventory, Map<String, Integer> areas, Map<String, Float> donations, Map<String, Integer> families, DashboardMetrics metrics, Map<String, Integer> impact) {
                // Check if fragment is alive
                if (!isAdded() || getActivity() == null) return;

                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                // Success: Load with Real Data
                loadCategories(inventory);
            }

            @Override
            public void onError(String message) {
                // Check if fragment is alive
                if (!isAdded() || getActivity() == null) return;

                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                // Timeout or Error: Load Default Menu (Safe Mode)
                // This prevents the user from being stuck on a blank screen
                Toast.makeText(getContext(), "Slow connection: Showing default list", Toast.LENGTH_SHORT).show();
                loadCategories(null);
            }
        });
    }

    // -----------------------------------------
    // LOAD CATEGORIES (Handles Real OR Default Data)
    // -----------------------------------------
    private void loadCategories(Map<String, Integer> inventory) {
        if (categoryContainer == null || getContext() == null) return;
        categoryContainer.removeAllViews(); // Clear existing

        LayoutInflater inflater = LayoutInflater.from(getContext());

        String foodStatus, hygieneStatus, medStatus, packStatus;

        if (inventory != null) {
            // --- MODE A: REAL DATA ---
            foodStatus = calculateStatus(getStockCount(inventory, "Rice", "Canned", "Noodles", "Food"));
            hygieneStatus = calculateStatus(getStockCount(inventory, "Soap", "Shampoo", "Toothpaste", "Hygiene"));
            medStatus = calculateStatus(getStockCount(inventory, "Medicine", "Vitamin", "Biogesic"));
            packStatus = calculateStatus(getStockCount(inventory, "Relief Pack", "Pack"));
        } else {
            // --- MODE B: SAFE DEFAULTS (Offline/Slow Wifi) ---
            foodStatus = "Critical";
            hygieneStatus = "High Priority";
            medStatus = "Critical";
            packStatus = "High Priority";
        }

        // --- ADD CATEGORIES ---
        addCategory(inflater, "CASH", "Funds for supplies and relief support.", "Always Needed", R.drawable.ic_money);
        addCategory(inflater, "FOOD", "Rice, noodles, and canned goods.", foodStatus, R.drawable.ic_food);
        addCategory(inflater, "HYGIENE KITS", "Soap, toothpaste, and essentials.", hygieneStatus, R.drawable.ic_hygiene);
        addCategory(inflater, "MEDICINE", "Vitamins and first aid.", medStatus, R.drawable.ic_health);
        addCategory(inflater, "RELIEF PACKS", "Packed goods for distribution.", packStatus, R.drawable.ic_packs);
    }

    // Helper: Logic for status text
    private String calculateStatus(int count) {
        if (count < THRESHOLD_CRITICAL) {
            return "Critical (" + count + " left)";
        } else if (count < THRESHOLD_HIGH) {
            return "Low Stock (" + count + ")";
        } else {
            return "Normal (" + count + ")";
        }
    }

    // Helper: Sum stock items by keywords
    private int getStockCount(Map<String, Integer> inventory, String... keywords) {
        if (inventory == null) return 0;
        int total = 0;
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            String key = entry.getKey().toLowerCase();
            for (String keyword : keywords) {
                if (key.contains(keyword.toLowerCase())) {
                    total += entry.getValue();
                    break;
                }
            }
        }
        return total;
    }

    // ------------------------------------------------------
    // INFLATE CARD VIEW AND SET CLICK LISTENER
    // ------------------------------------------------------
    private void addCategory(LayoutInflater inflater, String title, String description,
                             String status, int imageRes) {

        View item = inflater.inflate(R.layout.category_item, categoryContainer, false);

        ImageView imgIcon = item.findViewById(R.id.imgIcon);
        TextView txtTitle = item.findViewById(R.id.txtTitle);
        TextView txtStatus = item.findViewById(R.id.txtStatus);
        TextView txtDescription = item.findViewById(R.id.txtDescription);

        imgIcon.setImageResource(imageRes);
        txtTitle.setText(title);
        txtDescription.setText(description);
        txtStatus.setText(status);

        // ⭐ DYNAMIC BADGE COLOR
        if (status.toLowerCase().contains("critical") || status.toLowerCase().contains("always")) {
            txtStatus.setBackgroundResource(R.drawable.status_red);
        } else if (status.toLowerCase().contains("low") || status.toLowerCase().contains("high")) {
            txtStatus.setBackgroundResource(R.drawable.status_orange);
        } else {
            txtStatus.setBackgroundResource(R.drawable.status_green);
        }

        // 2. CHECK RESTRICTION (Overseas User Check)
        boolean isRestricted = currentUserType != null
                && (currentUserType.equalsIgnoreCase("Foreign") || currentUserType.equalsIgnoreCase("Overseas"))
                && !title.equalsIgnoreCase("CASH");

        if (isRestricted) {
            // --- DISABLED STATE ---
            item.setAlpha(0.5f);
            item.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Overseas donors can only donate Cash.", Toast.LENGTH_SHORT).show()
            );
        } else {
            // --- ENABLED STATE ---
            item.setAlpha(1.0f);
            item.setOnClickListener(v -> {
                Fragment nextFragment;
                if (title.equalsIgnoreCase("CASH")) {
                    nextFragment = CashInfo_fragment.newInstance(title, description, status, imageRes);
                } else {
                    nextFragment = Donation_details_fragment.newInstance(title, description, status, imageRes);
                }

                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, nextFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        categoryContainer.addView(item);
    }
}