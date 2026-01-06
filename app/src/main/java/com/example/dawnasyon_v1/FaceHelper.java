package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FaceHelper {

    private FaceDetector faceDetector;
    private Interpreter tflite;

    // MobileFaceNet requires images to be 112x112 pixels
    private static final int INPUT_SIZE = 112;
    // Normalization values for this specific model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    public interface FaceCallback {
        void onFaceDetected(float[] embedding);
        void onError(String error);
    }

    public FaceHelper(Context context) {
        // 1. Setup ML Kit (Updated for Liveness/Blink Detection)
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)       // Needed to find eyes
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // ⭐ CRITICAL: Enables Blink Detection
                .build();

        faceDetector = FaceDetection.getClient(options);

        // 2. Setup TensorFlow Lite (To recognize who the face is)
        try {
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            // Load the model file
            ByteBuffer modelBuffer = FileUtil.loadMappedFile(context, "mobile_face_net.tflite");
            tflite = new Interpreter(modelBuffer, tfliteOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Main function: Takes a photo, finds the face, and returns the "math signature"
    public void scanFace(Bitmap bitmap, FaceCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        // We found a face! Get the first one.
                        Face face = faces.get(0);

                        // Crop the image so we only have the face part
                        Bitmap croppedFace = cropBitmap(bitmap, face.getBoundingBox());

                        // Ask the AI model to calculate the numbers (embedding)
                        float[] embedding = getFaceEmbedding(croppedFace);
                        callback.onFaceDetected(embedding);
                    } else {
                        callback.onError("No face found. Please look at the camera.");
                    }
                })
                .addOnFailureListener(e -> callback.onError("Error: " + e.getMessage()));
    }

    // Helper: Cuts the face out of the full photo
    private Bitmap cropBitmap(Bitmap original, Rect box) {
        // Ensure the box is within the image bounds to prevent crashes
        int x = Math.max(0, box.left);
        int y = Math.max(0, box.top);
        int w = Math.min(original.getWidth() - x, box.width());
        int h = Math.min(original.getHeight() - y, box.height());

        return Bitmap.createBitmap(original, x, y, w, h);
    }

    // Helper: The AI Math (TensorFlow Lite)
    private float[] getFaceEmbedding(Bitmap faceBitmap) {
        // Resize the face to 112x112 because the model expects that exact size
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();

        TensorImage tensorImage = new TensorImage(org.tensorflow.lite.DataType.FLOAT32);
        tensorImage.load(faceBitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // Run the model
        Object[] inputs = {tensorImage.getBuffer()};
        float[][] outputs = new float[1][192]; // This model returns 192 numbers
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputs);

        tflite.runForMultipleInputsOutputs(inputs, outputMap);
        return outputs[0];
    }

    // Helper: Compare two face signatures
    // Result > 0.8 usually means it is the SAME person.
    public static float compareFaces(float[] embed1, float[] embed2) {
        float dot = 0;
        float mag1 = 0;
        float mag2 = 0;

        for (int i = 0; i < embed1.length; i++) {
            dot += embed1[i] * embed2[i];
            mag1 += embed1[i] * embed1[i];
            mag2 += embed2[i] * embed2[i];
        }

        return dot / ((float) (Math.sqrt(mag1) * Math.sqrt(mag2)));
    }
}