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

public class TranslationHelper {

    private static final String PREFS_NAME = "TranslationCache";
    private static Translator englishToTagalog;
    private static boolean isModelReady = false;

    // 1. Initialize & Download Model (Call this in MainActivity or Splash Screen)
    public static void downloadModel(Context context) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.TAGALOG)
                .build();

        englishToTagalog = Translation.getClient(options);

        // Require Wifi to download the ~30MB model
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

    // 2. The Main Function: Auto-Translate OR Restore
    public static void autoTranslate(Context context, TextView textView, String textToTranslate) {
        if (context == null || textView == null) return;

        SharedPreferences settings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isTagalog = settings.getBoolean("is_tagalog", false);

        // --- STEP A: SAVE ORIGINAL ENGLISH ---
        // We use setTag() to store the original English text permanently on the View
        if (textView.getTag() == null) {
            textView.setTag(textToTranslate); // Save "Submit"
        }

        String originalEnglish = textView.getTag().toString();

        // --- STEP B: CHECK MODE ---
        if (!isTagalog) {
            // ENGLISH MODE: Restore the original text immediately
            textView.setText(originalEnglish);
            return;
        }

        // --- STEP C: TAGALOG MODE (ML KIT) ---
        // 1. Check Cache first (Instant load)
        SharedPreferences cache = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedTranslation = cache.getString(originalEnglish, null);

        if (cachedTranslation != null) {
            textView.setText(cachedTranslation);
            return;
        }

        // 2. If model not ready, keep English for now
        if (!isModelReady || englishToTagalog == null) {
            textView.setText(originalEnglish);
            return;
        }

        // 3. Perform Translation
        englishToTagalog.translate(originalEnglish)
                .addOnSuccessListener(translatedText -> {
                    if (textView != null) {
                        textView.setText(translatedText);
                        // Save to Cache so we don't need internet/ML next time
                        cache.edit().putString(originalEnglish, translatedText).apply();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep English if error
                    textView.setText(originalEnglish);
                });
    }

    // 3. Recursive Layout Translator
    public static void translateViewHierarchy(Context context, View view) {
        if (view == null) return;

        // If it's a Layout (Linear, Constraint, etc.), iterate children
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                translateViewHierarchy(context, group.getChildAt(i));
            }
        }
        // If it's a TextView or Button
        else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CharSequence currentText = textView.getText();

            // Filter: Don't translate empty, numbers, or very short text
            if (currentText != null && currentText.length() > 1 && !isNumeric(currentText.toString())) {

                // CRITICAL FIX:
                // If the view already has a TAG, use that as the source (it's the Original English).
                // If NO TAG, use the current text as the source and it will be saved as the tag.
                String sourceText;
                if (textView.getTag() != null) {
                    sourceText = textView.getTag().toString();
                } else {
                    sourceText = currentText.toString();
                }

                autoTranslate(context, textView, sourceText);
            }
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public static void close() {
        if (englishToTagalog != null) {
            englishToTagalog.close();
        }
    }
}