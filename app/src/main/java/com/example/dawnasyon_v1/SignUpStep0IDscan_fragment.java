package com.example.dawnasyon_v1;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
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

public class SignUpStep0IDscan_fragment extends BaseFragment {

    private Button btnScanStart, btnUploadId;

    // Variables to hold extracted data
    private Uri capturedImageUri = null;
    private String extractFName = "";
    private String extractLName = "";
    private String extractMName = "";

    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_step0_idscan, container, false);
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
                                runTextRecognition(capturedImageUri);
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
                        runTextRecognition(capturedImageUri);
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnScanStart = view.findViewById(R.id.btn_scan_start);
        btnUploadId = view.findViewById(R.id.btn_upload_id);

        btnScanStart.setOnClickListener(v -> startDocumentScan());
        btnUploadId.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    // ⭐ UPDATED METHOD TO PREVENT CRASH ON SCREEN RECORDING
    private void startDocumentScan() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        GmsDocumentScanning.getClient(options)
                .getStartScanIntent(requireActivity())
                .addOnSuccessListener(intentSender -> {
                    try {
                        scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error launching scanner.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // This catches the security exception when screen recording is active
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Cannot scan while screen recording is active.", Toast.LENGTH_LONG).show();
                });
    }

    private void runTextRecognition(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            btnScanStart.setText("Verifying ID...");
            btnScanStart.setEnabled(false);
            btnUploadId.setEnabled(false);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        // VALIDATION CHECK
                        if (isValidIdDocument(visionText.getText())) {
                            processSmartTextResult(visionText);
                            proceedToStep1();
                        } else {
                            // REJECT THE IMAGE
                            Toast.makeText(getContext(), "Invalid ID. Please upload a valid Government ID.", Toast.LENGTH_LONG).show();
                            resetButtons();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Could not verify image. Try again.", Toast.LENGTH_SHORT).show();
                        resetButtons();
                    });

        } catch (Exception e) {
            e.printStackTrace();
            resetButtons();
        }
    }

    // --- 🛡️ VALIDATION FUNCTION ---
    private boolean isValidIdDocument(String fullText) {
        String blob = fullText.toUpperCase();

        // 1. Must contain at least one "Government" keyword
        boolean hasGovKeyword = blob.contains("REPUBLIC") || blob.contains("PHILIPPINES") ||
                blob.contains("PILIPINAS") || blob.contains("QUEZON CITY") ||
                blob.contains("BARANGAY") || blob.contains("PASSPORT");

        // 2. Must contain at least one "Identity" keyword
        boolean hasIdKeyword = blob.contains("NAME") || blob.contains("SURNAME") ||
                blob.contains("APELYIDO") || blob.contains("BIRTH") ||
                blob.contains("QCITIZEN") || blob.contains("ID NO") ||
                blob.contains("LAST") || blob.contains("GIVEN");

        // It is valid ONLY if it has BOTH a Gov keyword AND an ID keyword
        return hasGovKeyword && hasIdKeyword;
    }

    private void resetButtons() {
        btnScanStart.setText("Scan with Camera");
        btnScanStart.setEnabled(true);
        btnUploadId.setEnabled(true);
        capturedImageUri = null; // Clear the invalid image
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

    // --- PARSING LOGIC ---
    private void processSmartTextResult(Text text) {
        String fullText = text.getText();
        String[] lines = fullText.split("\n");
        String blob = fullText.toUpperCase();

        if (blob.contains("QCITIZEN") || (blob.contains("QUEZON") && blob.contains("CITY"))) {
            parseQCID(lines);
        } else if (blob.contains("PHILSYS") || blob.contains("NATIONAL ID")) {
            parseNationalID(lines);
        } else if (blob.contains("BARANGAY") || blob.contains("OFFICE OF THE")) {
            parseBarangayID(lines);
        } else if (blob.contains("PASSPORT") || blob.contains("P<PHL")) {
            parsePassport(lines);
        } else {
            parseGenericID(lines);
        }
    }

    private void parseQCID(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("LAST NAME") && line.contains("FIRST NAME")) {
                if (i + 1 < lines.length) {
                    String valueLine = lines[i + 1].trim();
                    if (valueLine.contains(",")) {
                        String[] parts = valueLine.split(",");
                        if (parts.length >= 2) {
                            extractLName = cleanText(parts[0]);
                            splitFirstAndMiddleName(parts[1].trim());
                        }
                    } else {
                        extractLName = cleanText(valueLine);
                    }
                }
                return;
            }
        }
    }

    private void parseNationalID(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("LAST NAME") && i + 1 < lines.length)
                extractLName = cleanText(lines[i + 1]);
            if (line.contains("GIVEN NAME") && i + 1 < lines.length)
                extractFName = cleanText(lines[i + 1]);
            if (line.contains("MIDDLE NAME") && i + 1 < lines.length)
                extractMName = cleanText(lines[i + 1]);
        }
    }

    private void parsePassport(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (line.contains("SURNAME") && i + 1 < lines.length)
                extractLName = cleanText(lines[i + 1]);
            if (line.contains("GIVEN NAME") && i + 1 < lines.length)
                extractFName = cleanText(lines[i + 1]);
            if (line.contains("P<PHL")) {
                try {
                    String raw = line.substring(line.indexOf("P<PHL")).replace("P<PHL", "");
                    String[] parts = raw.split("<<");
                    if (parts.length >= 2) {
                        extractLName = parts[0].replace("<", " ").trim();
                        extractFName = parts[1].replace("<", " ").trim();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    private void parseBarangayID(String[] lines) {
        for (String rawLine : lines) {
            String line = rawLine.trim();
            String upper = line.toUpperCase();
            if (upper.contains("REPUBLIC") || upper.contains("BARANGAY") || upper.contains("ADDRESS"))
                continue;
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