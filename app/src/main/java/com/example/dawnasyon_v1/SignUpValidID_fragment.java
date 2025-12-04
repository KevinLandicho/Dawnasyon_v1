package com.example.dawnasyon_v1;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpValidID_fragment extends Fragment {

    private Button btnQcId, btnBrgyId;
    private String selectedIdType = null;

    public SignUpValidID_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_valid_id, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnQcId = view.findViewById(R.id.btn_qc_id);
        btnBrgyId = view.findViewById(R.id.btn_brgy_id);
        Button btnStartScan = view.findViewById(R.id.btn_start_scan);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // --- ID Type Selection Logic ---
        btnQcId.setOnClickListener(v -> handleIdSelection("Quezon City ID"));
        btnBrgyId.setOnClickListener(v -> handleIdSelection("Barangay ID"));

        // --- Start Scan Logic ---
        // In SignUpValidID_fragment.java

        btnStartScan.setOnClickListener(v -> {
            if (selectedIdType == null) {
                Toast.makeText(getContext(), "Please select an ID type first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // NAVIGATE TO INSTRUCTION/CONFIRMATION SCREEN
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpIDConfirmation_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Go Back
        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
// Inside SignUpValidID_fragment.java

//    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            result -> {
//                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                    Bundle extras = result.getData().getExtras();
//                    Bitmap imageBitmap = (Bitmap) extras.get("data");
//
//                    if (imageBitmap != null) {
//                        // 1. Save image to the next fragment's static variable
//                        SignUpIDConfirmation_fragment.capturedImage = imageBitmap;
//
//                        // 2. Navigate to the Confirmation Screen
//                        getParentFragmentManager().beginTransaction()
//                                .replace(R.id.fragment_container_signup, new SignUpIDConfirmation_fragment())
//                                .addToBackStack(null)
//                                .commit();
//                    }
//                }
//            }
//    );
    private void handleIdSelection(String idType) {
        selectedIdType = idType;

        // Visual Feedback: Reset both, then highlight selected
        // Using simple colors here (#E0E0E0 is gray, #FFDAB9 is light orange)
        btnQcId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        btnBrgyId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));

        if (idType.equals("Quezon City ID")) {
            btnQcId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFDAB9"))); // Selected Color
        } else {
            btnBrgyId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFDAB9"))); // Selected Color
        }
    }
}