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
import androidx.fragment.app.Fragment;

public class DeleteConfirmation_fragment extends BaseFragment {

    private CountDownTimer deleteTimer;
    private TextView tvCountdown;

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
        Button btnCancel = view.findViewById(R.id.btn_cancel_timer);
        Button btnDeleteNow = view.findViewById(R.id.btn_delete_now);

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
                // Update UI (approx seconds remaining)
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
        // Ensure timer is cancelled if called manually
        if (deleteTimer != null) deleteTimer.cancel();

        // TODO: Call API to actually delete the user account here

        Toast.makeText(getContext(), "Account Deleted Successfully.", Toast.LENGTH_LONG).show();

        // Redirect to Login (Clear entire back stack)
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Safety: Cancel timer if user navigates away or app closes
        if (deleteTimer != null) deleteTimer.cancel();
    }
}