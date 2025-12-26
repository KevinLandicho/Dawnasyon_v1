package com.example.dawnasyon_v1;

public class RegistrationCache {
    public static String tempEmail = "";
    public static String tempPassword = "";
    public static String tempFullName = "";
    public static String tempContact = "";

    // Address
    public static String tempHouseNo = "";
    public static String tempStreet = "";
    public static String tempBrgy = "";
    public static String tempCity = "";
    public static String tempProvince = "";
    public static String tempZip = "";

    // ⭐ THIS WAS MISSING! Add it now to fix the error.
    public static String tempIdImageUri = "";

    public static void clear() {
        tempEmail = "";
        tempPassword = "";
        tempFullName = "";
        tempContact = "";
        tempHouseNo = "";
        tempStreet = "";
        tempBrgy = "";
        tempCity = "";
        tempProvince = "";
        tempZip = "";
        tempIdImageUri = ""; // Clear this too
    }
}