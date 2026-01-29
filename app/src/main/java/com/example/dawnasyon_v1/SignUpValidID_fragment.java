package com.example.dawnasyon_v1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

public class SignUpValidID_fragment extends BaseFragment {

    private Button btnNationalId, btnQcId, btnBrgyId, btnPassport, btnLicense;
    private Button btnStartScan, btnPrevious;

    private String selectedIdType = null;
    private List<Button> allIdButtons;

    private Uri capturedImageUri = null;
    private String extractFName = "", extractLName = "", extractMName = "";

    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    public SignUpValidID_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_valid_id, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Camera Scanner
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        try {
                            GmsDocumentScanningResult res = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                            if (res != null && !res.getPages().isEmpty()) {
                                capturedImageUri = res.getPages().get(0).getImageUri();
                                verifyAndProcessImage(capturedImageUri);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // 2. Gallery Picker
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        capturedImageUri = uri;
                        verifyAndProcessImage(capturedImageUri);
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnNationalId = view.findViewById(R.id.btn_national_id);
        btnQcId = view.findViewById(R.id.btn_qc_id);
        btnBrgyId = view.findViewById(R.id.btn_brgy_id);
        btnPassport = view.findViewById(R.id.btn_passport);
        btnLicense = view.findViewById(R.id.btn_license);
        btnStartScan = view.findViewById(R.id.btn_start_scan);
        btnPrevious = view.findViewById(R.id.btn_previous);

        allIdButtons = new ArrayList<>();
        allIdButtons.add(btnNationalId);
        allIdButtons.add(btnQcId);
        allIdButtons.add(btnBrgyId);
        allIdButtons.add(btnPassport);
        allIdButtons.add(btnLicense);

        btnNationalId.setOnClickListener(v -> handleIdSelection("NATIONAL_ID", btnNationalId));
        btnQcId.setOnClickListener(v -> handleIdSelection("QC_ID", btnQcId));
        btnBrgyId.setOnClickListener(v -> handleIdSelection("BRGY_ID", btnBrgyId));
        btnPassport.setOnClickListener(v -> handleIdSelection("PASSPORT", btnPassport));
        btnLicense.setOnClickListener(v -> handleIdSelection("LICENSE", btnLicense));

        btnStartScan.setOnClickListener(v -> {
            if (selectedIdType == null) {
                Toast.makeText(getContext(), "Please select an ID type first.", Toast.LENGTH_SHORT).show();
                return;
            }
            showScanOptionsDialog();
        });

        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    // --- 🛡️ SECURE VERIFICATION LOGIC ---

    private void verifyAndProcessImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            btnStartScan.setText("Verifying Authenticity...");
            btnStartScan.setEnabled(false);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        // 1. RUN SECURITY CHECK (STRICT)
                        if (isContentValid(visionText.getText(), selectedIdType)) {
                            // 2. If valid, proceed to extraction
                            processTextResult(visionText, selectedIdType);
                            proceedToStep1();
                        } else {
                            // 3. If invalid, reject
                            showInvalidIdDialog();
                            btnStartScan.setText("Start Scan");
                            btnStartScan.setEnabled(true);
                            capturedImageUri = null;
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Verification Failed. Try again.", Toast.LENGTH_SHORT).show();
                        btnStartScan.setEnabled(true);
                    });

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ⭐ UPDATED: STRICT VALIDATION (No Generic Fallbacks)
    private boolean isContentValid(String rawText, String expectedType) {
        String text = rawText.toUpperCase();

        switch (expectedType) {
            case "QC_ID":
                // Must explicitly contain QC keywords
                return text.contains("QCITIZEN") ||
                        text.contains("QUEZON CITY") ||
                        text.contains("LUNGSOD QUEZON") ||
                        text.contains("CITIZEN CARD") ||
                        text.contains("KASAMA KA");

            case "LICENSE":
                // Must explicitly look like a license
                return (text.contains("DRIVER") && text.contains("LICENSE")) ||
                        text.contains("LAND TRANSPORTATION") ||
                        text.contains("LTO") ||
                        text.contains("PROFESSIONAL") ||
                        text.contains("NON-PROFESSIONAL");

            case "NATIONAL_ID":
                // Must explicitly be a National ID
                return text.contains("PHILSYS") ||
                        text.contains("PAMBANSANG") ||
                        text.contains("PCN") ||
                        text.contains("PHILIPPINE IDENTIFICATION");

            case "PASSPORT":
                // Must explicitly be a Passport
                return text.contains("PASSPORT") ||
                        text.contains("PASAPORTE") ||
                        text.contains("P<PHL");

            case "BRGY_ID":
                // Must explicitly be Barangay related
                return text.contains("BARANGAY") ||
                        text.contains("BRGY") ||
                        text.contains("OFFICE OF THE PUNONG");
        }

        return false; // Default: If no keywords match the selected type, REJECT.
    }

    private void showInvalidIdDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Incorrect ID Type")
                .setMessage("The scanned image does not match the selected ID type (" + getReadableIdName() + ").\n\nPlease ensure you selected the correct button and scanned the correct document.")
                .setPositiveButton("Try Again", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private String getReadableIdName() {
        switch (selectedIdType) {
            case "NATIONAL_ID": return "National ID";
            case "QC_ID": return "Quezon City ID";
            case "PASSPORT": return "Passport";
            case "LICENSE": return "Driver's License";
            default: return "ID";
        }
    }

    private void handleIdSelection(String idType, Button selectedButton) {
        selectedIdType = idType;
        for (Button btn : allIdButtons) btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFDAB9")));
    }

    private void showScanOptionsDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Scan ID")
                .setMessage("Choose an option:")
                .setPositiveButton("Camera", (dialog, which) -> startCameraScan())
                .setNegativeButton("Gallery", (dialog, which) -> galleryLauncher.launch("image/*"))
                .show();
    }

    private void startCameraScan() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();
        GmsDocumentScanning.getClient(options).getStartScanIntent(requireActivity())
                .addOnSuccessListener(i -> scannerLauncher.launch(new IntentSenderRequest.Builder(i).build()));
    }

    private void proceedToStep1() {
        SignUpStep1Personal_fragment step1 = new SignUpStep1Personal_fragment();
        Bundle args = new Bundle();
        args.putString("FNAME", extractFName);
        args.putString("LNAME", extractLName);
        args.putString("MNAME", extractMName);
        if (capturedImageUri != null) args.putString("ID_IMAGE_URI", capturedImageUri.toString());
        step1.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_signup, step1)
                .addToBackStack(null)
                .commit();
    }

    // --- 🔍 TEXT EXTRACTION LOGIC (Same robust logic as before) ---

    private void processTextResult(Text text, String type) {
        String[] lines = text.getText().split("\n");
        extractFName = ""; extractLName = ""; extractMName = "";

        switch (type) {
            case "QC_ID": parseQCID(lines); break;
            case "LICENSE": parseDriversLicense(lines); break;
            case "NATIONAL_ID": parseNationalID(lines); break;
            case "BRGY_ID": parseBarangayID(lines); break;
            case "PASSPORT": parsePassport(lines); break;
            default: parseGenericID(lines); break;
        }
    }

    private void parseQCID(String[] lines) {
        boolean headerFound = false;
        // Strategy 1: Header
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("LAST NAME") && (line.contains("FIRST NAME") || line.contains("M.I."))) {
                if (i + 1 < lines.length) {
                    parseCommaSeparatedName(lines[i + 1].trim());
                    headerFound = true;
                    break;
                }
            }
        }
        // Strategy 2: Fallback
        if (!headerFound || extractLName.isEmpty()) {
            for (String line : lines) {
                String cleanLine = line.trim().toUpperCase();
                if (cleanLine.contains("LAST NAME") || cleanLine.contains("FIRST NAME")) continue;
                if (cleanLine.contains(",") && !cleanLine.matches(".*\\d.*") && !cleanLine.contains("QUEZON CITY") && !cleanLine.contains("ADDRESS")) {
                    parseCommaSeparatedName(cleanLine);
                    if (!extractLName.isEmpty()) return;
                }
            }
        }
    }

    private void parseDriversLicense(String[] lines) {
        // Strategy 1: Header
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("LAST NAME") && line.contains("FIRST NAME")) {
                if (i + 1 < lines.length) {
                    parseCommaSeparatedName(lines[i + 1].trim());
                    return;
                }
            }
        }
        // Strategy 2: Fallback
        for (String line : lines) {
            String cleanLine = line.trim().toUpperCase();
            if (cleanLine.contains("LAST NAME") || cleanLine.contains("FIRST NAME")) continue;
            if (cleanLine.contains(",") && !cleanLine.contains("ADDRESS") && !cleanLine.contains("NAME")) {
                parseCommaSeparatedName(cleanLine);
                if (!extractLName.isEmpty()) return;
            }
        }
    }

    private void parseCommaSeparatedName(String fullText) {
        if (!fullText.contains(",")) return;
        String[] parts = fullText.split(",");
        String lastNameCandidate = cleanText(parts[0]);
        if (!lastNameCandidate.equalsIgnoreCase("NAME") && !lastNameCandidate.equalsIgnoreCase("LAST")) {
            extractLName = lastNameCandidate;
        }
        if (parts.length > 1) {
            String firstAndMiddle = parts[1].trim();
            splitFirstAndMiddleName(firstAndMiddle);
        }
    }

    private void parseNationalID(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("LAST NAME") && i+1 < lines.length) extractLName = cleanText(lines[i+1]);
            if (line.contains("GIVEN NAME") && i+1 < lines.length) extractFName = cleanText(lines[i+1]);
            if (line.contains("MIDDLE NAME") && i+1 < lines.length) extractMName = cleanText(lines[i+1]);
        }
    }

    private void parsePassport(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("SURNAME") && i+1 < lines.length) extractLName = cleanText(lines[i+1]);
            if (line.contains("GIVEN NAME") && i+1 < lines.length) extractFName = cleanText(lines[i+1]);
            if (line.contains("P<PHL")) {
                try {
                    String raw = line.substring(line.indexOf("P<PHL")).replace("P<PHL", "");
                    String[] parts = raw.split("<<");
                    if (parts.length >= 2) {
                        extractLName = parts[0].replace("<", " ").trim();
                        extractFName = parts[1].replace("<", " ").trim();
                    }
                } catch(Exception e){}
            }
        }
    }

    private void parseBarangayID(String[] lines) {
        for (String rawLine : lines) {
            String line = rawLine.trim();
            String upper = line.toUpperCase();
            if (upper.contains("REPUBLIC") || upper.contains("BARANGAY") || upper.contains("ADDRESS")) continue;
            if (upper.matches("[A-Z Ññ.-]{5,}") && !upper.matches(".*\\d.*")) {
                String[] words = line.split("\\s+");
                if (words.length > 1) {
                    extractLName = cleanText(words[words.length - 1]);
                    StringBuilder firstMiddle = new StringBuilder();
                    for (int k = 0; k < words.length - 1; k++) firstMiddle.append(words[k]).append(" ");
                    splitFirstAndMiddleName(firstMiddle.toString().trim());
                    return;
                }
            }
        }
    }

    private void parseGenericID(String[] lines) {
        int count = 0;
        for (String line : lines) {
            if (line.length() > 2 && !line.matches(".*\\d.*") && !line.toUpperCase().contains("REPUBLIC")) {
                if (count == 0) extractLName = cleanText(line);
                else if (count == 1) extractFName = cleanText(line);
                count++;
            }
        }
    }

    private void splitFirstAndMiddleName(String text) {
        String[] parts = text.trim().split(" ");
        if (parts.length > 1) {
            extractMName = cleanText(parts[parts.length - 1]);
            StringBuilder first = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) first.append(parts[i]).append(" ");
            extractFName = cleanText(first.toString());
        } else {
            extractFName = cleanText(text);
        }
    }

    private String cleanText(String input) {
        if (input == null) return "";
        return input.replace("Name:", "").replace("Last Name", "")
                .replaceAll("[^a-zA-Z Ññ.-]", "").trim();
    }
}