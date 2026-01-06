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

    // ⭐ GESTURE CHALLENGE VARIABLES
    private enum SecurityStep {
        ALIGN,
        LOOK_LEFT,
        LOOK_RIGHT,
        LOOK_UP,
        VERIFYING
    }
    private SecurityStep currentStep = SecurityStep.ALIGN;
    private int stabilityCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scan);

        previewView = findViewById(R.id.cameraPreview);
        faceOverlay = findViewById(R.id.faceOverlay);
        tvStatus = findViewById(R.id.tvStatus);

        faceHelper = new FaceHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
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

                // Low latency analysis for smooth gesture tracking
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480))
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
            } catch (Exception e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(ImageProxy imageProxy) {
        if (isCapturing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // We don't need classification (eyes) anymore, just landmarks/contours
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .build();
            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() == 1) {
                            processGestures(faces.get(0), imageProxy.getWidth(), imageProxy.getHeight());
                        } else {
                            resetToStart("No Face / Too Many Faces");
                        }
                    })
                    .addOnFailureListener(e -> resetToStart("Error"))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    // ⭐ NEW GESTURE LOGIC LOOP
    private void processGestures(Face face, int w, int h) {
        // 1. Get Head Angles
        float rotY = face.getHeadEulerAngleY(); // Left/Right (Yaw)
        float rotX = face.getHeadEulerAngleX(); // Up/Down (Pitch)

        // 2. Check Alignment (Is face in box?)
        Rect box = face.getBoundingBox();
        float centerX = box.centerX() / (float) w;
        float centerY = box.centerY() / (float) h;
        boolean aligned = centerX > 0.35 && centerX < 0.65 && centerY > 0.35 && centerY < 0.65;

        // 3. State Machine
        switch (currentStep) {
            case ALIGN:
                if (aligned && Math.abs(rotY) < 10 && Math.abs(rotX) < 10) {
                    // Face is center and looking straight
                    stabilityCounter++;
                    if (stabilityCounter > 10) {
                        currentStep = SecurityStep.LOOK_LEFT;
                        updateStatus("Slowly Turn Head LEFT ⬅️", Color.CYAN);
                        stabilityCounter = 0;
                    } else {
                        updateStatus("Hold Still...", Color.WHITE);
                    }
                } else {
                    updateStatus("Center Your Face", Color.WHITE);
                    stabilityCounter = 0;
                }
                break;

            case LOOK_LEFT:
                // ML Kit: Positive Y is usually Left (depends on camera mirror, check < -20 or > 20)
                // Let's assume Turn Left means Angle Y becomes > 20 degrees
                if (rotY > 20) {
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.LOOK_RIGHT;
                        updateStatus("Now Turn Head RIGHT ➡️", Color.CYAN);
                        stabilityCounter = 0;
                    }
                }
                break;

            case LOOK_RIGHT:
                // Turn Right means Angle Y becomes < -20 degrees
                if (rotY < -20) {
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.LOOK_UP;
                        updateStatus("Now Look UP ⬆️", Color.CYAN);
                        stabilityCounter = 0;
                    }
                }
                break;

            case LOOK_UP:
                // Look Up means Angle X becomes > 15 degrees
                if (rotX > 15) {
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        currentStep = SecurityStep.VERIFYING;
                        updateStatus("Look Straight to Finish!", Color.GREEN);
                        faceOverlay.setBorderColor(Color.GREEN);
                        stabilityCounter = 0;
                    }
                }
                break;

            case VERIFYING:
                // User must look straight again to take the photo
                if (Math.abs(rotY) < 10 && Math.abs(rotX) < 10) {
                    stabilityCounter++;
                    if (stabilityCounter > 5) {
                        triggerCapture();
                    }
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
        runOnUiThread(() -> {
            tvStatus.setText("Verifying Identity...");
            captureAndVerify();
        });
    }

    private void captureAndVerify() {
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
                        resetToStart("Retry: " + error);
                    }
                });
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
            }
        });
    }

    private void checkMatch(float[] newEmbedding) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedData = prefs.getString("face_embedding", "");

        if (savedData.isEmpty()) {
            Toast.makeText(this, "No registered face found.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] split = savedData.split(",");
        float[] savedEmbedding = new float[split.length];
        for (int i = 0; i < split.length; i++) savedEmbedding[i] = Float.parseFloat(split[i]);

        float score = FaceHelper.compareFaces(newEmbedding, savedEmbedding);

        if (score > 0.8f) {
            Toast.makeText(this, "✅ Identity Verified!", Toast.LENGTH_SHORT).show();
            prefs.edit().putLong("last_verified_timestamp", System.currentTimeMillis()).apply();
            startActivity(new Intent(FaceVerifyActivity.this, MainActivity.class));
            finish();
        } else {
            isCapturing = false;
            Toast.makeText(this, "❌ Mismatch. Try again.", Toast.LENGTH_SHORT).show();
            resetToStart("Identity Mismatch");
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Verification Required", Toast.LENGTH_SHORT).show();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}