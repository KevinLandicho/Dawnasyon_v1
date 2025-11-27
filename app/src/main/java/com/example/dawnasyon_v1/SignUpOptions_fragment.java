package com.example.dawnasyon_v1;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SignUpOptions_fragment extends Fragment {

    private Button btnLocal;
    private Button btnOverseas;
    private String selectedUserType = null;

    public SignUpOptions_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnLocal = view.findViewById(R.id.btn_local_community);
        btnOverseas = view.findViewById(R.id.btn_overseas_donor);
        Button btnNext = view.findViewById(R.id.btn_next);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // 1. Click Logic
        btnLocal.setOnClickListener(v -> handleSelection(
                btnLocal, R.drawable.ic_ppl,
                btnOverseas, R.drawable.ic_glob,
                "Local"));

        btnOverseas.setOnClickListener(v -> handleSelection(
                btnOverseas, R.drawable.ic_glob,
                btnLocal, R.drawable.ic_ppl,
                "Overseas"));

        // 2. Navigation Logic
        btnNext.setOnClickListener(v -> navigateNext());
        btnPrevious.setOnClickListener(v -> navigatePrevious());

        // 3. Default State (Local Community)
        if (savedInstanceState == null) {
            this.selectedUserType = "Local";
            btnLocal.post(() -> {
                handleSelection(btnLocal, R.drawable.ic_ppl,
                        btnOverseas, R.drawable.ic_glob,
                        "Local");
            });
        }
    }

    private void handleSelection(Button selectedButton, int selectedIconRes,
                                 Button otherButton, int otherIconRes,
                                 String userType) {

        // --- 1. RESET the "Other" button (Gray Circle) ---
        otherButton.setBackgroundResource(R.drawable.bg_option_unselected);

        // Generate Gray Circle Icon (#DDDDDD)
        Drawable otherIcon = getIconWithCircle(otherIconRes, "#DDDDDD");
        otherButton.setCompoundDrawablesWithIntrinsicBounds(otherIcon, null, null, null);

        // --- 2. HIGHLIGHT the "Selected" button (Orange Circle) ---
        selectedButton.setBackgroundResource(R.drawable.bg_option_selected);

        // Generate Orange Circle Icon (#F5901A)
        Drawable selectedIcon = getIconWithCircle(selectedIconRes, "#F5901A");
        selectedButton.setCompoundDrawablesWithIntrinsicBounds(selectedIcon, null, null, null);

        // 3. Store selection
        this.selectedUserType = userType;
    }

    /**
     * Helper Function: Creates a perfect circle background with a centered icon.
     */
    private Drawable getIconWithCircle(int iconResId, String colorHex) {
        int sizePx = dpToPx(48);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor(colorHex));
        circle.setSize(sizePx, sizePx);

        Drawable icon = ContextCompat.getDrawable(requireContext(), iconResId);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{circle, icon});
        layerDrawable.setLayerGravity(1, Gravity.CENTER);

        int padding = dpToPx(12);
        layerDrawable.setLayerInset(1, padding, padding, padding, padding);
        layerDrawable.setBounds(0, 0, sizePx, sizePx);

        return layerDrawable;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ⭐ REVISED NAVIGATION LOGIC ⭐
    private void navigateNext() {
        if (selectedUserType == null) {
            Toast.makeText(getContext(), "Please choose a user type to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        Fragment nextFragment = null;

        if (selectedUserType.equals("Local")) {
            // IF LOCAL -> Go to "Are you a Resident?" screen
            nextFragment = new SignUpResident_fragment();
        } else {
            // IF OVERSEAS -> Skip Resident check (Implement this later)
            Toast.makeText(getContext(), "Overseas flow coming soon", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nextFragment != null && getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, nextFragment) // Replace content in SignUpActivity
                    .addToBackStack(null) // Allow user to press Back to return here
                    .commit();
        }
    }

    private void navigatePrevious() {
        requireActivity().finish();
    }
}