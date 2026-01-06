package com.example.dawnasyon_v1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
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

public class SignUpOTP_fragment extends BaseFragment {

    private EditText[] otpInputs;
    private TextView tvTimer;
    private TextView tvSubtitle;
    private CountDownTimer countDownTimer;

    // ⭐ NEW: Launcher to handle the Face Scan result
    private ActivityResultLauncher<Intent> faceScanLauncher;

    public SignUpOTP_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ⭐ INITIALIZE THE LAUNCHER
        // This listens for when FaceRegisterActivity finishes
        faceScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Face scan was successful! NOW we create the profile.
                        Toast.makeText(getContext(), "Face Registered! Finalizing...", Toast.LENGTH_SHORT).show();
                        createProfile();
                    } else {
                        Toast.makeText(getContext(), "Face scan skipped or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        tvTimer = view.findViewById(R.id.tv_timer);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // Display the actual email from cache
        if (tvSubtitle != null) {
            tvSubtitle.setText("We've sent a code to " + RegistrationCache.tempEmail);
        }

        otpInputs = new EditText[]{
                view.findViewById(R.id.otp_1), view.findViewById(R.id.otp_2),
                view.findViewById(R.id.otp_3), view.findViewById(R.id.otp_4),
                view.findViewById(R.id.otp_5), view.findViewById(R.id.otp_6)
        };
        setupOTPInputs();
        startTimer(5 * 60 * 1000);

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
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

        Toast.makeText(getContext(), "Verifying code...", Toast.LENGTH_SHORT).show();

        AuthHelper.verifyOtp(code.toString(), new AuthHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

                // ⭐ CHANGED: Instead of createProfile(), launch Face Scan
                Toast.makeText(getContext(), "Code Verified! Please register your face.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), FaceRegisterActivity.class);
                faceScanLauncher.launch(intent);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Invalid Code: " + message, Toast.LENGTH_LONG).show();
                for (EditText et : otpInputs) et.setText("");
                otpInputs[0].requestFocus();
            }
        });
    }

    private void createProfile() {
        // NOTE: Ensure your AuthHelper uses RegistrationCache.faceEmbedding when saving the user!
        AuthHelper.createProfileAfterVerification(requireContext(), new AuthHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Registration Complete!", Toast.LENGTH_LONG).show();
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
    }

    private void startTimer(long duration) {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millis) {
                if (tvTimer != null)
                    tvTimer.setText(String.format("The code will expire in %02d:%02d", (millis/1000)/60, (millis/1000)%60));
            }
            public void onFinish() {
                if (tvTimer != null) tvTimer.setText("Code expired. Please resend.");
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}