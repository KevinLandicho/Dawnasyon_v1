package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

public class TranslationHelper {

    private static final String PREFS_NAME = "TranslationCache";
    private static Translator englishToTagalog;
    private static boolean isModelReady = false;

    // ⭐ 1. MANUAL DICTIONARY OVERRIDES
    private static final Map<String, String> MANUAL_OVERRIDES = new HashMap<>();
    static {
        // Fix for Family Members (Miyembro ng Pamilya)
        MANUAL_OVERRIDES.put("household members", "Miyembro ng Pamilya");
        MANUAL_OVERRIDES.put("household member", "Miyembro ng Pamilya");
        MANUAL_OVERRIDES.put("household family", "Miyembro ng Pamilya");
        MANUAL_OVERRIDES.put("family members", "Miyembro ng Pamilya");
        MANUAL_OVERRIDES.put("household", "Miyembro ng Pamilya");

        // Fix for Head
        MANUAL_OVERRIDES.put("head", "Ulo ng Pamilya");
        MANUAL_OVERRIDES.put("household head", "Ulo ng Pamilya");

        // Fix for Score Details Headers
        MANUAL_OVERRIDES.put("score breakdown:", "Detalye ng Puntos:");
        MANUAL_OVERRIDES.put("score breakdown", "Detalye ng Puntos");
        MANUAL_OVERRIDES.put("critical priority", "Kritikal na Prayoridad");
    }

    public static void downloadModel(Context context) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.TAGALOG)
                .build();

        englishToTagalog = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        englishToTagalog.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    Log.d("Translator", "Tagalog Model Downloaded & Ready");
                    isModelReady = true;
                })
                .addOnFailureListener(e -> {
                    Log.e("Translator", "Model download failed: " + e.getMessage());
                    isModelReady = false;
                });
    }

    public static void autoTranslate(Context context, TextView textView, String textToTranslate) {
        if (context == null || textView == null || textToTranslate == null) return;

        SharedPreferences settings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isTagalog = settings.getBoolean("is_tagalog", false);

        textView.setTag(textToTranslate);
        String originalEnglish = textToTranslate;

        if (!isTagalog) {
            textView.setText(originalEnglish);
            return;
        }

        // --- STEP B: CHECK MANUAL OVERRIDES ---
        String lowerOriginal = originalEnglish.toLowerCase().trim();
        if (MANUAL_OVERRIDES.containsKey(lowerOriginal)) {
            String overrideTranslation = MANUAL_OVERRIDES.get(lowerOriginal);
            textView.setText(matchCasing(originalEnglish, overrideTranslation));
            return;
        }

        // --- STEP C: LIST/BULLET POINT LOGIC (FIX FOR PRIORITY SCORE) ---
        // If the text contains bullet points, we translate it line by line to prevent scrambling
        if (originalEnglish.contains("•")) {
            translateBulletList(textView, originalEnglish);
            return;
        }

        // --- STEP D: STANDARD TRANSLATION ---
        SharedPreferences cache = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedTranslation = cache.getString(originalEnglish, null);

        if (cachedTranslation != null) {
            textView.setText(cachedTranslation);
            return;
        }

        if (!isModelReady || englishToTagalog == null) {
            textView.setText(originalEnglish);
            return;
        }

        englishToTagalog.translate(originalEnglish)
                .addOnSuccessListener(translatedText -> {
                    String properlyCasedTranslation = matchCasing(originalEnglish, translatedText);
                    if (textView != null && textView.getTag().equals(originalEnglish)) {
                        textView.setText(properlyCasedTranslation);
                    }
                    cache.edit().putString(originalEnglish, properlyCasedTranslation).apply();
                })
                .addOnFailureListener(e -> {
                    if (textView != null && textView.getTag().equals(originalEnglish)) {
                        textView.setText(originalEnglish);
                    }
                });
    }

    // ⭐ NEW: HELPER TO PREVENT BULLET POINT SCRAMBLING
    private static void translateBulletList(TextView textView, String fullText) {
        String[] parts = fullText.split("•");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;

            final int index = i;
            final boolean isLast = (i == parts.length - 1);

            // Translate each bullet point individually
            englishToTagalog.translate(part).addOnSuccessListener(translatedPart -> {
                result.append("• ").append(translatedPart).append(isLast ? "" : " ");
                // Update text view as each part arrives to keep UI responsive
                textView.setText(result.toString().trim());
            });
        }
    }

    private static String matchCasing(String original, String translated) {
        if (original == null || translated == null || translated.isEmpty() || original.isEmpty()) return translated;
        if (original.equals(original.toUpperCase())) return translated.toUpperCase();
        if (original.equals(original.toLowerCase())) return translated.toLowerCase();
        if (Character.isUpperCase(original.charAt(0))) {
            return translated.substring(0, 1).toUpperCase() + translated.substring(1);
        }
        return translated;
    }

    public static void translateViewHierarchy(Context context, View view) {
        if (view == null) return;

        // Skip items marked as "no_translate" (Names, etc.)
        if (view.getContentDescription() != null && view.getContentDescription().toString().equals("no_translate")) {
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                translateViewHierarchy(context, group.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            String sourceText = (textView.getTag() != null) ? textView.getTag().toString() : textView.getText().toString();
            if (sourceText.length() > 1 && !isNumeric(sourceText)) {
                autoTranslate(context, textView, sourceText);
            }
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public static void close() {
        if (englishToTagalog != null) englishToTagalog.close();
    }
}