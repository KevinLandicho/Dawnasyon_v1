package com.example.dawnasyon_v1;

import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PayMongoHelper {

    // ✅ YOUR ACTUAL TEST KEY IS HERE
    private static final String SECRET_KEY = "sk_test_X5nsvqygqWuMRLYvbmXWwu6y";
    private static final String PAYMONGO_LINKS_URL = "https://api.paymongo.com/v1/links";

    public interface PaymentListener {
        void onSuccess(String checkoutUrl, String linkId);
        void onFailure(String error);
    }

    public interface StatusListener {
        void onCheck(String status); // Returns "unpaid", "paid", or "error"
    }

    // 1. GENERATE LINK
    public static void createDonationLink(int amountPhp, String description, PaymentListener listener) {
        OkHttpClient client = new OkHttpClient();

        // PayMongo uses centavos (100 PHP = 10000 centavos)
        int amountInCentavos = amountPhp * 100;

        JSONObject json = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            JSONObject attributes = new JSONObject();

            attributes.put("amount", amountInCentavos);
            attributes.put("description", description);
            attributes.put("remarks", "Donation for Relief Goods");

            data.put("attributes", attributes);
            json.put("data", data);

        } catch (Exception e) {
            listener.onFailure("JSON Error");
            return;
        }

        // Basic Auth: Secret Key as username, empty password
        String authString = SECRET_KEY + ":";
        String encodedAuth = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(PAYMONGO_LINKS_URL)
                .post(body)
                .addHeader("Authorization", "Basic " + encodedAuth)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PAYMONGO_API", "Create Link Network Fail: " + e.getMessage());
                listener.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String respData = response.body().string();
                        JSONObject jsonResp = new JSONObject(respData);
                        JSONObject attr = jsonResp.getJSONObject("data").getJSONObject("attributes");

                        String checkoutUrl = attr.getString("checkout_url");
                        String linkId = jsonResp.getJSONObject("data").getString("id");

                        listener.onSuccess(checkoutUrl, linkId);
                    } catch (Exception e) {
                        Log.e("PAYMONGO_API", "Create Link Parse Error: " + e.getMessage());
                        listener.onFailure("Parse Error: " + e.getMessage());
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No info";
                    Log.e("PAYMONGO_API", "Create Link API Error: " + response.code() + " " + errorBody);
                    listener.onFailure("API Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    // 2. CHECK STATUS (With Detailed Logging)
    public static void checkPaymentStatus(String linkId, StatusListener listener) {
        Log.d("PAYMONGO_API", "Checking status for Link ID: " + linkId);

        OkHttpClient client = new OkHttpClient();
        String authString = SECRET_KEY + ":";
        String encodedAuth = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);

        String url = PAYMONGO_LINKS_URL + "/" + linkId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Basic " + encodedAuth)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PAYMONGO_API", "Check Status Network Fail: " + e.getMessage());
                listener.onCheck("error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String respData = response.body().string();
                        // 🔍 LOG THE RAW JSON HERE SO YOU CAN SEE IT
                        Log.d("PAYMONGO_API", "Status Response: " + respData);

                        JSONObject jsonResp = new JSONObject(respData);
                        String status = jsonResp.getJSONObject("data").getJSONObject("attributes").getString("status");

                        Log.d("PAYMONGO_API", "Parsed Status: " + status);
                        listener.onCheck(status); // "paid" or "unpaid"

                    } catch (Exception e) {
                        Log.e("PAYMONGO_API", "Check Status Parse Error: " + e.getMessage());
                        listener.onCheck("error");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No info";
                    Log.e("PAYMONGO_API", "Check Status API Error: " + response.code() + " " + errorBody);
                    listener.onCheck("error");
                }
            }
        });
    }
}