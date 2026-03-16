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

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceRegisterActivity extends AppCompatActivity {

    private PreviewView previewView;
    private FaceRegisterOverlayView faceOverlay;
    private TextView tvStatus;
    private ImageCapture imageCapture;
    private FaceHelper faceHelper;
    private ExecutorService cameraExecutor;

    private boolean isCapturing = false;
    private int alignCounter = 0;

    private String followUpUserId = null; // ⭐ Added to track if this is a follow-up registration

    // ⭐ Threshold set to 10 for a quick 1-second capture
    private static final int ALIGN_THRESHOLD = 10;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 10;

    // Supabase config for direct upload
    private static final String SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register);

        // ⭐ Check if this was launched from LoginActivity (Follow-up registration)
        if (getIntent() != null && getIntent().hasExtra("USER_ID")) {
            followUpUserId = getIntent().getStringExtra("USER_ID");
        }

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

    // ⭐ CRITICAL FIX FOR THE BLACK SCREEN!
    // This tells the camera to turn on immediately after the user clicks "Allow"
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan your face.", Toast.LENGTH_LONG).show();
                finish();
            }
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

    // ⭐ DUAL-FLOW LOGIC ADDED HERE
    private void saveFaceData(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (float f : embedding) sb.append(f).append(",");
        String embeddingString = sb.toString();

        if (followUpUserId != null && !followUpUserId.isEmpty()) {
            // ➡️ FLOW 2: Follow-up Registration from Login Screen (Saves direct to Supabase)
            updateFaceDataInSupabase(followUpUserId, embeddingString);
        } else {
            // ➡️ FLOW 1: Original Sign-Up Flow (Saves to cache and goes back to previous screen)
            RegistrationCache.faceEmbedding = embeddingString;
            setResult(RESULT_OK, new Intent());
            finish();
        }
    }

    // ⭐ Direct database updater for follow-up registrations
    private void updateFaceDataInSupabase(String userId, String embedding) {
        runOnUiThread(() -> tvStatus.setText("Saving to Database..."));

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("face_embedding", embedding);

                RequestBody body = RequestBody.create(
                        json.toString(), MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .patch(body)
                        .build();

                OkHttpClient client = new OkHttpClient();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Face registered successfully!", Toast.LENGTH_SHORT).show();

                            // Automatically take them into the app now that they are verified!
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> resetAlignment("Failed to save. Try again."));
                        isCapturing = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> resetAlignment("Network Error"));
                isCapturing = false;
            }
        }).start();
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