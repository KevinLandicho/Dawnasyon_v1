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
    private FaceRegisterOverlayView faceOverlay;
    private TextView tvStatus;
    private ImageCapture imageCapture;
    private FaceHelper faceHelper;
    private ExecutorService cameraExecutor;

    private boolean isCapturing = false;
    private int alignCounter = 0;

    // ⭐ Threshold set to 10 for a quick 1-second capture
    private static final int ALIGN_THRESHOLD = 10;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register);

        previewView = findViewById(R.id.cameraPreview);
        faceOverlay = findViewById(R.id.faceOverlay);
        tvStatus = findViewById(R.id.tvStatus);

        faceHelper = new FaceHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
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
                        .setTargetResolution(new Size(480, 640))
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
            FaceDetector detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build());

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() == 1) {
                            checkAlignment(faces.get(0).getBoundingBox(), image.getWidth(), image.getHeight());
                        } else if (faces.size() > 1) {
                            resetAlignment("Please scan alone");
                        } else {
                            resetAlignment("Face not detected");
                        }
                    })
                    .addOnFailureListener(e -> resetAlignment("Scanning..."))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else { imageProxy.close(); }
    }

    private void checkAlignment(Rect faceBox, int frameW, int frameH) {
        float faceCenterX = faceBox.centerX() / (float) frameW;
        float faceCenterY = faceBox.centerY() / (float) frameH;

        // ⭐ RELAXED BOUNDARIES: Widened from 0.4-0.6 to 0.35-0.65 for easier use
        boolean centeredX = faceCenterX > 0.35 && faceCenterX < 0.65;
        boolean centeredY = faceCenterY > 0.30 && faceCenterY < 0.70;

        float faceRatio = (float) faceBox.width() / frameW;
        boolean isCorrectDistance = faceRatio > 0.30 && faceRatio < 0.80;

        if (centeredX && centeredY && isCorrectDistance) {
            alignCounter++;
            runOnUiThread(() -> {
                faceOverlay.setBorderColor(Color.GREEN);
                int progress = (int) (((float) alignCounter / ALIGN_THRESHOLD) * 100);
                tvStatus.setText("Scanning... " + progress + "%");
                tvStatus.setTextColor(Color.GREEN);
            });

            if (alignCounter >= ALIGN_THRESHOLD && !isCapturing) {
                isCapturing = true;
                captureAndRegister();
            }
        } else {
            // ⭐ SPECIFIC FEEDBACK
            if (!isCorrectDistance) {
                if (faceRatio < 0.30) resetAlignment("Move a bit closer");
                else resetAlignment("Move further back");
            } else {
                resetAlignment("Align face in the center");
            }
        }
    }

    private void resetAlignment(String msg) {
        alignCounter = 0;
        runOnUiThread(() -> {
            faceOverlay.setBorderColor(Color.CYAN);
            tvStatus.setText(msg);
            tvStatus.setTextColor(Color.WHITE);
        });
    }

    private void captureAndRegister() {
        runOnUiThread(() -> tvStatus.setText("Processing..."));

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();
                faceHelper.scanFace(bitmap, new FaceHelper.FaceCallback() {
                    @Override
                    public void onFaceDetected(float[] embedding) { saveFaceData(embedding); }
                    @Override
                    public void onError(String error) {
                        isCapturing = false;
                        resetAlignment("Try again");
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

    private void saveFaceData(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (float f : embedding) sb.append(f).append(",");
        RegistrationCache.faceEmbedding = sb.toString();
        setResult(RESULT_OK, new Intent());
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