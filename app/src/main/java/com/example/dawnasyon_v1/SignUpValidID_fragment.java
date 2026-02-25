package com.example.dawnasyon_v1;

import android.app.Activity;
import android.app.AlertDialog;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpValidID_fragment extends BaseFragment {

    private Button btnStartScan, btnPrevious;

    // Hardcoded to QC ID as per client request
    private final String selectedIdType = "QC_ID";

    private Uri capturedImageUri = null;
    private String extractFName = "", extractLName = "", extractMName = "", extractAddress = "";

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

        // ⭐ CLEAR OLD NOTES WHEN STARTING A NEW SCAN
        RegistrationCache.notes = "";
        RegistrationCache.nameMismatchNotes = "";

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

        btnStartScan = view.findViewById(R.id.btn_start_scan);
        btnPrevious = view.findViewById(R.id.btn_previous);

        btnStartScan.setOnClickListener(v -> showScanOptionsDialog());
        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // ⭐ ENABLE AUTO-TRANSLATION FOR STATIC LAYOUT
        applyTagalogTranslation(view);
    }

    // --- 🛡️ SECURE VERIFICATION LOGIC ---

    private void verifyAndProcessImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            // ⭐ TRANSLATE LOADING STATE
            String verifyingText = "Verifying Authenticity...";
            btnStartScan.setText(verifyingText);
            TranslationHelper.autoTranslate(getContext(), btnStartScan, verifyingText);

            btnStartScan.setEnabled(false);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        // 1. RUN SECURITY CHECK (Strict to QC ID)
                        if (isContentValid(visionText.getText())) {
                            // 2. If valid, proceed to extraction
                            parseQCID(visionText.getText().split("\n"));
                            proceedToStep1();
                        } else {
                            // 3. If invalid, reject
                            showInvalidIdDialog();

                            // ⭐ TRANSLATE RESET STATE
                            String startText = "Start Scan";
                            btnStartScan.setText(startText);
                            TranslationHelper.autoTranslate(getContext(), btnStartScan, startText);

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

    // ⭐ STRICT QC ID VALIDATION
    private boolean isContentValid(String rawText) {
        String text = rawText.toUpperCase();
        return text.contains("QCITIZEN") ||
                text.contains("QUEZON CITY") ||
                text.contains("LUNGSOD QUEZON") ||
                text.contains("CITIZEN CARD") ||
                text.contains("KASAMA KA");
    }

    private void showInvalidIdDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Incorrect ID Type")
                .setMessage("The scanned image does not appear to be a Quezon City ID (QCitizen Card).\n\nPlease ensure you captured a clear photo of the correct document.")
                .setPositiveButton("Try Again", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showScanOptionsDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Scan QCitizen Card")
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

        // Pass Names
        args.putString("FNAME", extractFName);
        args.putString("LNAME", extractLName);
        args.putString("MNAME", extractMName);

        // ⭐ PASS THE EXTRACTED ADDRESS via Arguments (just in case Step 1 needs it)
        args.putString("EXTRACTED_ADDRESS", extractAddress);

        // ⭐ CRITICAL FIX: Save the address globally to the Cache so Step 3 can actually see it!
        RegistrationCache.extractedAddress = extractAddress;

        if (capturedImageUri != null) args.putString("ID_IMAGE_URI", capturedImageUri.toString());

        step1.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_signup, step1)
                .addToBackStack(null)
                .commit();
    }

    // --- 🔍 ADVANCED TEXT EXTRACTION LOGIC (QC ID SPECIFIC) ---

    private void parseQCID(String[] lines) {
        extractFName = ""; extractLName = ""; extractMName = ""; extractAddress = "";
        boolean expectName = false;
        StringBuilder rawBlocks = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            // Aggressively remove watermarks so they don't break the name parser
            String line = lines[i].toUpperCase()
                    .replaceAll("PREVIEW ONLY", "")
                    .replaceAll("PREVIEW", "")
                    .replaceAll("VIEW ONLY", "")
                    .trim();

            if (line.isEmpty()) continue;

            // Build a continuous string of the whole card for Regex to parse later
            rawBlocks.append(line).append(" ");

            // --- 1. EXTRACT NAME ---
            // Updated to catch both "M.I." (digital card) and "MIDDLE NAME" (physical card)
            if (line.contains("LAST NAME") && (line.contains("FIRST NAME") || line.contains("M.I.") || line.contains("MIDDLE"))) {
                expectName = true;
                continue;
            }

            if (expectName && line.contains(",")) {
                parseCommaSeparatedName(line);
                expectName = false;
            }
            // Fallback for Name: If we missed the header, but the line fits the format
            else if (extractLName.isEmpty() && line.contains(",") && !line.contains("EMERGENCY") && !line.matches(".*\\d.*") && !line.contains("QUEZON CITY")) {
                parseCommaSeparatedName(line);
            }
        }

        // --- 2. EXTRACT ADDRESS VIA REGEX ---
        extractAddress = extractCleanAddress(rawBlocks.toString());
    }

    // ⭐ COMPLETELY REWRITTEN: Isolates address using structure, not exact spelling
    private String extractCleanAddress(String fullCardText) {
        String cleanText = fullCardText.toUpperCase()
                .replaceAll("SINGLE", " ")
                .replaceAll("MARRIED", " ")
                .replaceAll("WIDOWED", " ");

        // Step 1: Remove the dates that usually bleed into the front of the address
        // This removes things like "2027/05/11" or the mangled "2027106 11" from your example
        cleanText = cleanText.replaceAll("\\d{4}/\\d{2}/\\d{2}", " ");
        cleanText = cleanText.replaceAll("20\\d{5}\\s*\\d*", " "); // Targets mangled dates starting with 20

        // Step 2: Extract everything starting from the first standalone number (House Number)
        // Look for a number (like 53) that might have a letter (like B) right after it
        Pattern addressStartPattern = Pattern.compile("(?<!\\d)\\d{1,4}\\s*[A-Z]?\\s+[A-ZÑ]+");
        Matcher startMatcher = addressStartPattern.matcher(cleanText);

        String potentialAddress = "";
        if (startMatcher.find()) {
            potentialAddress = cleanText.substring(startMatcher.start());
        } else {
            return ""; // Could not find a house number to start from
        }

        // Step 3: Chop off the garbage at the end (Emergency Info, Cardholder, Phone numbers)
        String[] stopWords = {"IN CASE", "EMERGENCY", "CESE", "SERGENC", "CONTACT", "CARDHOLDER", "RESIDENT", "09"};
        int earliestStopIndex = potentialAddress.length();

        for (String stopWord : stopWords) {
            int index = potentialAddress.indexOf(stopWord);
            if (index != -1 && index < earliestStopIndex) {
                earliestStopIndex = index;
            }
        }

        // Also look for long string of digits (phone number or barcode number) and cut there
        Pattern numbersPattern = Pattern.compile("\\d{5,}");
        Matcher numberMatcher = numbersPattern.matcher(potentialAddress);
        if (numberMatcher.find()) {
            if (numberMatcher.start() < earliestStopIndex) {
                earliestStopIndex = numberMatcher.start();
            }
        }

        // Make the cut
        potentialAddress = potentialAddress.substring(0, earliestStopIndex);

        // Step 4: Final Polish - Replace broken "QUEZON TO" back to "QUEZON CITY" if it happened
        potentialAddress = potentialAddress.replace("QUEZON TO", "QUEZON CITY");

        return potentialAddress.replaceAll("[^a-zA-Z0-9 Ññ.,-]", " ").replaceAll("\\s+", " ").trim();
    }

    private void parseCommaSeparatedName(String fullText) {
        if (!fullText.contains(",")) return;

        // Split only at the first comma to separate Last Name from the rest
        String[] parts = fullText.split(",", 2);

        extractLName = cleanText(parts[0]);

        if (parts.length > 1) {
            String firstAndMiddle = parts[1].trim();
            splitFirstAndMiddleName(firstAndMiddle);
        }
    }

    private void splitFirstAndMiddleName(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length > 1) {
            // Usually, the last word is the Middle Name (M.I. or full)
            extractMName = cleanText(parts[parts.length - 1]);

            // Everything else before it is the First Name
            StringBuilder first = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                first.append(parts[i]).append(" ");
            }
            extractFName = cleanText(first.toString().trim());
        } else {
            extractFName = cleanText(text);
            extractMName = "";
        }
    }

    private String cleanText(String input) {
        if (input == null) return "";
        return input.replace("Name:", "")
                .replace("Last Name", "")
                .replaceAll("[^a-zA-Z0-9 Ññ.,-]", "") // Keeps letters, numbers, and Ñ
                .replaceAll("\\s+", " ") // Removes extra spaces
                .trim();
    }
}