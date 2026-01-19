package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SignUpStep3Location_fragment extends BaseFragment {

    private EditText etHouseNo, etZip;
    private AutoCompleteTextView dropdownProv, dropdownCity, dropdownBrgy, dropdownStreet;
    private Button btnSubmit, btnPrevious;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step3_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHouseNo = view.findViewById(R.id.et_house_no);
        etZip = view.findViewById(R.id.et_zip);

        dropdownProv = view.findViewById(R.id.et_prov_dropdown);
        dropdownCity = view.findViewById(R.id.et_city_dropdown);
        dropdownBrgy = view.findViewById(R.id.et_brgy_dropdown);
        dropdownStreet = view.findViewById(R.id.et_street_dropdown);

        btnSubmit = view.findViewById(R.id.btn_submit);
        btnPrevious = view.findViewById(R.id.btn_previous);

        // 1. Load Data
        PhLocationHelper.loadData(requireContext());

        // 2. Setup Logic
        setupCascadingDropdowns();

        // ⭐ 3. FORCE DROPDOWNS TO OPEN ON CLICK
        setupDropdownTrigger(dropdownProv);
        setupDropdownTrigger(dropdownCity);
        setupDropdownTrigger(dropdownBrgy);
        // Note: We don't force 'Street' because we want the keyboard to open first for typing.
        // But if you want suggestions to appear immediately upon clicking street, you can add it here too.
        dropdownStreet.setOnClickListener(v -> dropdownStreet.showDropDown());

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSubmit.setOnClickListener(v -> {
            String prov = dropdownProv.getText().toString().trim();
            String city = dropdownCity.getText().toString().trim();
            String brgy = dropdownBrgy.getText().toString().trim();
            String street = dropdownStreet.getText().toString().trim();
            String house = etHouseNo.getText().toString().trim();
            String zip = etZip.getText().toString().trim();

            if (prov.isEmpty() || city.isEmpty() || brgy.isEmpty() || street.isEmpty() || house.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all address fields", Toast.LENGTH_SHORT).show();
                return;
            }

            RegistrationCache.tempProvince = prov;
            RegistrationCache.tempCity = city;
            RegistrationCache.tempBrgy = brgy;
            RegistrationCache.tempStreet = street;
            RegistrationCache.tempHouseNo = house;
            RegistrationCache.tempZip = zip;

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpStepAccount_fragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // ⭐ NEW HELPER METHOD: Forces the list to open when clicked
    private void setupDropdownTrigger(AutoCompleteTextView dropdown) {
        // Open when clicked
        dropdown.setOnClickListener(v -> dropdown.showDropDown());

        // Open when focused (e.g. tapping next from previous field)
        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dropdown.showDropDown();
            }
        });
    }

    private void setupCascadingDropdowns() {
        // A. Load Provinces
        List<String> provinces = PhLocationHelper.getProvinces();
        ArrayAdapter<String> provAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, provinces);
        dropdownProv.setAdapter(provAdapter);

        // B. On Province Selected
        dropdownProv.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProv = (String) parent.getItemAtPosition(position);

            dropdownCity.setText("");
            dropdownBrgy.setText("");
            dropdownStreet.setText("");

            dropdownBrgy.setAdapter(null);
            dropdownStreet.setAdapter(null);

            List<String> cities = PhLocationHelper.getCities(selectedProv);
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cities);
            dropdownCity.setAdapter(cityAdapter);

            // Auto-open next dropdown for convenience
            dropdownCity.requestFocus();
            dropdownCity.showDropDown();
        });

        // C. On City Selected
        dropdownCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);

            dropdownBrgy.setText("");
            dropdownStreet.setText("");

            List<String> brgys = PhLocationHelper.getBarangays(selectedCity);
            ArrayAdapter<String> brgyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, brgys);
            dropdownBrgy.setAdapter(brgyAdapter);

            // Auto-open next dropdown
            dropdownBrgy.requestFocus();
            dropdownBrgy.showDropDown();
        });

        // D. On Barangay Selected
        dropdownBrgy.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBrgy = (String) parent.getItemAtPosition(position);
            dropdownStreet.setText("");

            List<String> sampleStreets = getSampleStreets(selectedBrgy);
            ArrayAdapter<String> streetAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sampleStreets);
            dropdownStreet.setAdapter(streetAdapter);

            // Focus street (Keyboard will open because it is textCapWords)
            dropdownStreet.requestFocus();
        });
    }

    private List<String> getSampleStreets(String brgy) {
        List<String> streets = new ArrayList<>();
        streets.add("Rizal Street");
        streets.add("Mabini Street");
        streets.add("Quezon Avenue");
        return streets;
    }
}