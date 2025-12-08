package com.example.dawnasyon_v1;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // This line grabs the saved language (Tagalog/English) and applies it
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}