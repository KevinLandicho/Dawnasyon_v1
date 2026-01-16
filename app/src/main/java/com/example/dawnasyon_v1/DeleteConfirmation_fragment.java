package com.example.dawnasyon_v1;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DeleteConfirmation_fragment extends BaseFragment {

    private CountDownTimer deleteTimer;
    private TextView tvCountdown;
    private Button btnCancel, btnDeleteNow;
    private boolean isDeleting = false; // Prevent double clicks

    public DeleteConfirmation_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delete_confirmation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCountdown = view.findViewById(R.id.tv_countdown);
        btnCancel = view.findViewById(R.id.btn_cancel_timer);
        btnDeleteNow = view.findViewById(R.id.btn_delete_now);

        // Start the 10-second countdown
        startDeleteTimer();

        // Cancel Button: Stop timer and go back
        btnCancel.setOnClickListener(v -> {
            if (deleteTimer != null) deleteTimer.cancel();
            getParentFragmentManager().popBackStack();
        });

        // Delete Now Button: Skip timer and delete immediately
        btnDeleteNow.setOnClickListener(v -> performAccountDeletion());
    }

    private void startDeleteTimer() {
        deleteTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = (millisUntilFinished / 1000) + 1;
                tvCountdown.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("0");
                performAccountDeletion();
            }
        }.start();
    }

    private void performAccountDeletion() {
        // 1. Safety Checks
        if (deleteTimer != null) deleteTimer.cancel();
        if (isDeleting) return; // Prevent double execution
        isDeleting = true;

        // UI Feedback
        btnDeleteNow.setText("Deleting...");
        btnDeleteNow.setEnabled(false);
        btnCancel.setEnabled(false);

        // 2. Call the Archive Function
        // We use AuthHelper (or SupabaseJavaHelper depending on where you put the code)
        SupabaseJavaHelper.archiveAccount(new SupabaseJavaHelper.RegistrationCallback() {
            @Override
            public void onSuccess() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Account has been deleted.", Toast.LENGTH_LONG).show();

                    // 3. Redirect to Login
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    isDeleting = false;
                    btnDeleteNow.setText("Delete Now");
                    btnDeleteNow.setEnabled(true);
                    btnCancel.setEnabled(true);
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (deleteTimer != null) deleteTimer.cancel();
    }
}