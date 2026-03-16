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

    // 1. Initialize & Download Model
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

    // 2. The Main Function: Auto-Translate OR Restore
    public static void autoTranslate(Context context, TextView textView, String textToTranslate) {
        if (context == null || textView == null || textToTranslate == null) return;

        SharedPreferences settings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isTagalog = settings.getBoolean("is_tagalog", false);

        // ⭐ CRITICAL FIX 1: ALWAYS update the tag to the incoming text.
        // This ensures recycled views (like in lists) track the correct new text.
        textView.setTag(textToTranslate);
        String originalEnglish = textToTranslate;

        // --- STEP A: CHECK MODE ---
        if (!isTagalog) {
            // ENGLISH MODE: Restore the original text immediately
            textView.setText(originalEnglish);
            return;
        }

        // --- STEP B: TAGALOG MODE (ML KIT) ---
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
                        // ⭐ CRITICAL FIX 2: RECYCLERVIEW GLITCH PREVENTION
                        // Check if the View's tag STILL matches the text we just translated.
                        // If the user scrolled and the view was recycled, the tag will be different,
                        // so we skip setting the text to prevent scrambling the UI.
                        Object currentTag = textView.getTag();
                        if (currentTag != null && currentTag.toString().equals(originalEnglish)) {
                            textView.setText(translatedText);
                        }

                        // Save to Cache so we don't need internet/ML next time
                        cache.edit().putString(originalEnglish, translatedText).apply();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep English if error, but only if view wasn't recycled
                    Object currentTag = textView.getTag();
                    if (currentTag != null && currentTag.toString().equals(originalEnglish)) {
                        textView.setText(originalEnglish);
                    }
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

                // Use the stored Tag if it exists (Original English), otherwise use current text
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