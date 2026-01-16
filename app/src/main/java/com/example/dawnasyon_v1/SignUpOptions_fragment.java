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

public class SignUpOptions_fragment extends BaseFragment {

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
                "Local")); // Local = Resident

        btnOverseas.setOnClickListener(v -> handleSelection(
                btnOverseas, R.drawable.ic_glob,
                btnLocal, R.drawable.ic_ppl,
                "Overseas")); // Overseas = Non-Resident

        // 2. Navigation Logic
        btnNext.setOnClickListener(v -> navigateNext());
        btnPrevious.setOnClickListener(v -> navigatePrevious());

        // 3. Default State (Local Community)
        if (savedInstanceState == null) {
            handleSelection(btnLocal, R.drawable.ic_ppl,
                    btnOverseas, R.drawable.ic_glob,
                    "Local");
        }
    }

    private void handleSelection(Button selectedButton, int selectedIconRes,
                                 Button otherButton, int otherIconRes,
                                 String userType) {

        otherButton.setBackgroundResource(R.drawable.bg_option_unselected);
        Drawable otherIcon = getIconWithCircle(otherIconRes, "#DDDDDD");
        otherButton.setCompoundDrawablesWithIntrinsicBounds(otherIcon, null, null, null);

        selectedButton.setBackgroundResource(R.drawable.bg_option_selected);
        Drawable selectedIcon = getIconWithCircle(selectedIconRes, "#F5901A");
        selectedButton.setCompoundDrawablesWithIntrinsicBounds(selectedIcon, null, null, null);

        this.selectedUserType = userType;
    }

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

    private void navigateNext() {
        if (selectedUserType == null) {
            Toast.makeText(getContext(), "Please choose a user type to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ⭐ STORE SELECTION IN CACHE
        RegistrationCache.userType = selectedUserType;

        Fragment nextFragment;

        if (selectedUserType.equals("Local")) {
            // Local -> Goes to Resident/Valid ID Check
            nextFragment = new SignUpResident_fragment();
        } else {
            // ⭐ OVERSEAS -> Skip ID/Resident check, Go straight to Personal Info
            nextFragment = new SignUpStep1Personal_fragment();
        }

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, nextFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigatePrevious() {
        requireActivity().finish();
    }
}