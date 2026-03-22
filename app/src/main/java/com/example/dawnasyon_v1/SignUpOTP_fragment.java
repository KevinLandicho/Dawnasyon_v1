package com.example.dawnasyon_v1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUpOTP_fragment extends BaseFragment {

    private EditText[] otpInputs;
    private TextView tvTimer;
    private TextView tvSubtitle;
    private CountDownTimer countDownTimer;
    private boolean isResendEnabled = false;

    // ⭐ 5 Minutes Timer (300,000 milliseconds)
    private final long TIMER_DURATION = 300000;

    private ActivityResultLauncher<Intent> faceScanLauncher;

    public SignUpOTP_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Face Scan Launcher (The next step after OTP)
        faceScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(getContext(), "Face Registered! Finalizing...", Toast.LENGTH_SHORT).show();
                        createProfile();
                    } else {
                        Toast.makeText(getContext(), "Face scan skipped or failed.", Toast.LENGTH_SHORT).show();
                        // Proceed to profile creation anyway
                        createProfile();
                    }
                }
        );

        tvTimer = view.findViewById(R.id.tv_timer);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        String email = RegistrationCache.tempEmail;
        if (tvSubtitle != null) {
            String subText = "We've sent a code to " + (email != null ? email : "your email");
            tvSubtitle.setText(subText);
            // Translate dynamic subtitle
            TranslationHelper.autoTranslate(getContext(), tvSubtitle, subText);
        }

        otpInputs = new EditText[]{
                view.findViewById(R.id.otp_1), view.findViewById(R.id.otp_2),
                view.findViewById(R.id.otp_3), view.findViewById(R.id.otp_4),
                view.findViewById(R.id.otp_5), view.findViewById(R.id.otp_6)
        };
        setupOTPInputs();

        // Start the 5-minute timer immediately
        startTimer(TIMER_DURATION);

        // Resend Button Logic
        tvTimer.setOnClickListener(v -> {
            if (isResendEnabled) {
                performResendOtp();
            }
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }

    private void performResendOtp() {
        String email = RegistrationCache.tempEmail;
        if (email == null || email.isEmpty()) {
            Toast.makeText(getContext(), "Email missing. Please restart signup.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button immediately to prevent double-clicks
        String sendingText = "Sending...";
        tvTimer.setText(sendingText);
        TranslationHelper.autoTranslate(getContext(), tvTimer, sendingText);

        tvTimer.setEnabled(false);
        tvTimer.setTextColor(Color.GRAY);

        // Call Supabase (which now uses Resend)
        AuthHelper.resendOtp(email, new AuthHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Code resent! Check your inbox.", Toast.LENGTH_LONG).show();
                // Restart 5 minute timer
                startTimer(TIMER_DURATION);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Resend failed: " + message, Toast.LENGTH_SHORT).show();

                // Allow clicking again after 3 seconds if it failed (so they aren't stuck)
                tvTimer.postDelayed(() -> {
                    if (isAdded()) {
                        String resendText = "Resend Code";
                        tvTimer.setText(resendText);
                        TranslationHelper.autoTranslate(getContext(), tvTimer, resendText);

                        tvTimer.setEnabled(true);
                        tvTimer.setTextColor(Color.BLUE);
                    }
                }, 3000);
            }
        });
    }

    private void setupOTPInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    }
                    if (isAllFilled()) {
                        verifyRealOTP();
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpInputs[index].getText().length() == 0 && index > 0) {
                        otpInputs[index - 1].requestFocus();
                        otpInputs[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private boolean isAllFilled() {
        for (EditText et : otpInputs) {
            if (et.getText().length() == 0) return false;
        }
        return true;
    }

    private void verifyRealOTP() {
        StringBuilder code = new StringBuilder();
        for (EditText et : otpInputs) code.append(et.getText().toString());

        // Disable inputs while verifying to prevent editing
        for (EditText et : otpInputs) et.setEnabled(false);
        Toast.makeText(getContext(), "Verifying...", Toast.LENGTH_SHORT).show();

        AuthHelper.verifyOtp(code.toString(), new AuthHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Verified! Registering face...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), FaceRegisterActivity.class);
                faceScanLauncher.launch(intent);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                // Re-enable inputs on failure so user can retry
                for (EditText et : otpInputs) et.setEnabled(true);
                Toast.makeText(getContext(), "Invalid Code. Please try again.", Toast.LENGTH_SHORT).show();

                // Clear inputs and focus first box
                for (EditText et : otpInputs) et.setText("");
                otpInputs[0].requestFocus();
            }
        });
    }

    // ⭐ THE FINAL FIX: Background image upload happens right here!
    private void createProfile() {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Uploading documents and finalizing...", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co";
                String SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi";

                // 1. Iterate through the cached members and upload their documents
                if (RegistrationCache.tempHouseholdList != null) {
                    for (int i = 0; i < RegistrationCache.tempHouseholdList.size(); i++) {
                        HouseholdMember member = RegistrationCache.tempHouseholdList.get(i);
                        int originalRowIndex = i + 1; // Because index 1 is Head, index 2 is first relative

                        // If a document URI exists for this specific row...
                        if (SignUpStep2Household_fragment.memberDocuments != null &&
                                SignUpStep2Household_fragment.memberDocuments.containsKey(originalRowIndex)) {

                            android.net.Uri imageUri = SignUpStep2Household_fragment.memberDocuments.get(originalRowIndex);

                            // Convert local URI into raw bytes for uploading
                            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
                            byte[] fileBytes = new byte[is.available()];
                            is.read(fileBytes);
                            is.close();

                            // Generate a unique filename for Supabase Storage
                            String fileName = System.currentTimeMillis() + "_member_" + originalRowIndex + ".jpg";

                            // Perform the POST request to the household_docs bucket
                            RequestBody body = RequestBody.create(fileBytes, MediaType.parse("image/jpeg"));
                            Request request = new Request.Builder()
                                    .url(SUPABASE_URL + "/storage/v1/object/household_docs/" + fileName)
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                                    .post(body)
                                    .build();

                            try (Response response = client.newCall(request).execute()) {
                                if (response.isSuccessful()) {
                                    // SUCCESS! Attach the public URL to the Kotlin HouseholdMember model
                                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/household_docs/" + fileName;
                                    member.setIdImageUrl(publicUrl);
                                } else {
                                    Log.e("Upload", "Failed to upload document for member " + originalRowIndex);
                                }
                            }
                        }
                    }
                }

                // 2. Now that the URLs are attached, proceed to save the profile & household in Supabase
                requireActivity().runOnUiThread(() -> {
                    AuthHelper.createProfileAfterVerification(requireContext(), new AuthHelper.RegistrationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) return;

                            // Free up memory by clearing the temporary document URIs
                            if (SignUpStep2Household_fragment.memberDocuments != null) {
                                SignUpStep2Household_fragment.memberDocuments.clear();
                            }

                            Toast.makeText(getContext(), "Welcome aboard!", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }

                        @Override
                        public void onError(String message) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Profile Save Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error uploading documents. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void startTimer(long duration) {
        isResendEnabled = false;
        if (tvTimer != null) {
            tvTimer.setEnabled(false);
            tvTimer.setTextColor(Color.GRAY);
        }

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millis) {
                if (tvTimer != null) {
                    long minutes = (millis / 1000) / 60;
                    long seconds = (millis / 1000) % 60;

                    String timerText = String.format("Resend code in %02d:%02d", minutes, seconds);
                    tvTimer.setText(timerText);

                    // ⭐ TRANSLATE DYNAMIC TIMER
                    TranslationHelper.autoTranslate(getContext(), tvTimer, timerText);
                }
            }
            public void onFinish() {
                isResendEnabled = true;
                if (tvTimer != null) {
                    String resendText = "Resend Code";
                    tvTimer.setText(resendText);
                    TranslationHelper.autoTranslate(getContext(), tvTimer, resendText);

                    tvTimer.setEnabled(true);
                    tvTimer.setTextColor(Color.BLUE);
                }
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}