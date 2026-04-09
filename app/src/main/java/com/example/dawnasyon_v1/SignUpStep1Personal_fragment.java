package com.example.dawnasyon_v1;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SignUpStep1Personal_fragment extends BaseFragment {

    private EditText etFirstName, etMiddleName, etLastName, etContact, etEmail;
    private CheckBox cbNoMiddleName;
    private Button btnNext, btnPrevious;
    private ImageView ivIdPreview;

    private Uri finalIdUri;

    // ⭐ Variables to hold original scanned data
    private String originalFName = "";
    private String originalLName = "";
    private String extractedAddress = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step1_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etFirstName = view.findViewById(R.id.et_firstname);
        etMiddleName = view.findViewById(R.id.et_middlename);
        etLastName = view.findViewById(R.id.et_lastname);
        etContact = view.findViewById(R.id.et_contact);
        etEmail = view.findViewById(R.id.et_email);

        cbNoMiddleName = view.findViewById(R.id.cb_no_middlename);
        btnNext = view.findViewById(R.id.btn_next);
        btnPrevious = view.findViewById(R.id.btn_previous);
        ivIdPreview = view.findViewById(R.id.iv_id_preview);

        if ("Overseas".equalsIgnoreCase(RegistrationCache.userType) || "Non-Resident".equalsIgnoreCase(RegistrationCache.userType)) {
            ivIdPreview.setVisibility(View.GONE);
            if (ivIdPreview.getParent() instanceof View) {
                ((View) ivIdPreview.getParent()).setVisibility(View.GONE);
            }
        }

        setupRealTimeValidation();

        if (getArguments() != null) {
            originalFName = getArguments().getString("FNAME", "");
            originalLName = getArguments().getString("LNAME", "");
            String mName = getArguments().getString("MNAME", "");
            extractedAddress = getArguments().getString("EXTRACTED_ADDRESS", "");

            if (!originalFName.isEmpty()) etFirstName.setText(originalFName);
            if (!originalLName.isEmpty()) etLastName.setText(originalLName);

            if (!mName.isEmpty()) {
                etMiddleName.setText(mName);
                cbNoMiddleName.setChecked(false);
            }

            String uriString = getArguments().getString("ID_IMAGE_URI", "");
            if (!uriString.isEmpty() && "Resident".equalsIgnoreCase(RegistrationCache.userType)) {
                finalIdUri = Uri.parse(uriString);
                ivIdPreview.setImageURI(finalIdUri);
            } else {
                finalIdUri = null;
            }
        }

        // ⭐ NEW FIX: Lock name fields if the user is a Resident
        if ("Resident".equalsIgnoreCase(RegistrationCache.userType)) {
            etFirstName.setEnabled(false);
            etLastName.setEnabled(false);
            etMiddleName.setEnabled(false);
            cbNoMiddleName.setEnabled(false); // Lock the checkbox so they can't toggle it

            // Dim the fields slightly so the user knows they are locked
            etFirstName.setAlpha(0.7f);
            etLastName.setAlpha(0.7f);
            etMiddleName.setAlpha(0.7f);
        }

        cbNoMiddleName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etMiddleName.setText("");
                etMiddleName.setEnabled(false);
                etMiddleName.setAlpha(0.5f);
            } else {
                // Only re-enable the middle name field if they are NOT a Resident
                if (!"Resident".equalsIgnoreCase(RegistrationCache.userType)) {
                    etMiddleName.setEnabled(true);
                    etMiddleName.setAlpha(1.0f);
                }
            }
            validateForm();
        });

        // --- NEXT BUTTON WITH STRICT CENSUS VALIDATION ---
        btnNext.setOnClickListener(v -> {
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String mName = etMiddleName.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || contact.isEmpty() || email.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("Resident".equalsIgnoreCase(RegistrationCache.userType) || "Non-Resident".equalsIgnoreCase(RegistrationCache.userType)) {
                if (contact.length() != 11 || !contact.startsWith("09")) {
                    etContact.setError("Must be an 11-digit number starting with '09'.");
                    etContact.requestFocus();
                    return;
                }
            }

            if (!email.toLowerCase().endsWith("@gmail.com")) {
                etEmail.setError("Email must be a valid @gmail.com address.");
                etEmail.requestFocus();
                return;
            }

            String fullName;
            if (cbNoMiddleName.isChecked() || mName.isEmpty()) {
                fullName = fName + " " + lName;
            } else {
                fullName = fName + " " + mName + " " + lName;
            }

            if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

            // ⭐ 1. Check if email/name is already in our app
            SupabaseJavaHelper.checkUserExists(fullName, email, new SupabaseJavaHelper.SimpleCallback() {
                @Override
                public void onSuccess() {

                    // ⭐ 2. If they are a Resident, STRICTLY verify them against the Master Census
                    if ("Resident".equalsIgnoreCase(RegistrationCache.userType)) {

                        SupabaseJavaHelper.verifyAgainstMasterCensus(fullName, new SupabaseJavaHelper.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                // FOUND IN CENSUS! Let them pass.
                                if (isAdded()) {
                                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                                    proceedToNextStep(fullName, contact, email, fName, lName);
                                }
                            }

                            @Override
                            public void onError(String msg) {
                                // NOT FOUND IN CENSUS (Or already registered)
                                if (isAdded()) {
                                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                                    // Alert Dialog to be very clear to the user
                                    new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                            .setTitle("Validation Failed")
                                            .setMessage(msg)
                                            .setPositiveButton("OK", null)
                                            .show();
                                }
                            }
                        });

                    } else {
                        // If they are a Donor/Overseas, skip the census check and let them pass
                        if (isAdded()) {
                            if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                            proceedToNextStep(fullName, contact, email, fName, lName);
                        }
                    }
                }

                @Override
                public void onError(String message) {
                    if (isAdded()) {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        validateForm();
        applyTagalogTranslation(view);
    }

    private void setupRealTimeValidation() {
        TextWatcher formWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateForm(); }
        };
        etFirstName.addTextChangedListener(formWatcher);
        etMiddleName.addTextChangedListener(formWatcher);
        etLastName.addTextChangedListener(formWatcher);
        etContact.addTextChangedListener(formWatcher);
        etEmail.addTextChangedListener(formWatcher);
    }

    private void validateForm() {
        boolean isFirstNameFilled = !etFirstName.getText().toString().trim().isEmpty();
        boolean isLastNameFilled = !etLastName.getText().toString().trim().isEmpty();
        boolean isContactFilled = !etContact.getText().toString().trim().isEmpty();
        boolean isEmailFilled = !etEmail.getText().toString().trim().isEmpty();
        boolean isMiddleNameValid = cbNoMiddleName.isChecked() || !etMiddleName.getText().toString().trim().isEmpty();

        if (isFirstNameFilled && isLastNameFilled && isContactFilled && isEmailFilled && isMiddleNameValid) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
        } else {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
        }
    }

    private void proceedToNextStep(String fullName, String contact, String email, String typedFName, String typedLName) {
        RegistrationCache.tempFullName = fullName;
        RegistrationCache.tempContact = contact;
        RegistrationCache.tempEmail = email;

        // ⭐ THE FIX: Pass an empty string instead of null!
        if (finalIdUri != null) {
            RegistrationCache.tempIdImageUri = finalIdUri.toString();
        } else {
            RegistrationCache.tempIdImageUri = "";
        }

        String nameMismatchNote = "";
        if (!originalFName.isEmpty() && !originalLName.isEmpty()) {

            String normTypedFName = typedFName.toUpperCase().replaceAll("\\s+", " ");
            String normTypedLName = typedLName.toUpperCase().replaceAll("\\s+", " ");
            String normScannedFName = originalFName.toUpperCase().replaceAll("\\s+", " ");
            String normScannedLName = originalLName.toUpperCase().replaceAll("\\s+", " ");

            boolean hasMismatch = false;

            if (!normScannedFName.contains(normTypedFName) && !normTypedFName.contains(normScannedFName)) {
                hasMismatch = true;
            }
            if (!normScannedLName.contains(normTypedLName) && !normTypedLName.contains(normScannedLName)) {
                hasMismatch = true;
            }

            if (hasMismatch) {
                nameMismatchNote = "⚠️ NAME MISMATCH: User typed [" + typedFName + " " + typedLName + "], but ID showed [" + originalFName + " " + originalLName + "].\n";
                Log.w("SignUpMismatch", nameMismatchNote);
            }
        }

        RegistrationCache.nameMismatchNotes = nameMismatchNote;

        Fragment nextFragment;
        if ("Overseas".equalsIgnoreCase(RegistrationCache.userType)) {
            nextFragment = new SignUpStepAccount_fragment();
        } else {
            nextFragment = new SignUpStep2Household_fragment();
        }

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_signup, nextFragment)
                .addToBackStack(null)
                .commit();
    }
}