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

    // ⭐ ADDED: etEmail
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

        // ⭐ BIND EMAIL VIEW (Make sure you added this ID to your XML!)
        etEmail = view.findViewById(R.id.et_email);

        cbNoMiddleName = view.findViewById(R.id.cb_no_middlename);
        btnNext = view.findViewById(R.id.btn_next);
        btnPrevious = view.findViewById(R.id.btn_previous);
        ivIdPreview = view.findViewById(R.id.iv_id_preview);

        // ⭐ HIDE ID PREVIEW FOR OVERSEAS USERS (Since they skipped ID upload)
        if ("Overseas".equals(RegistrationCache.userType)) {
            ivIdPreview.setVisibility(View.GONE);
        }

        // --- RECEIVE DATA FROM STEP 0 (If available) ---
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

        // --- UI LOGIC ---

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

        // ⭐ UPDATED NEXT BUTTON LISTENER ⭐
        btnNext.setOnClickListener(v -> {
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String mName = etMiddleName.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            // 1. Validate Required Fields
            if (fName.isEmpty() || lName.isEmpty() || contact.isEmpty() || email.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields (Name, Contact, Email)", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Construct Full Name
            String fullName;
            if (cbNoMiddleName.isChecked() || mName.isEmpty()) {
                fullName = fName + " " + lName;
            } else {
                fullName = fName + " " + mName + " " + lName;
            }

            // 3. Save to Cache (Crucial for Step 4 and Supabase)
            RegistrationCache.tempFullName = fullName;
            RegistrationCache.tempContact = contact;
            RegistrationCache.tempEmail = email;

            // ⭐ FIX: SAVE THE ID IMAGE URI TO CACHE HERE
            if (finalIdUri != null) {
                RegistrationCache.tempIdImageUri = finalIdUri.toString();
            }

            // 4. ⭐ HANDLE NAVIGATION (Overseas vs Local)
            Fragment nextFragment;

            if ("Overseas".equals(RegistrationCache.userType)) {
                // Overseas skips Household (Step 2) -> Goes to Location (Step 3)
                nextFragment = new SignUpStep3Location_fragment();
            } else {
                // Local goes to Household (Step 2)
                nextFragment = new SignUpStep2Household_fragment();
            }

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, nextFragment)
                    .addToBackStack(null)
                    .commit();
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}