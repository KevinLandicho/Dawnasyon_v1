package com.example.dawnasyon_v1;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SignUpStep3Location_fragment extends BaseFragment {

    private EditText etHouseNo, etZip;
    private AutoCompleteTextView dropdownProv, dropdownCity, dropdownBrgy, dropdownStreet;
    private Button btnSubmit, btnPrevious;

    // ⭐ Variables to hold data passed from the Cache
    private String extractedAddress = "";
    private String existingNotes = "";

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

        // ⭐ EXPLICITLY ALLOW MANUAL TYPING IN STREET DROPDOWN
        // We set threshold to 1 so the dropdown only appears if what they type matches the sample list
        dropdownStreet.setInputType(InputType.TYPE_CLASS_TEXT);
        dropdownStreet.setThreshold(1);

        // ⭐ INITIALIZE VALIDATION TRACKER
        setupRealTimeValidation();

        // ⭐ FETCH DATA DIRECTLY FROM CACHE, NOT ARGUMENTS
        extractedAddress = RegistrationCache.extractedAddress != null ? RegistrationCache.extractedAddress : "";
        existingNotes = RegistrationCache.nameMismatchNotes != null ? RegistrationCache.nameMismatchNotes : "";

        // Log to prove the cache has the address
        Log.d("SignUpMismatch", "Cache Extracted Address: [" + extractedAddress + "]");

        // 1. Load Data
        PhLocationHelper.loadData(requireContext());

        // ⭐ 2. CONDITIONAL SETUP BASED ON USER TYPE
        if ("Resident".equalsIgnoreCase(RegistrationCache.userType)) {
            setupResidentMode();
        } else {
            setupNonResidentMode();
        }

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ⭐ SUBMIT BUTTON WITH ADDRESS VALIDATION
        btnSubmit.setOnClickListener(v -> {
            String prov = dropdownProv.getText().toString().trim();
            String city = dropdownCity.getText().toString().trim();
            String brgy = dropdownBrgy.getText().toString().trim();
            String street = dropdownStreet.getText().toString().trim();
            String house = etHouseNo.getText().toString().trim();
            String zip = etZip.getText().toString().trim();

            // ⭐ 3. ADDRESS MISMATCH LOGIC
            String typedAddress = house + " " + street + " " + brgy + " " + city;
            String currentNotes = existingNotes;

            if (!extractedAddress.isEmpty()) {
                String normalizedScanned = extractedAddress.toUpperCase().replace("Ñ", "N");
                String normalizedTypedStreet = street.toUpperCase().replace("Ñ", "N");
                String normalizedTypedHouse = house.toUpperCase();
                String normalizedTypedBrgy = brgy.toUpperCase();
                String normalizedTypedCity = city.toUpperCase();

                boolean hasMismatch = false;

                if (!normalizedScanned.contains(normalizedTypedHouse)) hasMismatch = true;
                if (!normalizedScanned.contains(normalizedTypedStreet)) hasMismatch = true;
                if (!normalizedScanned.contains(normalizedTypedBrgy)) hasMismatch = true;
                if (!normalizedScanned.contains(normalizedTypedCity)) hasMismatch = true;

                if (hasMismatch) {
                    String addressMismatch = "⚠️ ADDRESS MISMATCH: User typed [" + typedAddress + "], but ID showed [" + extractedAddress + "].\n";
                    Log.w("SignUpMismatch", addressMismatch);
                    currentNotes += addressMismatch;
                }
            }

            RegistrationCache.notes = currentNotes;

            String verifyingText = "Verifying Address...";
            btnSubmit.setText(verifyingText);
            TranslationHelper.autoTranslate(getContext(), btnSubmit, verifyingText);

            btnSubmit.setEnabled(false);

            if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

            SupabaseJavaHelper.checkAddressExists(house, street, brgy, city, new SupabaseJavaHelper.AddressCheckCallback() {
                @Override
                public void onResult(boolean isDuplicate) {
                    if (isAdded()) {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                        if (isDuplicate) {
                            Toast.makeText(getContext(), "📍 Address found in system! You will be registered under this existing household.", Toast.LENGTH_LONG).show();
                        }

                        proceedToAccountCreation(prov, city, brgy, street, house, zip);
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                        String submitText = "Submit";
                        btnSubmit.setText(submitText);
                        TranslationHelper.autoTranslate(getContext(), btnSubmit, submitText);

                        btnSubmit.setEnabled(true);

                        Toast.makeText(getContext(), "Validation Failed: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        // Run validation once immediately to disable button on fresh load
        validateForm();

        applyTagalogTranslation(view);
    }

    // ==========================================
    // ⭐ REAL-TIME VALIDATION LOGIC
    // ==========================================
    private void setupRealTimeValidation() {
        TextWatcher formWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateForm();
            }
        };

        etHouseNo.addTextChangedListener(formWatcher);
        etZip.addTextChangedListener(formWatcher);
        dropdownProv.addTextChangedListener(formWatcher);
        dropdownCity.addTextChangedListener(formWatcher);
        dropdownBrgy.addTextChangedListener(formWatcher);
        dropdownStreet.addTextChangedListener(formWatcher);
    }

    private void validateForm() {
        boolean isHouseFilled = !etHouseNo.getText().toString().trim().isEmpty();
        boolean isZipFilled = !etZip.getText().toString().trim().isEmpty();
        boolean isProvFilled = !dropdownProv.getText().toString().trim().isEmpty();
        boolean isCityFilled = !dropdownCity.getText().toString().trim().isEmpty();
        boolean isBrgyFilled = !dropdownBrgy.getText().toString().trim().isEmpty();
        boolean isStreetFilled = !dropdownStreet.getText().toString().trim().isEmpty();

        if (isHouseFilled && isZipFilled && isProvFilled && isCityFilled && isBrgyFilled && isStreetFilled) {
            btnSubmit.setEnabled(true);
            btnSubmit.setAlpha(1.0f);
        } else {
            btnSubmit.setEnabled(false);
            btnSubmit.setAlpha(0.5f);
        }
    }

    // ==========================================
    // ⭐ UI SETUP HELPERS
    // ==========================================

    private void addDropdownIcon(AutoCompleteTextView dropdown) {
        Drawable arrow = ContextCompat.getDrawable(requireContext(), android.R.drawable.arrow_down_float);
        if (arrow != null) {
            arrow.setTint(Color.GRAY);
            dropdown.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null);
            dropdown.setCompoundDrawablePadding(16);
        }
    }

    private void removeDropdownIcon(AutoCompleteTextView dropdown) {
        dropdown.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
    }

    // ⭐ LOGIC FOR RESIDENTS (LOCKED FIELDS)
    private void setupResidentMode() {
        dropdownProv.setText("Metro Manila");
        dropdownCity.setText("Quezon City");
        dropdownBrgy.setText("Santa Lucia");
        etZip.setText("1117");

        lockField(dropdownProv);
        lockField(dropdownCity);
        lockField(dropdownBrgy);

        etZip.setEnabled(true);
        dropdownStreet.setEnabled(true);
        dropdownStreet.setFocusableInTouchMode(true);
        dropdownStreet.setClickable(true);

        // ⭐ Only add the dropdown icon if they don't want to type freely.
        // If we want them to type freely, it's best to not have the arrow button force-opening the menu.
        // addDropdownIcon(dropdownStreet);

        List<String> streets = getSampleStreets("Santa Lucia");
        // ⭐ USING CUSTOM DROPDOWN LAYOUT
        ArrayAdapter<String> streetAdapter = new ArrayAdapter<>(requireContext(), R.layout.custom_dropdown_item, streets);
        dropdownStreet.setAdapter(streetAdapter);

        // ⭐ FIXED: Removed the on-click force dropdown so they can type their own street comfortably!
        // dropdownStreet.setOnClickListener(v -> dropdownStreet.showDropDown());
        // dropdownStreet.setOnFocusChangeListener((v, hasFocus) -> {
        //     if (hasFocus) dropdownStreet.showDropDown();
        // });

        validateForm();
    }

    // ⭐ LOGIC FOR NON-RESIDENTS (OPEN FIELDS)
    private void setupNonResidentMode() {
        setupCascadingDropdowns();

        addDropdownIcon(dropdownProv);
        addDropdownIcon(dropdownCity);
        addDropdownIcon(dropdownBrgy);

        // ⭐ Removed the forced dropdown icon for street here as well to allow free typing
        // addDropdownIcon(dropdownStreet);

        setupDropdownTrigger(dropdownProv);
        setupDropdownTrigger(dropdownCity);
        setupDropdownTrigger(dropdownBrgy);

        // ⭐ FIXED: Removed the on-click force dropdown here too
        // dropdownStreet.setOnClickListener(v -> dropdownStreet.showDropDown());

        validateForm();
    }

    private void lockField(AutoCompleteTextView view) {
        view.setEnabled(false);
        view.setFocusable(false);
        view.setClickable(false);
        view.setAdapter(null);
        removeDropdownIcon(view);
    }

    private void proceedToAccountCreation(String prov, String city, String brgy, String street, String house, String zip) {
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
    }

    private void setupDropdownTrigger(AutoCompleteTextView dropdown) {
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dropdown.showDropDown();
            }
        });
    }

    private void setupCascadingDropdowns() {
        List<String> provinces = PhLocationHelper.getProvinces();
        ArrayAdapter<String> provAdapter = new ArrayAdapter<>(requireContext(), R.layout.custom_dropdown_item, provinces);
        dropdownProv.setAdapter(provAdapter);

        dropdownProv.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProv = (String) parent.getItemAtPosition(position);
            dropdownCity.setText(""); dropdownBrgy.setText(""); dropdownStreet.setText("");
            dropdownBrgy.setAdapter(null); dropdownStreet.setAdapter(null);

            List<String> cities = PhLocationHelper.getCities(selectedProv);
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(), R.layout.custom_dropdown_item, cities);
            dropdownCity.setAdapter(cityAdapter);
            dropdownCity.requestFocus(); dropdownCity.showDropDown();
        });

        dropdownCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            dropdownBrgy.setText(""); dropdownStreet.setText("");

            List<String> brgys = PhLocationHelper.getBarangays(selectedCity);
            ArrayAdapter<String> brgyAdapter = new ArrayAdapter<>(requireContext(), R.layout.custom_dropdown_item, brgys);
            dropdownBrgy.setAdapter(brgyAdapter);
            dropdownBrgy.requestFocus(); dropdownBrgy.showDropDown();
        });

        dropdownBrgy.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBrgy = (String) parent.getItemAtPosition(position);
            dropdownStreet.setText("");
            List<String> sampleStreets = getSampleStreets(selectedBrgy);
            ArrayAdapter<String> streetAdapter = new ArrayAdapter<>(requireContext(), R.layout.custom_dropdown_item, sampleStreets);
            dropdownStreet.setAdapter(streetAdapter);

            // ⭐ Allow them to type or see suggestions, but don't force the dropdown open instantly
            dropdownStreet.requestFocus();
        });
    }

    private List<String> getSampleStreets(String brgy) {
        List<String> streets = new ArrayList<>();
        streets.add("A. Bonifacio St.");
        streets.add("A. Mabini St.");
        streets.add("Burgos St.");
        streets.add("Castro St.");
        streets.add("Cursilista St.");
        streets.add("Dela Cruz St.");
        streets.add("Diego Silang St.");
        streets.add("Dona Field");
        streets.add("E. Aguinaldo St.");
        streets.add("E. Jacinto St.");
        streets.add("F. Agoncillo St.");
        streets.add("F. Balagtas St.");
        streets.add("F. Calderon St.");
        streets.add("Francisco Park");
        streets.add("Galvez St.");
        streets.add("Gen. Malvar St.");
        streets.add("Gomez St.");
        streets.add("Humabon St.");
        streets.add("J. Abad Santos St.");
        streets.add("J. Basa St.");
        streets.add("J. Luna St.");
        streets.add("J. Palma St.");
        streets.add("J.P. Rizal St.");
        streets.add("Lapu Lapu St.");
        streets.add("Lopez Jaena St");
        streets.add("Lower Visayas Ave");
        streets.add("M. Aquino St.");
        streets.add("M.H. Del Pilar St.");
        streets.add("Marco Polo St.");
        streets.add("Naning Ponce St.");
        streets.add("Natividad Subd.");
        streets.add("P. Bukaneg St");
        streets.add("P. Paterno St.");
        streets.add("Paguio St.");
        streets.add("Pamana St.");
        streets.add("Panganiban St.");
        streets.add("Plain Ville");
        streets.add("Rajah Soliman St.");
        streets.add("Rivera St.");
        streets.add("Sta. Lucia Ave.");
        streets.add("Sta. Marcela St.");
        streets.add("T. Alonzo St.");
        streets.add("Tarha Ville");
        streets.add("Upper Visayas");
        streets.add("Valbuena Compd.");
        streets.add("Villa Hermano 4");
        streets.add("Zamora St.");

        return streets;
    }
}