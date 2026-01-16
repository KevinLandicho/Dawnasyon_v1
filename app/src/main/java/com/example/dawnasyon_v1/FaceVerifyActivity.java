package com.example.dawnasyon_v1;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceVerifyActivity extends AppCompatActivity {

    private PreviewView previewView;
    private FaceOverlayView faceOverlay;
    private TextView tvStatus;
    private ImageCapture imageCapture;
    private FaceHelper faceHelper;
    private ExecutorService cameraExecutor;

    private boolean isCapturing = false;
    private boolean isAnalyzing = false;
    private FaceDetector detector;

    private enum SecurityStep { ALIGN, LOOK_LEFT, LOOK_RIGHT, LOOK_UP, VERIFYING }
    private SecurityStep currentStep = SecurityStep.ALIGN;
    private int stabilityCounter = 0;

    private String userType = "Resident";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scan);

        previewView = findViewById(R.id.cameraPreview);
        faceOverlay = findViewById(R.id.faceOverlay);
        tvStatus = findViewById(R.id.tvStatus);

        // ⭐ SET VERIFICATION MODE (Dynamic Box)
        // Ensures we don't see the static hole punch here
        faceOverlay.setRegistrationMode(false);

        faceHelper = new FaceHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize Face Detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        detector = FaceDetection.getClient(options);

        // ⭐ KEY CHANGE: DO NOT START CAMERA HERE.
        // We strictly check permissions first, then check the User Type.
        if (allPermissionsGranted()) {
            checkUserTypeAndStart();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }
    }

    // ⭐ STEP 1: CHECK DATABASE BEFORE OPENING CAMERA
    private void checkUserTypeAndStart() {
        runOnUiThread(() -> {
            tvStatus.setText("Checking profile...");
            // Hide camera preview while checking to avoid confusion
            previewView.setVisibility(View.INVISIBLE);
        });

        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null) {
                String type = profile.getType(); // Should return "Foreign" or "Resident"

                // Save to cache for future speed
                getSharedPreferences("UserSession", MODE_PRIVATE)
                        .edit().putString("user_type", type).apply();

                handleUserType(type);
            } else {
                // If fetch fails (offline?), check local cache
                String cachedType = getSharedPreferences("UserSession", MODE_PRIVATE)
                        .getString("user_type", "Resident");
                handleUserType(cachedType);
            }
            return null;
        });
    }

    // ⭐ STEP 2: DECIDE - SKIP OR START
    private void handleUserType(String type) {
        userType = type;

        boolean isForeign = (type != null && (type.equalsIgnoreCase("Foreign") || type.equalsIgnoreCase("Overseas")));

        if (isForeign) {
            // ⭐ BYPASS: Go directly to Main Activity
            runOnUiThread(() -> {
                Toast.makeText(FaceVerifyActivity.this, "Welcome Foreign Donor!", Toast.LENGTH_SHORT).show();

                // Mark Verified
                getSharedPreferences("UserSession", MODE_PRIVATE)
                        .edit().putLong("last_verified_timestamp", System.currentTimeMillis()).apply();

                startActivity(new Intent(FaceVerifyActivity.this, MainActivity.class));
                finish();
            });
        } else {
            // ⭐ RESIDENT: Now we can start the camera
            runOnUiThread(() -> {
                previewView.setVisibility(View.VISIBLE); // Show camera again
                startCamera();
            });
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480))
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

                runOnUiThread(() -> tvStatus.setText("Center Your Face"));

            } catch (Exception e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(ImageProxy imageProxy) {
        if (isAnalyzing || isCapturing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            isAnalyzing = true;
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() == 1) {
                            Face face = faces.get(0);
                            runOnUiThread(() -> faceOverlay.updateFace(face, imageProxy.getWidth(), imageProxy.getHeight()));
                            processGestures(face, imageProxy.getWidth(), imageProxy.getHeight());
                        } else {
                            runOnUiThread(() -> {
                                faceOverlay.updateFace(null, 0, 0);
                                resetToStart("No Face Detected");
                            });
                        }
                    })
                    .addOnFailureListener(e -> { })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                        isAnalyzing = false;
                    });
        } else {
            imageProxy.close();
            isAnalyzing = false;
        }
    }

    private float calculateYaw(Face face) {
        Rect box = face.getBoundingBox();
        FaceContour nose = face.getContour(FaceContour.NOSE_BRIDGE);
        if (nose == null || nose.getPoints().isEmpty()) return 0;
        float noseX = nose.getPoints().get(0).x;
        float ratio = (noseX - box.left) / (float)box.width();
        return (ratio - 0.5f) * 100;
    }

    private float calculatePitch(Face face) {
        Rect box = face.getBoundingBox();
        FaceContour noseBottom = face.getContour(FaceContour.NOSE_BOTTOM);
        if (noseBottom == null || noseBottom.getPoints().isEmpty()) return 0;
        float noseY = noseBottom.getPoints().get(0).y;
        float ratio = (noseY - box.top) / (float)box.height();
        float diff = 0.60f - ratio;
        return diff * 100;
    }

    private void processGestures(Face face, int w, int h) {
        // Redundant safety check (Main logic is in handleUserType)
        if (userType != null && userType.equalsIgnoreCase("Foreign")) {
            currentStep = SecurityStep.VERIFYING;
        }

        float rotY = calculateYaw(face);
        float rotX = calculatePitch(face);
        String debug = String.format("\nY: %.1f | X: %.1f", rotY, rotX);

        switch (currentStep) {
            case ALIGN:
                if (Math.abs(rotY) < 10 && Math.abs(rotX) < 10) {
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.LOOK_LEFT;
                        stabilityCounter = 0;
                    }
                    updateStatus("Hold Still..." + debug, Color.GREEN);
                } else {
                    updateStatus("Center Your Face" + debug, Color.WHITE);
                    stabilityCounter = 0;
                }
                break;

            case LOOK_LEFT:
                if (rotY > 12) {
                    updateStatus("Good! Hold Left..." + stabilityCounter, Color.GREEN);
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.LOOK_RIGHT;
                        stabilityCounter = 0;
                    }
                } else if (rotY < -10) {
                    updateStatus("Wrong Way! Turn LEFT ⬅️" + debug, Color.RED);
                } else {
                    updateStatus("Turn Head LEFT ⬅️" + debug, Color.CYAN);
                }
                break;

            case LOOK_RIGHT:
                if (rotY < -12) {
                    updateStatus("Good! Hold Right..." + stabilityCounter, Color.GREEN);
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.LOOK_UP;
                        stabilityCounter = 0;
                    }
                } else if (rotY > 10) {
                    updateStatus("Wrong Way! Turn RIGHT ➡️" + debug, Color.RED);
                } else {
                    updateStatus("Turn Head RIGHT ➡️" + debug, Color.CYAN);
                }
                break;

            case LOOK_UP:
                if (rotX > 4) {
                    updateStatus("Good! Hold Up...", Color.GREEN);
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.VERIFYING;
                        runOnUiThread(() -> faceOverlay.setBorderColor(Color.GREEN));
                        stabilityCounter = 0;
                    }
                } else {
                    updateStatus("Look UP ⬆️" + debug, Color.CYAN);
                }
                break;

            case VERIFYING:
                updateStatus("Look Straight!", Color.GREEN);
                if (Math.abs(rotY) < 10) {
                    stabilityCounter++;
                    if (stabilityCounter > 3) triggerCapture();
                }
                break;
        }
    }

    private void resetToStart(String msg) {
        if (currentStep != SecurityStep.ALIGN) {
            currentStep = SecurityStep.ALIGN;
            stabilityCounter = 0;
            updateStatus(msg, Color.WHITE);
            runOnUiThread(() -> faceOverlay.setBorderColor(Color.CYAN));
        }
    }

    private void updateStatus(String msg, int color) {
        runOnUiThread(() -> {
            tvStatus.setText(msg);
            tvStatus.setTextColor(color);
        });
    }

    private void triggerCapture() {
        if (isCapturing) return;
        isCapturing = true;
        runOnUiThread(() -> tvStatus.setText("Verifying Identity..."));

        if (imageCapture == null) return;
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();
                faceHelper.scanFace(bitmap, new FaceHelper.FaceCallback() {
                    @Override
                    public void onFaceDetected(float[] newEmbedding) {
                        checkMatch(newEmbedding);
                    }
                    @Override
                    public void onError(String error) {
                        isCapturing = false;
                        resetToStart("Error: " + error);
                    }
                });
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
                resetToStart("Capture Failed");
            }
        });
    }

    private void checkMatch(float[] newEmbedding) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedData = prefs.getString("face_embedding", "");

        if (savedData.isEmpty()) {
            Toast.makeText(this, "No registered face found.", Toast.LENGTH_LONG).show();
            isCapturing = false;
            return;
        }

        String[] split = savedData.split(",");
        float[] savedEmbedding = new float[split.length];
        for (int i = 0; i < split.length; i++) savedEmbedding[i] = Float.parseFloat(split[i]);

        float score = FaceHelper.compareFaces(newEmbedding, savedEmbedding);

        if (score > 0.60f) {
            Toast.makeText(this, "✅ Welcome!", Toast.LENGTH_SHORT).show();
            prefs.edit().putLong("last_verified_timestamp", System.currentTimeMillis()).apply();
            startActivity(new Intent(FaceVerifyActivity.this, MainActivity.class));
            finish();
        } else {
            isCapturing = false;
            resetToStart("❌ Face Mismatch");
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (allPermissionsGranted()) {
                checkUserTypeAndStart();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}