package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ⭐ FORCE BACKGROUND & CLICK CONSUMPTION ⭐
        // This makes every screen solid so you don't see the one behind it.
        view.setBackgroundResource(R.drawable.background3);
        view.setClickable(true);
        view.setFocusable(true);
    }

    /**
     * ⭐ NEW: Helper to translate the entire screen automatically.
     * Call this at the end of onViewCreated() in your fragments.
     */
    protected void applyTagalogTranslation(View rootView) {
        if (getContext() == null || rootView == null) return;

        // ⭐ CHECK PREFERENCE FIRST
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE);
        boolean isTagalog = prefs.getBoolean("is_tagalog", false);

        if (isTagalog) {
            TranslationHelper.translateViewHierarchy(getContext(), rootView);
        }
    }
}