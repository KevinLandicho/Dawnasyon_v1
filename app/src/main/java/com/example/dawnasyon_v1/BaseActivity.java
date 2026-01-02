package com.example.dawnasyon_v1;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Just keeps your language settings working
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}