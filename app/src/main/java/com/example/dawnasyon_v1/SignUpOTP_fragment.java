package com.example.dawnasyon_v1;

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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SignUpOTP_fragment extends BaseFragment {

    private EditText[] otpInputs;
    private TextView tvTimer;
    private TextView tvSubtitle;
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
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        Button btnPrevious = view.findViewById(R.id.btn_previous);
        TextView tvResend = view.findViewById(R.id.tv_resend);

        // Display the actual email from cache
        if (tvSubtitle != null) {
            tvSubtitle.setText("We've sent a code to " + RegistrationCache.tempEmail);
        }

        // Setup 6 OTP inputs to match your Supabase config
        otpInputs = new EditText[]{
                view.findViewById(R.id.otp_1), view.findViewById(R.id.otp_2),
                view.findViewById(R.id.otp_3), view.findViewById(R.id.otp_4),
                view.findViewById(R.id.otp_5), view.findViewById(R.id.otp_6)
        };
        setupOTPInputs();

        // Start 5 min timer to match Supabase '300 seconds' config
        startTimer(5 * 60 * 1000);

       // tvResend.setOnClickListener(v -> resendCode());
        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupOTPInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;

            // Handle typing and auto-forward
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

            // Handle backspace to move focus backward
            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpInputs[index].getText().length() == 0 && index > 0) {
                        otpInputs[index - 1].requestFocus();
                        otpInputs[index - 1].setText(""); // Optional: clear previous box on backspace
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
                if (!isAdded()) return; // Safety check
                Toast.makeText(getContext(), "Verified! Saving Profile...", Toast.LENGTH_SHORT).show();
                createProfile();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Invalid Code: " + message, Toast.LENGTH_LONG).show();
                // Clear inputs for retry
                for (EditText et : otpInputs) et.setText("");
                otpInputs[0].requestFocus();
            }
        });
    }

    private void createProfile() {
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

//    private void resendCode() {
//        // UPDATED: Call resendOtp instead of initiateSignUp
//        AuthHelper.resendOtp(new AuthHelper.RegistrationCallback() {
//            @Override
//            public void onSuccess() {
//                if (!isAdded()) return;
//                Toast.makeText(getContext(), "A new code has been sent!", Toast.LENGTH_SHORT).show();
//                startTimer(5 * 60 * 1000); // Reset the visual timer
//            }
//            @Override
//            public void onError(String message) {
//                if (!isAdded()) return;
//                Toast.makeText(getContext(), "Resend Failed: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

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