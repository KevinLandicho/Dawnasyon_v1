package com.example.dawnasyon_v1;

import android.net.Uri;
import android.os.Bundle;
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

        if ("Overseas".equals(RegistrationCache.userType)) {
            ivIdPreview.setVisibility(View.GONE);
        }

        if (getArguments() != null) {
            String fName = getArguments().getString("FNAME", "");
            String lName = getArguments().getString("LNAME", "");
            String mName = getArguments().getString("MNAME", "");

            if (!fName.isEmpty()) etFirstName.setText(fName);
            if (!lName.isEmpty()) etLastName.setText(lName);

            if (!mName.isEmpty()) {
                etMiddleName.setText(mName);
                cbNoMiddleName.setChecked(false);
            }

            String uriString = getArguments().getString("ID_IMAGE_URI", "");
            if (!uriString.isEmpty()) {
                finalIdUri = Uri.parse(uriString);
                ivIdPreview.setImageURI(finalIdUri);
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

        // --- NEXT BUTTON WITH STRICT VALIDATION ---
        btnNext.setOnClickListener(v -> {
            // Trim inputs to remove leading/trailing spaces
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String mName = etMiddleName.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            // 1. Check for Empty Fields
            if (fName.isEmpty() || lName.isEmpty() || contact.isEmpty() || email.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields (Name, Contact, Email)", Toast.LENGTH_SHORT).show();
                return;
            }

            // ⭐ 2. VALIDATE CONTACT NUMBER (Must be 11 digits)
            if (contact.length() != 11) {
                etContact.setError("Contact number must be exactly 11 digits.");
                etContact.requestFocus();
                return;
            }

            // ⭐ 3. VALIDATE EMAIL (Must contain @gmail.com)
            // .toLowerCase() ensures "User@Gmail.com" is accepted as "user@gmail.com"
            if (!email.toLowerCase().endsWith("@gmail.com")) {
                etEmail.setError("Email must be a valid @gmail.com address.");
                etEmail.requestFocus();
                return;
            }

            // Construct Full Name
            String fullName;
            if (cbNoMiddleName.isChecked() || mName.isEmpty()) {
                fullName = fName + " " + lName;
            } else {
                fullName = fName + " " + mName + " " + lName;
            }

            if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

            // ⭐ Call SupabaseJavaHelper checkUserExists
            SupabaseJavaHelper.checkUserExists(fullName, email, new SupabaseJavaHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                        proceedToNextStep(fullName, contact, email);
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
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }

    private void proceedToNextStep(String fullName, String contact, String email) {
        RegistrationCache.tempFullName = fullName;
        RegistrationCache.tempContact = contact;
        RegistrationCache.tempEmail = email;

        if (finalIdUri != null) {
            RegistrationCache.tempIdImageUri = finalIdUri.toString();
        }

        Fragment nextFragment;
        if ("Overseas".equals(RegistrationCache.userType)) {
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