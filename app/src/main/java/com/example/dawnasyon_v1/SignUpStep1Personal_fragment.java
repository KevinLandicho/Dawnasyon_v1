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
import androidx.cardview.widget.CardView;

public class SignUpStep1Personal_fragment extends BaseFragment {

    private EditText etFirstName, etMiddleName, etLastName, etContact;
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
        cbNoMiddleName = view.findViewById(R.id.cb_no_middlename);

        btnNext = view.findViewById(R.id.btn_next);
        btnPrevious = view.findViewById(R.id.btn_previous);
        ivIdPreview = view.findViewById(R.id.iv_id_preview);

        // --- RECEIVE DATA FROM STEP 0 ---
        if (getArguments() != null) {

            // 1. Text Auto-Fill
            String fName = getArguments().getString("FNAME", "");
            String lName = getArguments().getString("LNAME", "");
            String mName = getArguments().getString("MNAME", "");

            if (!fName.isEmpty()) etFirstName.setText(fName);
            if (!lName.isEmpty()) etLastName.setText(lName);

            if (!mName.isEmpty()) {
                etMiddleName.setText(mName);
                cbNoMiddleName.setChecked(false);
            }

            // 2. Image Preview
            // Since Step 0 is mandatory, we assume this is always present.
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

        btnNext.setOnClickListener(v -> {
            if (etFirstName.getText().toString().isEmpty() || etLastName.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Proceed to Step 2
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_signup, new SignUpStep2Household_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }
}