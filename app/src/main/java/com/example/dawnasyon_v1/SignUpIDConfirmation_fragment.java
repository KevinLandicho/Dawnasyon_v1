package com.example.dawnasyon_v1;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class SignUpIDConfirmation_fragment extends BaseFragment {

    // Static variable to hold the image temporarily
    public static Bitmap capturedImage;

    private ImageView imgPreviewArea;
    private TextView tvInstructions;
    private Button btnScan;
    private Button btnRetake, btnSubmit; // Promoted to field level for translation access
    private LinearLayout layoutConfirmButtons;

    // Camera Launcher
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Object data = extras.get("data");
                    if (data instanceof Bitmap) {
                        showCapturedImage((Bitmap) data);
                    }
                }
            }
    );

    // Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    public SignUpIDConfirmation_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_id_confirmation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPreviewArea = view.findViewById(R.id.img_preview_area);
        tvInstructions = view.findViewById(R.id.tv_instructions);
        btnScan = view.findViewById(R.id.btn_scan_action);
        layoutConfirmButtons = view.findViewById(R.id.layout_confirm_buttons);

        btnRetake = view.findViewById(R.id.btn_retake);
        btnSubmit = view.findViewById(R.id.btn_submit_final);

        // Check if we already have an image (e.g. passed from previous screen or retained)
        if (capturedImage != null) {
            showCapturedImage(capturedImage);
        }

        // 1. Scan Button -> Checks Permission -> Opens Camera
        btnScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // 2. Retake Button -> Reset UI to Instruction Mode
        btnRetake.setOnClickListener(v -> resetToInstructionMode());

        // 3. Submit Button -> Navigate to Pending Verification Screen
        btnSubmit.setOnClickListener(v -> {
            // TODO: Upload 'capturedImage' to server here

            // ⭐ FIX: Navigate to the Verification Pending (Clock) Screen ⭐
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpVerificationPending_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCapturedImage(Bitmap bitmap) {
        capturedImage = bitmap; // Save it

        // Update UI to "Confirmation" State
        imgPreviewArea.setImageBitmap(bitmap);
        imgPreviewArea.setScaleType(ImageView.ScaleType.FIT_CENTER); // Show full photo

        tvInstructions.setVisibility(View.GONE); // Hide instructions
        btnScan.setVisibility(View.GONE); // Hide scan button
        layoutConfirmButtons.setVisibility(View.VISIBLE); // Show Retake/Submit

        // ⭐ RE-APPLY TRANSLATION TO BUTTONS THAT JUST APPEARED
        if(isAdded()) {
            applyTagalogTranslation(layoutConfirmButtons);
        }
    }

    private void resetToInstructionMode() {
        // Update UI back to "Instruction" State
        imgPreviewArea.setImageResource(R.drawable.ic_id_scan_illustration); // Reset to icon
        imgPreviewArea.setScaleType(ImageView.ScaleType.FIT_CENTER);

        tvInstructions.setVisibility(View.VISIBLE);
        btnScan.setVisibility(View.VISIBLE);
        layoutConfirmButtons.setVisibility(View.GONE);

        // ⭐ RE-APPLY TRANSLATION TO INSTRUCTIONS/SCAN BUTTON
        if(isAdded()) {
            applyTagalogTranslation(tvInstructions);
            applyTagalogTranslation(btnScan);
        }
    }
}