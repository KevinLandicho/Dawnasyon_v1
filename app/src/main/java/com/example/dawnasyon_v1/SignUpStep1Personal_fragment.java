package com.example.dawnasyon_v1;

import android.net.Uri;
import android.os.Bundle;
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

        // ⭐ THE FIX: Hide both the image AND its parent container (the orange box/label)
        if ("Overseas".equalsIgnoreCase(RegistrationCache.userType) || "Non-Resident".equalsIgnoreCase(RegistrationCache.userType)) {
            ivIdPreview.setVisibility(View.GONE);

            // This safely hides the CardView/FrameLayout wrapping the image and text
            if (ivIdPreview.getParent() instanceof View) {
                ((View) ivIdPreview.getParent()).setVisibility(View.GONE);
            }
        }

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

            // Only process and attach the ID image if the user is a Resident
            String uriString = getArguments().getString("ID_IMAGE_URI", "");
            if (!uriString.isEmpty() && "Resident".equalsIgnoreCase(RegistrationCache.userType)) {
                finalIdUri = Uri.parse(uriString);
                ivIdPreview.setImageURI(finalIdUri);
            } else {
                finalIdUri = null;
            }
        }

        cbNoMiddleName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etMiddleName.setText("");
                etMiddleName.setEnabled(false);
                etMiddleName.setAlpha(0.5f);
            } else {
                etMiddleName.setEnabled(true);
                etMiddleName.setAlpha(1.0f);
            }
        });

        // --- NEXT BUTTON WITH STRICT LOCAL VALIDATION ---
        btnNext.setOnClickListener(v -> {
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String mName = etMiddleName.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || contact.isEmpty() || email.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields (Name, Contact, Email)", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("Resident".equalsIgnoreCase(RegistrationCache.userType) || "Non-Resident".equalsIgnoreCase(RegistrationCache.userType)) {
                if (contact.length() != 11) {
                    etContact.setError("Contact number must be exactly 11 digits.");
                    etContact.requestFocus();
                    return;
                }
                if (!contact.startsWith("09")) {
                    etContact.setError("Contact number must start with '09'.");
                    etContact.requestFocus();
                    return;
                }
            } else {
                if (contact.length() < 7) {
                    etContact.setError("Please enter a valid contact number.");
                    etContact.requestFocus();
                    return;
                }
            }

            if (!email.toLowerCase().endsWith("@gmail.com")) {
                etEmail.setError("Email must be a valid @gmail.com address.");
                etEmail.requestFocus();
                return;
            }

            String usernamePart = email.split("@")[0];
            if (usernamePart.length() < 6) {
                etEmail.setError("Fake email detected. Gmail usernames must be at least 6 characters.");
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

            checkSupabaseAndProceed(fullName, email, contact, fName, lName);
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        applyTagalogTranslation(view);
    }

    private void checkSupabaseAndProceed(String fullName, String email, String contact, String fName, String lName) {
        SupabaseJavaHelper.checkUserExists(fullName, email, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    proceedToNextStep(fullName, contact, email, fName, lName);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                    Toast.makeText(getContext(), "Validation Failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void proceedToNextStep(String fullName, String contact, String email, String typedFName, String typedLName) {
        RegistrationCache.tempFullName = fullName;
        RegistrationCache.tempContact = contact;
        RegistrationCache.tempEmail = email;

        if (finalIdUri != null) {
            RegistrationCache.tempIdImageUri = finalIdUri.toString();
        } else {
            RegistrationCache.tempIdImageUri = null;
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