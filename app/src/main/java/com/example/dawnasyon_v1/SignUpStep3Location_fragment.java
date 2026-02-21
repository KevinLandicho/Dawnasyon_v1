package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.text.InputType;
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

        // ⭐ EXPLICITLY ALLOW MANUAL TYPING IN STREET DROPDOWN
        dropdownStreet.setInputType(InputType.TYPE_CLASS_TEXT);
        dropdownStreet.setThreshold(1); // Show suggestions after 1 character is typed

        btnSubmit = view.findViewById(R.id.btn_submit);
        btnPrevious = view.findViewById(R.id.btn_previous);

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

            if (prov.isEmpty() || city.isEmpty() || brgy.isEmpty() || street.isEmpty() || house.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all address fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ⭐ TRANSLATE LOADING STATE
            String verifyingText = "Verifying Address...";
            btnSubmit.setText(verifyingText);
            TranslationHelper.autoTranslate(getContext(), btnSubmit, verifyingText);

            btnSubmit.setEnabled(false);

            if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

            // ⭐ USE THE NEW AddressCheckCallback
            SupabaseJavaHelper.checkAddressExists(house, street, brgy, city, new SupabaseJavaHelper.AddressCheckCallback() {
                @Override
                public void onResult(boolean isDuplicate) {
                    if (isAdded()) {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                        // If address exists, notify them but DON'T stop them!
                        if (isDuplicate) {
                            Toast.makeText(getContext(), "📍 Address found in system! You will be registered under this existing household.", Toast.LENGTH_LONG).show();
                        }

                        // Always proceed to the next step
                        proceedToAccountCreation(prov, city, brgy, street, house, zip);
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                        // ⭐ TRANSLATE RESET STATE
                        String submitText = "Submit";
                        btnSubmit.setText(submitText);
                        TranslationHelper.autoTranslate(getContext(), btnSubmit, submitText);

                        btnSubmit.setEnabled(true);

                        Toast.makeText(getContext(), "Validation Failed: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }

    // ⭐ LOGIC FOR RESIDENTS (LOCKED FIELDS)
    private void setupResidentMode() {
        // 1. Pre-fill fields
        dropdownProv.setText("Metro Manila");
        dropdownCity.setText("Quezon City");
        dropdownBrgy.setText("Santa Lucia");
        etZip.setText("1117");

        // 2. Lock specific fields
        lockField(dropdownProv);
        lockField(dropdownCity);
        lockField(dropdownBrgy);

        // 3. UNLOCK Zip Code and Street Name
        etZip.setEnabled(true);
        dropdownStreet.setEnabled(true);
        dropdownStreet.setFocusableInTouchMode(true);
        dropdownStreet.setClickable(true);

        // 4. Populate Street Dropdown with Santa Lucia Streets
        List<String> streets = getSampleStreets("Santa Lucia");
        ArrayAdapter<String> streetAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, streets);
        dropdownStreet.setAdapter(streetAdapter);

        // Setup Street Trigger
        dropdownStreet.setOnClickListener(v -> dropdownStreet.showDropDown());
        dropdownStreet.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dropdownStreet.showDropDown();
        });
    }

    // ⭐ LOGIC FOR NON-RESIDENTS (OPEN FIELDS)
    private void setupNonResidentMode() {
        setupCascadingDropdowns();

        // Force Dropdowns to Open on Click
        setupDropdownTrigger(dropdownProv);
        setupDropdownTrigger(dropdownCity);
        setupDropdownTrigger(dropdownBrgy);
        dropdownStreet.setOnClickListener(v -> dropdownStreet.showDropDown());
    }

    private void lockField(AutoCompleteTextView view) {
        view.setEnabled(false);
        view.setFocusable(false);
        view.setClickable(false);
        view.setAdapter(null); // Remove dropdown adapter so it doesn't show suggestions
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
        ArrayAdapter<String> provAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, provinces);
        dropdownProv.setAdapter(provAdapter);

        dropdownProv.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProv = (String) parent.getItemAtPosition(position);
            dropdownCity.setText(""); dropdownBrgy.setText(""); dropdownStreet.setText("");
            dropdownBrgy.setAdapter(null); dropdownStreet.setAdapter(null);

            List<String> cities = PhLocationHelper.getCities(selectedProv);
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cities);
            dropdownCity.setAdapter(cityAdapter);
            dropdownCity.requestFocus(); dropdownCity.showDropDown();
        });

        dropdownCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            dropdownBrgy.setText(""); dropdownStreet.setText("");

            List<String> brgys = PhLocationHelper.getBarangays(selectedCity);
            ArrayAdapter<String> brgyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, brgys);
            dropdownBrgy.setAdapter(brgyAdapter);
            dropdownBrgy.requestFocus(); dropdownBrgy.showDropDown();
        });

        dropdownBrgy.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBrgy = (String) parent.getItemAtPosition(position);
            dropdownStreet.setText("");
            List<String> sampleStreets = getSampleStreets(selectedBrgy);
            ArrayAdapter<String> streetAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sampleStreets);
            dropdownStreet.setAdapter(streetAdapter);
            dropdownStreet.requestFocus(); dropdownStreet.showDropDown();
        });
    }

    private List<String> getSampleStreets(String brgy) {
        List<String> streets = new ArrayList<>();

        // Add all unique streets from your list
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