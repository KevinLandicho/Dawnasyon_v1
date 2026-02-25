package com.example.dawnasyon_v1;

import java.util.ArrayList;
import java.util.List;

public class RegistrationCache {
    public static String tempEmail = "";
    public static String tempPassword = "";
    public static boolean hasViewedTerms = false;
    public static String tempFullName = ""; // This stores the name from Step 1
    public static String tempContact = "";
    public static String faceEmbedding = "";

    // Address
    public static String tempHouseNo = "";
    public static String tempStreet = "";
    public static String tempBrgy = "";
    public static String tempCity = "";
    public static String tempProvince = "";
    public static String tempZip = "";
    public static String userType = "Resident";
    public static String tempIdImageUri = "";

    // ⭐ ADDED: To store Name and Address mismatch warnings
    public static String notes = "";
    public static String extractedAddress = "";
    public static String nameMismatchNotes = "";

    // Household Members List
    public static List<HouseholdMember> tempHouseholdList = new ArrayList<>();

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
        tempIdImageUri = "";
        notes = ""; // ⭐ Reset notes
        tempHouseholdList.clear();
    }
}