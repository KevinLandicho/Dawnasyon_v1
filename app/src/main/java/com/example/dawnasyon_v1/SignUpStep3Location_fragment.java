package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpStep3Location_fragment extends BaseFragment {

    private EditText etProv, etCity, etBrgy, etStreet;

    public SignUpStep3Location_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step3_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        etProv = view.findViewById(R.id.et_prov);
        etCity = view.findViewById(R.id.et_city);
        etBrgy = view.findViewById(R.id.et_brgy);
        etStreet = view.findViewById(R.id.et_street);

        Button btnNext = view.findViewById(R.id.btn_submit); // Using the same ID 'btn_submit' from XML
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // Optional: Change button text to "Next" since there is one more step
        btnNext.setText("Next →");

        // --- Navigate to Step 4 (Account Info) ---
        btnNext.setOnClickListener(v -> {
            // 1. Validation
//            if (etProv.getText().toString().isEmpty() ||
//                    etCity.getText().toString().isEmpty() ||
//                    etBrgy.getText().toString().isEmpty()) {
//                Toast.makeText(getContext(), "Please fill in all location fields", Toast.LENGTH_SHORT).show();
//                return;
//            }

            // 2. Navigate to SignUpStepAccount_fragment
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpStepAccount_fragment())
                    .addToBackStack(null) // Allow back navigation
                    .commit();
        });

        // --- Go Back to Step 2 ---
        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}