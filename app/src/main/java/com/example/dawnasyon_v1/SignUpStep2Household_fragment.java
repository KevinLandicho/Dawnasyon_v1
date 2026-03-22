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

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.HashMap;
import java.util.Map;

public class SignUpStep2Household_fragment extends BaseFragment {

    private LinearLayout membersContainer;
    private EditText etHouseNum;

    private GmsDocumentScanner scanner;
    private int currentScanningIndex = -1;

    public static Map<Integer, Uri> memberDocuments = new HashMap<>();

    public SignUpStep2Household_fragment() {}

    // ⭐ UPDATED LAUNCHER: Now intercepts the image and runs the OCR Validation!
    private final ActivityResultLauncher<IntentSenderRequest> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    GmsDocumentScanningResult scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null && scanningResult.getPages() != null && !scanningResult.getPages().isEmpty()) {

                        Uri imageUri = scanningResult.getPages().get(0).getImageUri();

                        // Pass the image to our AI Text Recognizer to verify it's a real document
                        validateDocumentWithOCR(imageUri, currentScanningIndex);
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

        memberDocuments.clear();

        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build();
        scanner = GmsDocumentScanning.getClient(options);

        etHouseNum.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMemberRows(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etHouseNum.setText("1");

        btnNext.setOnClickListener(v -> {
            if (saveMembersToCache()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_signup, new SignUpStep3Location_fragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        applyTagalogTranslation(view);
    }

    // ⭐ THE FIX: AI Document Validation Method
    private void validateDocumentWithOCR(Uri imageUri, int index) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            Toast.makeText(getContext(), "Validating document...", Toast.LENGTH_SHORT).show();

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String text = visionText.getText().trim();

                        // Check if the image contains enough text to be an ID or Certificate (10 characters minimum)
                        if (text.length() < 10) {
                            Toast.makeText(getContext(), "❌ DECLINED: Not a valid document. No text detected.", Toast.LENGTH_LONG).show();
                        } else {
                            // ✅ Success! The AI found text, meaning it's a real document!
                            memberDocuments.put(index, imageUri);
                            updateButtonToSuccess(index);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "❌ DECLINED: Unreadable document.", Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error processing image.", Toast.LENGTH_SHORT).show();
        }
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
        RegistrationCache.tempHouseholdList.clear();
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

            if (memberIndex > 1 && !memberDocuments.containsKey(memberIndex)) {
                Toast.makeText(getContext(), "Please scan the required document for member #" + memberIndex, Toast.LENGTH_SHORT).show();
                return false;
            }

            int age = Integer.parseInt(ageStr);

            HouseholdMember member = new HouseholdMember(
                    0L, null, name, relation, age, gender, true, false, null
            );

            RegistrationCache.tempHouseholdList.add(member);
        }
        return true;
    }

    private void updateMemberRows(String input) {
        int count = 1;
        try {
            if (!input.trim().isEmpty()) {
                count = Integer.parseInt(input.trim());
            }
        } catch (NumberFormatException e) {
            count = 1;
        }

        if (count < 1) count = 1;
        if (count > 15) count = 15;

        int currentChildCount = membersContainer.getChildCount();

        if (count > currentChildCount) {
            for (int i = currentChildCount; i < count; i++) {
                addMemberRow(i + 1);
            }
        } else if (count < currentChildCount) {
            for (int i = currentChildCount - 1; i >= count; i--) {
                int indexToRemove = i + 1;
                memberDocuments.remove(indexToRemove);
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
        Button btnUploadDoc = row.findViewById(R.id.btn_upload_doc);

        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders);
        spGender.setAdapter(genderAdapter);

        if (index == 1) {
            if (!RegistrationCache.tempFullName.isEmpty()) {
                etName.setText(RegistrationCache.tempFullName);
            }
            etName.setEnabled(false);
            etName.setFocusable(false);

            String[] headRelation = {"Head"};
            ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, headRelation);
            spRelation.setAdapter(relationAdapter);
            spRelation.setSelection(0);
            spRelation.setEnabled(false);
            spRelation.setClickable(false);

            if (btnUploadDoc != null) btnUploadDoc.setVisibility(View.GONE);

        } else {
            String[] relations = {"Spouse", "Son", "Daughter", "Parent", "Sibling", "Relative"};
            ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, relations);
            spRelation.setAdapter(relationAdapter);
            spRelation.setSelection(1);

            if (btnUploadDoc != null) {
                btnUploadDoc.setVisibility(View.VISIBLE);
                btnUploadDoc.setOnClickListener(v -> startScanner(index));

                if (memberDocuments.containsKey(index)) {
                    btnUploadDoc.setText("✅ Document Scanned Successfully");
                    btnUploadDoc.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            }
        }

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
                        btnUploadDoc.setText("Scan Birth Certificate");
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
        TranslationHelper.translateViewHierarchy(getContext(), row);
    }
}