package com.example.dawnasyon_v1;

import android.content.Context;

public class AvatarHelper {

    // 1. Database Name -> Drawable Resource ID
    public static int getDrawableId(Context context, String avatarName) {
        if (avatarName == null) return R.drawable.avatar1;

        switch (avatarName) {
            case "avatar2": return R.drawable.avatar2;
            case "avatar3": return R.drawable.avatar3;
            case "avatar4": return R.drawable.avatar4;
            case "avatar5": return R.drawable.avatar5;
            case "avatar6": return R.drawable.avatar6;
            case "avatar7": return R.drawable.avatar7;
            case "avatar8": return R.drawable.avatar8;
            case "avatar11": return R.drawable.avatar11;
            default: return R.drawable.avatar1; // Handles "avatar1" and unknowns
        }
    }

    // 2. Drawable Resource ID -> Database Name
    // We use explicit mapping to ensure the string name is always correct
    public static String getResourceName(Context context, int resourceId) {
        if (resourceId == R.drawable.avatar2) return "avatar2";
        if (resourceId == R.drawable.avatar3) return "avatar3";
        if (resourceId == R.drawable.avatar4) return "avatar4";
        if (resourceId == R.drawable.avatar5) return "avatar5";
        if (resourceId == R.drawable.avatar6) return "avatar6";
        if (resourceId == R.drawable.avatar7) return "avatar7";
        if (resourceId == R.drawable.avatar8) return "avatar8";
        if (resourceId == R.drawable.avatar11) return "avatar11";

        return "avatar1"; // Default fallback
    }
}