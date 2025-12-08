package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpStep1Personal_fragment extends BaseFragment {
    public SignUpStep1Personal_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step1_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Navigation Logic
        view.findViewById(R.id.btn_next).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpStep2Household_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.btn_previous).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}