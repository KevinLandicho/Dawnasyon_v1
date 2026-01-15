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

    // Flags
    private boolean isCapturing = false; // Prevents double snapshots

    // Auto-Capture Counters
    private int alignCounter = 0;
    private static final int ALIGN_THRESHOLD = 15; // Approx 0.5 - 1.0 second of stability

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ USES YOUR NEW DEDICATED LAYOUT
        setContentView(R.layout.activity_face_register);

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

                // 1. Preview (The visual feed)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. Image Capture (High Res for the actual saving)
                imageCapture = new ImageCapture.Builder().build();

                // 3. Image Analysis (Low Res for fast detection logic)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480))
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ⭐ LIVE ANALYZER: Checks every frame to see if face is aligned
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(ImageProxy imageProxy) {
        if (isCapturing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // Lightweight detector for alignment check
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
                            resetAlignment("No Face Detected");
                        }
                    })
                    .addOnFailureListener(e -> resetAlignment("Detection Error"))
                    .addOnCompleteListener(task -> imageProxy.close()); // Essential: Close proxy!
        } else {
            imageProxy.close();
        }
    }

    // ⭐ ALIGNMENT LOGIC: Is the face inside the overlay hole?
    private void checkAlignment(Rect faceBox, int frameW, int frameH) {
        // Calculate the center point of the face (0.0 to 1.0)
        float faceCenterX = faceBox.centerX() / (float) frameW;
        float faceCenterY = faceBox.centerY() / (float) frameH;

        // 1. Check Centering (Must be within the middle 30% of screen)
        // Since camera logic is often rotated, X and Y might be swapped depending on orientation,
        // but typically 0.35 - 0.65 covers the center well.
        boolean centeredX = faceCenterX > 0.35 && faceCenterX < 0.65;
        boolean centeredY = faceCenterY > 0.35 && faceCenterY < 0.65;

        // 2. Check Size (Must not be too far away)
        float faceRatio = (float) faceBox.width() / frameW;
        boolean bigEnough = faceRatio > 0.25;

        if (centeredX && centeredY && bigEnough) {
            alignCounter++;

            // VISUAL FEEDBACK: Turn Green
            runOnUiThread(() -> {
                faceOverlay.setBorderColor(Color.GREEN);
                tvStatus.setText("Hold still... " + (ALIGN_THRESHOLD - alignCounter));
                tvStatus.setTextColor(Color.GREEN);
            });

            // TRIGGER CAPTURE
            if (alignCounter >= ALIGN_THRESHOLD && !isCapturing) {
                isCapturing = true;
                runOnUiThread(() -> captureAndRegister());
            }
        } else {
            resetAlignment("Align Face in Center");
        }
    }

    private void resetAlignment(String msg) {
        alignCounter = 0;
        runOnUiThread(() -> {
            faceOverlay.setBorderColor(Color.CYAN); // Default color
            tvStatus.setText(msg);
            tvStatus.setTextColor(Color.WHITE);
        });
    }

    // ⭐ FINAL CAPTURE
    private void captureAndRegister() {
        runOnUiThread(() -> {
            tvStatus.setText("Scanning...");
            Toast.makeText(this, "Capturing...", Toast.LENGTH_SHORT).show();
        });

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                // Convert to Bitmap
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                // Send to FaceHelper for Embedding extraction
                faceHelper.scanFace(bitmap, new FaceHelper.FaceCallback() {
                    @Override
                    public void onFaceDetected(float[] embedding) {
                        saveFaceData(embedding);
                    }
                    @Override
                    public void onError(String error) {
                        isCapturing = false; // Allow retry
                        resetAlignment("Scan Failed: " + error);
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
                resetAlignment("Camera Error");
            }
        });
    }

    // Save the vector data to pass back to previous screen
    private void saveFaceData(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (float f : embedding) sb.append(f).append(",");

        // Ideally save to a Singleton or Static variable if passing large data
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