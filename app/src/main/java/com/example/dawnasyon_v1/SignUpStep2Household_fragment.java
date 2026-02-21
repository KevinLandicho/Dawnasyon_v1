package com.example.dawnasyon_v1;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.util.HashMap;
import java.util.Map;

public class SignUpStep2Household_fragment extends BaseFragment {

    private LinearLayout membersContainer;
    private EditText etHouseNum;

    // Document Scanner Variables
    private GmsDocumentScanner scanner;
    private int currentScanningIndex = -1;

    // Temporarily store scanned document URIs mapped to the member's row index
    // This is public static so the Final Registration step can access these URIs to upload them
    public static Map<Integer, Uri> memberDocuments = new HashMap<>();

    public SignUpStep2Household_fragment() {}

    // Launcher for the Document Scanner
    private final ActivityResultLauncher<IntentSenderRequest> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    GmsDocumentScanningResult scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null && scanningResult.getPages() != null && !scanningResult.getPages().isEmpty()) {

                        // Get the cropped image URI
                        Uri imageUri = scanningResult.getPages().get(0).getImageUri();

                        // Save it to our map for this specific member
                        memberDocuments.put(currentScanningIndex, imageUri);

                        // Update UI to show success
                        updateButtonToSuccess(currentScanningIndex);
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step2_household, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHouseNum = view.findViewById(R.id.et_house_num);
        membersContainer = view.findViewById(R.id.ll_members_container);
        Button btnNext = view.findViewById(R.id.btn_next);
        Button btnPrevious = view.findViewById(R.id.btn_previous);

        // Clear documents when entering the page fresh
        memberDocuments.clear();

        // Initialize the Document Scanner (Auto-Crop Box Mode)
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                .setGalleryImportAllowed(true) // Allows uploading from gallery!
                .setPageLimit(1) // Only need 1 page (front of ID or Birth Cert)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build();
        scanner = GmsDocumentScanning.getClient(options);

        // --- DYNAMIC POPULATION LOGIC ---
        etHouseNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMemberRows(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Automatically set to 1 so the first row appears
        etHouseNum.setText("1");

        // Navigation
        btnNext.setOnClickListener(v -> {
            if (saveMembersToCache()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new SignUpStep3Location_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }

    private void startScanner(int index) {
        currentScanningIndex = index;
        scanner.getStartScanIntent(requireActivity())
                .addOnSuccessListener(intentSender -> {
                    scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to open scanner: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateButtonToSuccess(int index) {
        // Find the button in the specific row and change it to green/success
        if (membersContainer != null && index - 1 < membersContainer.getChildCount()) {
            View row = membersContainer.getChildAt(index - 1);
            Button btnUploadDoc = row.findViewById(R.id.btn_upload_doc);
            if (btnUploadDoc != null) {
                btnUploadDoc.setText("Document Scanned Successfully");
                btnUploadDoc.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private boolean saveMembersToCache() {
        RegistrationCache.tempHouseholdList.clear(); // Start fresh
        int childCount = membersContainer.getChildCount();

        if (childCount == 0) {
            Toast.makeText(getContext(), "Please add at least one household member.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (int i = 0; i < childCount; i++) {
            View row = membersContainer.getChildAt(i);
            int memberIndex = i + 1;

            EditText etName = row.findViewById(R.id.et_name);
            EditText etAge = row.findViewById(R.id.et_age);
            Spinner spGender = row.findViewById(R.id.sp_gender);
            Spinner spRelation = row.findViewById(R.id.sp_relation);

            if (etName == null || etAge == null) continue;

            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String gender = spGender.getSelectedItem().toString();
            String relation = spRelation.getSelectedItem().toString();

            if (name.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in Name and Age for member #" + memberIndex, Toast.LENGTH_SHORT).show();
                return false;
            }

            // Validate that everyone except the Head has uploaded a document
            if (memberIndex > 1 && !memberDocuments.containsKey(memberIndex)) {
                Toast.makeText(getContext(), "Please scan the required document for member #" + memberIndex, Toast.LENGTH_SHORT).show();
                return false;
            }

            int age = Integer.parseInt(ageStr);

            // Create Member Object
            HouseholdMember member = new HouseholdMember(
                    0L,    // member_id
                    null,  // head_id
                    name,
                    relation,
                    age,
                    gender,
                    true,  // is_registered_census
                    false  // is_authorized_proxy
            );

            RegistrationCache.tempHouseholdList.add(member);
        }
        return true;
    }

    private void updateMemberRows(String input) {
        int count = 0;
        try {
            if (!input.isEmpty()) {
                count = Integer.parseInt(input);
            }
        } catch (NumberFormatException e) {
            count = 0;
        }

        if (count > 15) count = 15;

        int currentChildCount = membersContainer.getChildCount();

        if (count > currentChildCount) {
            for (int i = currentChildCount; i < count; i++) {
                addMemberRow(i + 1);
            }
        } else if (count < currentChildCount) {
            for (int i = currentChildCount - 1; i >= count; i--) {
                int indexToRemove = i + 1;
                memberDocuments.remove(indexToRemove); // Clean up removed images
                membersContainer.removeViewAt(i);
            }
        }
    }

    private void addMemberRow(int index) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_household_member, membersContainer, false);

        TextView tvNumber = row.findViewById(R.id.tv_row_number);
        if (tvNumber != null) tvNumber.setText(index + ".");

        EditText etName = row.findViewById(R.id.et_name);
        Spinner spGender = row.findViewById(R.id.sp_gender);
        Spinner spRelation = row.findViewById(R.id.sp_relation);
        EditText etAge = row.findViewById(R.id.et_age);
        Button btnUploadDoc = row.findViewById(R.id.btn_upload_doc); // From your XML

        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spGender.setAdapter(genderAdapter);

        // Logic for first row (Registrant / Head)
        if (index == 1) {
            if (!RegistrationCache.tempFullName.isEmpty()) {
                etName.setText(RegistrationCache.tempFullName);
            }
            etName.setEnabled(false);
            etName.setFocusable(false);

            // Head only gets "Head" option
            String[] headRelation = {"Head"};
            ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, headRelation);
            spRelation.setAdapter(relationAdapter);
            spRelation.setSelection(0);
            spRelation.setEnabled(false);
            spRelation.setClickable(false);

            // Hide document button for the Head
            if (btnUploadDoc != null) btnUploadDoc.setVisibility(View.GONE);

        } else {
            // Logic for dynamically added members
            // Removed "Head", Added "Sibling"
            String[] relations = {"Spouse", "Son", "Daughter", "Parent", "Sibling", "Relative"};
            ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, relations);
            spRelation.setAdapter(relationAdapter);
            spRelation.setSelection(1); // Default to Son/Daughter

            // Setup document button for members
            if (btnUploadDoc != null) {
                btnUploadDoc.setVisibility(View.VISIBLE);
                btnUploadDoc.setOnClickListener(v -> startScanner(index));

                // If they already scanned it (e.g., scrolled away and back), keep it green
                if (memberDocuments.containsKey(index)) {
                    btnUploadDoc.setText("✅ Document Scanned Successfully");
                    btnUploadDoc.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            }
        }

        // Setup Age Up/Down logic and Document text changes
        View btnUp = row.findViewById(R.id.btn_age_up);
        View btnDown = row.findViewById(R.id.btn_age_down);

        etAge.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (index > 1 && btnUploadDoc != null && !memberDocuments.containsKey(index)) {
                    int age = 0;
                    try { age = Integer.parseInt(s.toString()); } catch (Exception ignored) {}

                    if (age < 18) {
                        btnUploadDoc.setText("Upload Birth Certificate");
                    } else {
                        btnUploadDoc.setText("Scan ID Document");
                    }
                }
            }
        });

        btnUp.setOnClickListener(v -> {
            int age = 0;
            try {
                String text = etAge.getText().toString();
                if (!text.isEmpty()) age = Integer.parseInt(text);
            } catch (NumberFormatException e) { age = 0; }
            etAge.setText(String.valueOf(age + 1));
        });

        btnDown.setOnClickListener(v -> {
            int age = 0;
            try {
                String text = etAge.getText().toString();
                if (!text.isEmpty()) age = Integer.parseInt(text);
            } catch (NumberFormatException e) { age = 0; }
            if (age > 0) etAge.setText(String.valueOf(age - 1));
        });

        membersContainer.addView(row);

        // ⭐ TRANSLATE THE NEW DYNAMIC ROW IMMEDIATELY
        TranslationHelper.translateViewHierarchy(getContext(), row);
    }
}