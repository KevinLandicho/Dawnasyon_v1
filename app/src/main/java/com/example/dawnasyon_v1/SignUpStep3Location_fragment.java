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

public class SignUpStep3Location_fragment extends BaseFragment {

    private EditText etHouseNo, etStreet, etBrgy, etCity, etProv, etZip;
    private Button btnSubmit, btnPrevious;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step3_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        etHouseNo = view.findViewById(R.id.et_house_no);
        etStreet = view.findViewById(R.id.et_street);
        etBrgy = view.findViewById(R.id.et_brgy);
        etCity = view.findViewById(R.id.et_city);
        etProv = view.findViewById(R.id.et_prov);
        etZip = view.findViewById(R.id.et_zip);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnPrevious = view.findViewById(R.id.btn_previous);

        // Optional: Update button text to "Next" since we aren't submitting yet
        btnSubmit.setText("Next");

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSubmit.setOnClickListener(v -> {
            // 1. Get Inputs
            String house = etHouseNo.getText().toString().trim();
            String street = etStreet.getText().toString().trim();
            String brgy = etBrgy.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String prov = etProv.getText().toString().trim();
            String zip = etZip.getText().toString().trim();

            if (house.isEmpty() || street.isEmpty() || brgy.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all address fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. ⭐ SAVE TO CACHE
            // This ensures Step 4 can access this data later when creating the profile
            RegistrationCache.tempHouseNo = house;
            RegistrationCache.tempStreet = street;
            RegistrationCache.tempBrgy = brgy;
            RegistrationCache.tempCity = city;
            RegistrationCache.tempProvince = prov;
            RegistrationCache.tempZip = zip;

            // 3. ⭐ NAVIGATE TO STEP 4 (ACCOUNT)
            // We do NOT submit to Supabase here. We move to the password screen.
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpStepAccount_fragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}