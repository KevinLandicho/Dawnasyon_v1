package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class SecureStorage {

    private static final String FILE_NAME = "secure_user_data";

    // Helper to save data (Simulates Encryption using Base64)
    public static void saveSensitiveData(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);

        // We use Base64 encoding to make it look like "gibberish" ciphertext
        // It's not military-grade encryption, but it hides the plain text for the demo.
        String encryptedValue = Base64.encodeToString(value.getBytes(), Base64.DEFAULT);

        prefs.edit().putString(key, encryptedValue).apply();
    }

    // Helper to get data (Decrypts the simulation)
    public static String getSensitiveData(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        String encryptedValue = prefs.getString(key, null);

        if (encryptedValue != null) {
            return new String(Base64.decode(encryptedValue, Base64.DEFAULT));
        }
        return null;
    }
}