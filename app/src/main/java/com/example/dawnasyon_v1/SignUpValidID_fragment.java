package com.example.dawnasyon_v1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
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
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpValidID_fragment extends BaseFragment {

    private Button btnStartScan, btnPrevious;
    private TextView tvHowToQcid;
    private RadioGroup rgIdType;

    private Uri capturedImageUri = null;
    private String extractFName = "", extractLName = "", extractMName = "", extractAddress = "";

    // 1. Original ML Kit Document Scanner
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    // ⭐ 2. Launcher for Custom Live Selfie Activity
    private ActivityResultLauncher<Intent> selfieLauncher;

    public SignUpValidID_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up_valid_id, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RegistrationCache.notes = "";
        RegistrationCache.nameMismatchNotes = "";

        // --- 1. ML KIT DOCUMENT SCANNER (For ID) ---
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

        // --- 2. GALLERY PICKER (For ID) ---
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        capturedImageUri = uri;
                        verifyAndProcessImage(capturedImageUri);
                    }
                }
        );

        // ⭐ 3. CUSTOM LIVE SELFIE CAMERA RESULT ---
        selfieLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selfieUri = Uri.parse(result.getData().getStringExtra("SELFIE_URI"));

                        try {
                            // Convert the selfie URI to a Bitmap for the Face Verification API
                            Bitmap selfieBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selfieUri);
                            performFaceMatch(selfieBitmap);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error processing selfie.", Toast.LENGTH_SHORT).show();
                            resetScanButton();
                        }
                    } else {
                        Toast.makeText(getContext(), "Selfie cancelled. Verification failed.", Toast.LENGTH_SHORT).show();
                        resetScanButton();
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStartScan = view.findViewById(R.id.btn_start_scan);
        btnPrevious = view.findViewById(R.id.btn_previous);
        tvHowToQcid = view.findViewById(R.id.tv_how_to_qcid);
        rgIdType = view.findViewById(R.id.rg_id_type);

        btnStartScan.setOnClickListener(v -> showScanOptionsDialog());
        btnPrevious.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        tvHowToQcid.setOnClickListener(v -> {
            String url = "https://quezoncity.gov.ph/qcitizen-guides/how-to-apply-for-a-qcitizen-id/";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        applyTagalogTranslation(view);
    }

    private String getCurrentIdType() {
        if (rgIdType == null) return "QC_ID";
        int checkedId = rgIdType.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_national_id) return "NATIONAL_ID";
        if (checkedId == R.id.rb_drivers_license) return "DRIVERS_LICENSE";
        return "QC_ID";
    }

    // --- 🛡️ SECURE VERIFICATION LOGIC ---

    private void verifyAndProcessImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            btnStartScan.setText("Reading ID...");
            btnStartScan.setEnabled(false);
            btnStartScan.setAlpha(0.5f);

            String selectedType = getCurrentIdType();

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        if (isContentValid(visionText.getText(), selectedType)) {

                            String[] lines = visionText.getText().split("\n");
                            if (selectedType.equals("NATIONAL_ID")) {
                                parseNationalID(lines);
                            } else if (selectedType.equals("DRIVERS_LICENSE")) {
                                parseDriversLicense(lines);
                            } else {
                                parseQCID(lines);
                            }

                            // ⭐ NEW FLOW: Trigger the Custom Live Selfie UI instead of moving on
                            launchSelfieCamera();

                        } else {
                            showInvalidIdDialog(selectedType);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "OCR Failed. Try again.", Toast.LENGTH_SHORT).show();
                        resetScanButton();
                        capturedImageUri = null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading image. Try again.", Toast.LENGTH_SHORT).show();
            resetScanButton();
            capturedImageUri = null;
        }
    }

    // ⭐ Launch the Custom Camera Activity
    private void launchSelfieCamera() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnStartScan.setText("Take a Live Selfie");
                new AlertDialog.Builder(getContext())
                        .setTitle("ID Read Successfully")
                        .setMessage("To verify your identity, please take a clear live selfie. We will compare this to the photo on your ID.")
                        .setPositiveButton("Take Selfie", (dialog, which) -> {
                            Intent intent = new Intent(requireContext(), LiveIdScannerActivity.class);
                            selfieLauncher.launch(intent);
                        })
                        .setCancelable(false)
                        .show();
            });
        }
    }

    // ⭐ Send the images to Face++
    private void performFaceMatch(Bitmap selfieBitmap) {
        btnStartScan.setText("Analyzing Biometrics...");
        btnStartScan.setEnabled(false);

        FaceVerificationHelper.compareFaces(requireContext(), capturedImageUri, selfieBitmap, new FaceVerificationHelper.FaceMatchCallback() {
            @Override
            public void onSuccess(double confidenceScore) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (confidenceScore >= 80.0) {
                        Toast.makeText(getContext(), "Identity Verified! Match Score: " + String.format("%.1f", confidenceScore) + "%", Toast.LENGTH_LONG).show();
                        resetScanButton();
                        proceedToStep1();
                    } else {
                        showSpoofAlert(confidenceScore);
                    }
                });
            }

            @Override
            public void onFailed(String reason) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Verification Error: " + reason, Toast.LENGTH_LONG).show();
                    resetScanButton();
                });
            }
        });
    }

    private void showSpoofAlert(double score) {
        new AlertDialog.Builder(getContext())
                .setTitle("Identity Verification Failed")
                .setMessage("Our AI detected that the live selfie does not match the face on the ID card. \n\nConfidence: " + String.format("%.1f", score) + "% (Requires 80%)")
                .setPositiveButton("Try Again", (dialog, which) -> {
                    resetScanButton();
                    capturedImageUri = null;
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    private void resetScanButton() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                String startText = "Start Scan";
                btnStartScan.setText(startText);
                btnStartScan.setEnabled(true);
                btnStartScan.setAlpha(1.0f);

                if (getContext() != null) {
                    TranslationHelper.autoTranslate(getContext(), btnStartScan, startText);
                }
            });
        }
    }

    private boolean isContentValid(String rawText, String idType) {
        String text = rawText.toUpperCase();

        if (idType.equals("NATIONAL_ID")) {
            return text.contains("PAMBANSANG") || text.contains("PAGKAKAKILANLAN") || text.contains("PHILIPPINE IDENTIFICATION") || text.contains("PHILID");
        } else if (idType.equals("DRIVERS_LICENSE")) {
            return text.contains("DRIVER") || text.contains("LICENSE") || text.contains("TRANSPORTATION") || text.contains("LTO");
        } else {
            return text.contains("QCITIZEN") || text.contains("QUEZON CITY") || text.contains("LUNGSOD QUEZON") || text.contains("CITIZEN CARD") || text.contains("KASAMA KA");
        }
    }

    private void showInvalidIdDialog(String idType) {
        String expectedName = "Quezon City ID";
        if (idType.equals("NATIONAL_ID")) expectedName = "National ID (PhilSys)";
        if (idType.equals("DRIVERS_LICENSE")) expectedName = "Driver's License";

        new AlertDialog.Builder(getContext())
                .setTitle("Incorrect ID Type")
                .setMessage("The scanned image does not appear to be a valid " + expectedName + ".\n\nPlease ensure you captured a clear photo of the correct document.")
                .setPositiveButton("Try Again", (dialog, which) -> {
                    resetScanButton();
                    capturedImageUri = null;
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
        args.putString("EXTRACTED_ADDRESS", extractAddress);

        RegistrationCache.extractedAddress = extractAddress;

        if (capturedImageUri != null) args.putString("ID_IMAGE_URI", capturedImageUri.toString());

        step1.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_signup, step1)
                .addToBackStack(null)
                .commit();
    }

    // =========================================================================
    // 🔍 TEXT EXTRACTION PARSERS (Your Original Code)
    // =========================================================================

    private void parseNationalID(String[] lines) {
        extractFName = ""; extractLName = ""; extractMName = ""; extractAddress = "";

        boolean nextIsLast = false, nextIsFirst = false, nextIsMiddle = false, readingAddress = false;
        StringBuilder addressBuilder = new StringBuilder();

        for (String line : lines) {
            String upper = line.toUpperCase().trim();
            if (upper.isEmpty()) continue;

            if (nextIsLast) { extractLName = cleanText(upper); nextIsLast = false; continue; }
            if (nextIsFirst) { extractFName = cleanText(upper); nextIsFirst = false; continue; }
            if (nextIsMiddle) { extractMName = cleanText(upper); nextIsMiddle = false; continue; }

            if (upper.contains("MIDDLE NAME") || upper.contains("GITNANG APELYIDO") || upper.contains("GITNANG")) {
                String inline = upper.replace("GITNANG APELYIDO", "").replace("MIDDLE NAME", "").replace("/", "").replace(":", "").trim();
                if (inline.length() > 1) extractMName = cleanText(inline);
                else nextIsMiddle = true;
                continue;
            }

            if (upper.contains("LAST NAME") || upper.contains("APELYIDO")) {
                String inline = upper.replace("APELYIDO", "").replace("LAST NAME", "").replace("/", "").replace(":", "").trim();
                if (inline.length() > 1) extractLName = cleanText(inline);
                else nextIsLast = true;
                continue;
            }

            if (upper.contains("GIVEN NAME") || upper.contains("MGA PANGALAN") || upper.contains("PANGALAN")) {
                String inline = upper.replace("MGA PANGALAN", "").replace("PANGALAN", "").replace("GIVEN NAMES", "").replace("GIVEN NAME", "").replace("/", "").replace(":", "").trim();
                if (inline.length() > 1) extractFName = cleanText(inline);
                else nextIsFirst = true;
                continue;
            }

            if (upper.contains("TIRAHAN") || upper.contains("ADDRESS")) {
                readingAddress = true;
                int idx = Math.max(upper.indexOf("TIRAHAN"), upper.indexOf("ADDRESS"));
                String inlineAddr = upper.substring(idx)
                        .replace("TIRAHAN/ADDRESS", "")
                        .replace("TIRAHAN", "")
                        .replace("ADDRESS", "")
                        .replace(":", "").trim();
                if (!inlineAddr.isEmpty()) {
                    addressBuilder.append(inlineAddr).append(" ");
                }
                continue;
            }

            if (readingAddress) {
                if (!upper.contains("BLOOD") && !upper.contains("PHILHEALTH") && upper.length() > 3) {
                    addressBuilder.append(upper).append(" ");
                }
            }
        }
        extractAddress = cleanText(addressBuilder.toString());
    }

    private void parseDriversLicense(String[] lines) {
        extractFName = ""; extractLName = ""; extractMName = ""; extractAddress = "";

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.toUpperCase().trim()).append(" ");
        }
        String fullCard = sb.toString().replaceAll("\\s+", " ");

        int idxMidName = fullCard.indexOf("MIDDLE NAME");
        int idxNat = fullCard.indexOf("NATIONALITY");
        int idxSex = fullCard.indexOf("SEX");

        int endNameIdx = (idxNat != -1) ? idxNat : idxSex;

        if (idxMidName != -1 && endNameIdx != -1 && idxMidName < endNameIdx) {
            String rawName = fullCard.substring(idxMidName + 11, endNameIdx).trim();
            extractNameFromString(rawName);
        }

        int startIdx = -1;
        int endIdx = fullCard.length();

        Matcher dobMatcher = Pattern.compile("\\b(19|20)\\d{2}/\\d{2}/\\d{2}\\b").matcher(fullCard);
        if (dobMatcher.find()) {
            startIdx = dobMatcher.end();
        } else if (fullCard.indexOf("ADDRESS") != -1) {
            startIdx = fullCard.indexOf("ADDRESS") + 7;
        }

        Matcher licMatcher = Pattern.compile("\\b[A-Z]\\d{2}-\\d{2}\\b").matcher(fullCard);
        if (licMatcher.find()) {
            endIdx = licMatcher.start();
        } else if (fullCard.indexOf("LICENSE NO") != -1) {
            endIdx = fullCard.indexOf("LICENSE NO");
        } else if (fullCard.indexOf("EXPIRATION") != -1) {
            endIdx = fullCard.indexOf("EXPIRATION");
        }

        if (startIdx != -1 && startIdx < endIdx) {
            String rawAddress = fullCard.substring(startIdx, endIdx);

            rawAddress = rawAddress.replaceAll("ADDRESS", " ");
            rawAddress = rawAddress.replaceAll("WEIGHT", " ");
            rawAddress = rawAddress.replaceAll("HEIGHT", " ");
            rawAddress = rawAddress.replaceAll("\\(KG\\)", " ");
            rawAddress = rawAddress.replaceAll("\\(M\\)", " ");
            rawAddress = rawAddress.replaceAll("\\b\\d\\.\\d{2}\\b", " ");

            extractAddress = cleanText(rawAddress);
            extractAddress = extractAddress.replaceFirst("^\\d{2}\\s+", "");
        }

        if (extractAddress.isEmpty() || extractAddress.length() < 10) {
            Pattern philPattern = Pattern.compile("(\\d{1,4}\\s+[A-Z0-9\\s,.-]+?PHILIPPINES)\\b");
            Matcher m = philPattern.matcher(fullCard);
            if (m.find()) {
                extractAddress = cleanText(m.group(1));
            }
        }
    }

    private void extractNameFromString(String text) {
        String[] parts = text.split(",");
        if (parts.length >= 3) {
            extractLName = cleanText(parts[0]);
            extractFName = cleanText(parts[1]);
            extractMName = cleanText(parts[2]);
        } else {
            String[] words = text.split(" ");
            if (words.length >= 3) {
                extractLName = cleanText(words[0]);
                extractMName = cleanText(words[words.length - 1]);
                extractFName = cleanText(text.replace(words[0], "").replace(words[words.length - 1], "").trim());
            } else if (words.length == 2) {
                extractLName = cleanText(words[0]);
                extractFName = cleanText(words[1]);
            } else {
                extractLName = cleanText(text);
            }
        }
    }

    private void parseQCID(String[] lines) {
        extractFName = ""; extractLName = ""; extractMName = ""; extractAddress = "";
        boolean expectName = false;
        StringBuilder rawBlocks = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toUpperCase()
                    .replaceAll("PREVIEW ONLY", "")
                    .replaceAll("PREVIEW", "")
                    .replaceAll("VIEW ONLY", "")
                    .trim();

            if (line.isEmpty()) continue;

            rawBlocks.append(line).append(" ");

            if (line.contains("LAST NAME") && (line.contains("FIRST NAME") || line.contains("M.I.") || line.contains("MIDDLE"))) {
                expectName = true;
                continue;
            }

            if (expectName && line.contains(",")) {
                parseCommaSeparatedName(line);
                expectName = false;
            }
            else if (extractLName.isEmpty() && line.contains(",") && !line.contains("EMERGENCY") && !line.matches(".*\\d.*") && !line.contains("QUEZON CITY")) {
                parseCommaSeparatedName(line);
            }
        }

        extractAddress = extractCleanAddress(rawBlocks.toString());
    }

    private String extractCleanAddress(String fullCardText) {
        String cleanText = fullCardText.toUpperCase()
                .replaceAll("SINGLE", " ")
                .replaceAll("MARRIED", " ")
                .replaceAll("WIDOWED", " ");

        cleanText = cleanText.replaceAll("\\d{4}/\\d{2}/\\d{2}", " ");
        cleanText = cleanText.replaceAll("20\\d{5}\\s*\\d*", " ");

        Pattern addressStartPattern = Pattern.compile("(?<!\\d)\\d{1,4}\\s*[A-Z]?\\s+[A-ZÑ]+");
        Matcher startMatcher = addressStartPattern.matcher(cleanText);

        String potentialAddress = "";
        if (startMatcher.find()) {
            potentialAddress = cleanText.substring(startMatcher.start());
        } else {
            return "";
        }

        String[] stopWords = {"IN CASE", "EMERGENCY", "CESE", "SERGENC", "CONTACT", "CARDHOLDER", "RESIDENT", "09"};
        int earliestStopIndex = potentialAddress.length();

        for (String stopWord : stopWords) {
            int index = potentialAddress.indexOf(stopWord);
            if (index != -1 && index < earliestStopIndex) {
                earliestStopIndex = index;
            }
        }

        Pattern numbersPattern = Pattern.compile("\\d{5,}");
        Matcher numberMatcher = numbersPattern.matcher(potentialAddress);
        if (numberMatcher.find()) {
            if (numberMatcher.start() < earliestStopIndex) {
                earliestStopIndex = numberMatcher.start();
            }
        }

        potentialAddress = potentialAddress.substring(0, earliestStopIndex);
        potentialAddress = potentialAddress.replace("QUEZON TO", "QUEZON CITY");

        return potentialAddress.replaceAll("[^a-zA-Z0-9 Ññ.,-]", " ").replaceAll("\\s+", " ").trim();
    }

    private void parseCommaSeparatedName(String fullText) {
        if (!fullText.contains(",")) return;
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
            extractMName = cleanText(parts[parts.length - 1]);
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
                .replaceAll("[^a-zA-Z0-9 Ññ.,\\-/#]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}