package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class FaceVerificationHelper {

    // ⭐ PASTE YOUR FACE++ KEYS HERE
    private static final String API_KEY = "ngWDogio3hF2TVCGCfvpTWiCGvUZtzMa";
    private static final String API_SECRET = "pv31P3Tj-9ut-_t0h46OaYiy8b_hsoUt";
    private static final String COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";

    public interface FaceMatchCallback {
        void onSuccess(double confidenceScore);
        void onFailed(String reason);
    }

    public static void compareFaces(Context context, Uri idUri, Bitmap selfieBitmap, FaceMatchCallback callback) {
        // Convert both the ID image and the Live Selfie into Base64 strings for the API
        String base64Id = encodeUriToBase64(context, idUri);
        String base64Selfie = encodeBitmapToBase64(selfieBitmap);

        if (base64Id == null || base64Selfie == null) {
            callback.onFailed("Could not process images. They might be corrupted.");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("api_key", API_KEY)
                .add("api_secret", API_SECRET)
                .add("image_base64_1", base64Id)       // The face from the ID
                .add("image_base64_2", base64Selfie)   // The face from the Live Selfie
                .build();

        Request request = new Request.Builder().url(COMPARE_URL).post(formBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailed("Network error. Please check your internet connection.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        if (json.has("confidence")) {
                            double confidence = json.getDouble("confidence");
                            callback.onSuccess(confidence);
                        } else {
                            callback.onFailed("Could not detect a face clearly in both images. Please ensure good lighting.");
                        }
                    } catch (Exception e) {
                        callback.onFailed("Error parsing AI response.");
                    }
                } else {
                    callback.onFailed("API Rejected the request. Check your API Keys.");
                }
            }
        });
    }

    private static String encodeUriToBase64(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return encodeBitmapToBase64(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String encodeBitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Compress the image down so it doesn't overload the API limits
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}