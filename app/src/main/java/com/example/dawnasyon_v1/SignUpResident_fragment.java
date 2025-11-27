package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SignUpResident_fragment extends Fragment {

    private Button btnResident;
    private Button btnNonResident;
    private String selectedResidency = null; // Stores "Resident" or "Non-Resident"

    public SignUpResident_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_resident, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnResident = view.findViewById(R.id.btn_resident);
        btnNonResident = view.findViewById(R.id.btn_non_resident);
        Button btnNext = view.findViewById(R.id.btn_next);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // 1. Click Logic
        btnResident.setOnClickListener(v -> handleSelection(btnResident, btnNonResident, "Resident"));
        btnNonResident.setOnClickListener(v -> handleSelection(btnNonResident, btnResident, "Non-Resident"));

        // 2. Navigation Logic
        btnNext.setOnClickListener(v -> navigateNext());
        btnPrevious.setOnClickListener(v -> navigatePrevious());

        // 3. Default State (Resident)
        if (savedInstanceState == null) {
            this.selectedResidency = "Resident";
            // Force visual update immediately
            btnResident.post(() -> {
                handleSelection(btnResident, btnNonResident, "Resident");
            });
        }
    }

    private void handleSelection(Button selectedButton, Button otherButton, String selection) {
        // --- 1. RESET the "Other" button (Gray Background) ---
        otherButton.setBackgroundResource(R.drawable.bg_option_unselected);

        // --- 2. HIGHLIGHT the "Selected" button (Orange Background) ---
        selectedButton.setBackgroundResource(R.drawable.bg_option_selected);

        // 3. Store selection
        this.selectedResidency = selection;
    }

    private void navigateNext() {
        if (selectedResidency == null) {
            Toast.makeText(getContext(), "Please select an option to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Navigate to the actual Sign Up Details Form (Step 3)
        // Fragment nextStepFragment = SignUpDetails_fragment.newInstance(selectedResidency);
        //
        // requireActivity().getSupportFragmentManager().beginTransaction()
        //         .replace(R.id.fragment_container_signup, nextStepFragment)
        //         .addToBackStack(null)
        //         .commit();

        Toast.makeText(getContext(), "Continuing as: " + selectedResidency, Toast.LENGTH_SHORT).show();
    }

    private void navigatePrevious() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}