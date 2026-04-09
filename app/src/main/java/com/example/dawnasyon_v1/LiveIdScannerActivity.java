package com.example.dawnasyon_v1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
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
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveIdScannerActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvInstruction;
    private TextView tvStatus;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private boolean isCapturing = false;
    private int alignCounter = 0;
    private static final int ALIGN_THRESHOLD = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using the XML you already created
        setContentView(R.layout.activity_live_id_scanner);

        previewView = findViewById(R.id.cameraPreview);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvStatus = findViewById(R.id.tvStatus);

        tvInstruction.setText("Align face within the frame");

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

                Preview preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                // ⭐ FRONT CAMERA FOR SELFIES
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
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            int imageWidth = (rotation == 90 || rotation == 270) ? imageProxy.getHeight() : imageProxy.getWidth();
            int imageHeight = (rotation == 90 || rotation == 270) ? imageProxy.getWidth() : imageProxy.getHeight();

            InputImage image = InputImage.fromMediaImage(mediaImage, rotation);

            FaceDetector detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build());

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() == 1) {
                            checkAlignment(faces.get(0).getBoundingBox(), imageWidth, imageHeight);
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

        boolean centeredX = faceCenterX > 0.25 && faceCenterX < 0.75;
        boolean centeredY = faceCenterY > 0.20 && faceCenterY < 0.80;

        float faceRatio = (float) faceBox.width() / frameW;
        boolean isCorrectDistance = faceRatio > 0.20 && faceRatio < 0.85;

        if (centeredX && centeredY && isCorrectDistance) {
            alignCounter++;
            runOnUiThread(() -> {
                int progress = (int) (((float) alignCounter / ALIGN_THRESHOLD) * 100);
                tvStatus.setText("Verifying... " + progress + "%");
                tvStatus.setTextColor(Color.GREEN);
            });

            if (alignCounter >= ALIGN_THRESHOLD && !isCapturing) {
                isCapturing = true;
                captureAndReturnImage();
            }
        } else {
            if (!isCorrectDistance) {
                if (faceRatio < 0.20) resetAlignment("Move a bit closer");
                else resetAlignment("Move further back");
            } else {
                resetAlignment("Align face in the center");
            }
        }
    }

    private void resetAlignment(String msg) {
        alignCounter = 0;
        runOnUiThread(() -> {
            tvStatus.setText(msg);
            tvStatus.setTextColor(Color.WHITE);
        });
    }

    private void captureAndReturnImage() {
        runOnUiThread(() -> tvStatus.setText("Capturing Selfie..."));

        File photoFile = new File(getCacheDir(), "live_selfie_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Return the URI of the newly taken selfie back to the Fragment
                Intent resultIntent = new Intent();
                resultIntent.putExtra("SELFIE_URI", Uri.fromFile(photoFile).toString());
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
                resetAlignment("Camera Error");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}