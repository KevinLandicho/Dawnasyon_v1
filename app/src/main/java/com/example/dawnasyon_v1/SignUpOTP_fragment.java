package com.example.dawnasyon_v1;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpOTP_fragment extends BaseFragment {

    private EditText[] otpInputs;
    private TextView tvTimer;
    private CountDownTimer countDownTimer;

    public SignUpOTP_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTimer = view.findViewById(R.id.tv_timer);
        Button btnPrevious = view.findViewById(R.id.btn_previous);
        TextView tvResend = view.findViewById(R.id.tv_resend);

        // --- Setup OTP Inputs (Auto-focus logic) ---
        otpInputs = new EditText[]{
                view.findViewById(R.id.otp_1),
                view.findViewById(R.id.otp_2),
                view.findViewById(R.id.otp_3),
                view.findViewById(R.id.otp_4),
                view.findViewById(R.id.otp_5),
                view.findViewById(R.id.otp_6)
        };
        setupOTPInputs();

        // --- Start Timer (5 Minutes) ---
        startTimer(5 * 60 * 1000);

        // --- Logic ---
        tvResend.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Code Resent!", Toast.LENGTH_SHORT).show();
            startTimer(5 * 60 * 1000); // Restart timer
        });

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
                    // Move to next box if length is 1
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    }

                    // If all filled, verify automatically
                    if (index == otpInputs.length - 1 && s.length() == 1) {
                        verifyOTP();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Optional: Move back if empty
                    if (s.length() == 0 && index > 0) {
                        otpInputs[index - 1].requestFocus();
                    }
                }
            });
        }
    }

    // ⭐ UPDATED VERIFICATION LOGIC ⭐
    private void verifyOTP() {
        // Collect code
        StringBuilder code = new StringBuilder();
        for (EditText et : otpInputs) {
            code.append(et.getText().toString());
        }

        if (code.length() == 6) {
            // Check if code matches "000000"
            if (code.toString().equals("000000")) {
                Toast.makeText(getContext(), "Verification Successful!", Toast.LENGTH_SHORT).show();

                // NAVIGATE TO VALID ID SCREEN
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new SignUpValidID_fragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                // Invalid Code
                Toast.makeText(getContext(), "Invalid Code. Please try again.", Toast.LENGTH_SHORT).show();

                // Optional: Clear inputs for retry
                // for (EditText et : otpInputs) et.setText("");
                // otpInputs[0].requestFocus();
            }
        }
    }

    private void startTimer(long duration) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("The code will expire in %02d:%02d", minutes, seconds));
            }

            public void onFinish() {
                tvTimer.setText("Code expired. Please resend.");
            }
        }.start();
    }
}