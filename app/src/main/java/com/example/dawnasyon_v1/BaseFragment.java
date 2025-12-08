package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.view.View;
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
}