package com.example.dawnasyon_v1;

import android.Manifest;
import android.content.Intent;
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

public class FaceRegisterActivity extends AppCompatActivity {

    private PreviewView previewView;
    private FaceOverlayView faceOverlay;
    private TextView tvStatus;
    private ImageCapture imageCapture;
    private FaceHelper faceHelper;
    private ExecutorService cameraExecutor;
    private boolean isCapturing = false; // Prevent double capture

    // Auto-Capture logic vars
    private int alignCounter = 0;
    private static final int ALIGN_THRESHOLD = 15; // Require ~0.5 seconds of stable alignment

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

                // 1. Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. Image Capture (High Quality for saving)
                imageCapture = new ImageCapture.Builder().build();

                // 3. Image Analysis (Live stream for alignment check)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480)) // Low res for fast detection
                        .build();

                // Set up the Live Analyzer
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ⭐ LIVE FRAME ANALYZER
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(ImageProxy imageProxy) {
        if (isCapturing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // Use a separate lightweight detector just for alignment
            FaceDetector detector = FaceDetection.getClient(
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                            .build()
            );

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() == 1) {
                            Face face = faces.get(0);
                            checkAlignment(face.getBoundingBox(), imageProxy.getWidth(), imageProxy.getHeight());
                        } else {
                            resetAlignment("No face / Too many faces");
                        }
                    })
                    .addOnFailureListener(e -> resetAlignment("Error"))
                    .addOnCompleteListener(task -> imageProxy.close()); // Must close!
        } else {
            imageProxy.close();
        }
    }

    // ⭐ ALIGNMENT LOGIC
    private void checkAlignment(Rect faceBox, int frameW, int frameH) {
        // The overlay coordinates are in View pixels, faceBox is in Camera pixels.
        // For simplicity in a vertical selfie, we check rough centering.

        // Calculate center of face (0.0 to 1.0)
        float faceCenterX = faceBox.centerX() / (float) frameW;
        float faceCenterY = faceBox.centerY() / (float) frameH;

        // Tolerances (Is it roughly in the middle?)
        boolean centeredX = faceCenterX > 0.35 && faceCenterX < 0.65;
        boolean centeredY = faceCenterY > 0.35 && faceCenterY < 0.65;

        // Is it big enough? (Not too far away)
        float faceRatio = (float) faceBox.width() / frameW;
        boolean bigEnough = faceRatio > 0.25;

        if (centeredX && centeredY && bigEnough) {
            alignCounter++;

            // UI Feedback
            runOnUiThread(() -> {
                faceOverlay.setBorderColor(Color.GREEN); // Turn Tech lines GREEN
                tvStatus.setText("Hold still... " + (ALIGN_THRESHOLD - alignCounter));
                tvStatus.setTextColor(Color.GREEN);
            });

            // TRIGGER CAPTURE
            if (alignCounter >= ALIGN_THRESHOLD && !isCapturing) {
                isCapturing = true;
                runOnUiThread(() -> captureAndRegister());
            }
        } else {
            resetAlignment("Align Face inside Box");
        }
    }

    private void resetAlignment(String msg) {
        alignCounter = 0;
        runOnUiThread(() -> {
            faceOverlay.setBorderColor(Color.CYAN); // Reset to Cyan
            tvStatus.setText(msg);
            tvStatus.setTextColor(Color.WHITE);
        });
    }

    // ⭐ FINAL CAPTURE (Same as before, but called automatically)
    private void captureAndRegister() {
        runOnUiThread(() -> Toast.makeText(this, "Capturing...", Toast.LENGTH_SHORT).show());

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                faceHelper.scanFace(bitmap, new FaceHelper.FaceCallback() {
                    @Override
                    public void onFaceDetected(float[] embedding) {
                        saveFaceData(embedding);
                    }
                    @Override
                    public void onError(String error) {
                        isCapturing = false; // Allow retry
                        Toast.makeText(FaceRegisterActivity.this, "Scan Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
            }
        });
    }

    private void saveFaceData(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (float f : embedding) sb.append(f).append(",");
        RegistrationCache.faceEmbedding = sb.toString();

        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}