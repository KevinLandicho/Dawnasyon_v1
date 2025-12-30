package com.example.dawnasyon_v1;

import java.util.ArrayList;
import java.util.List;

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

    public static String tempIdImageUri = "";

    // ⭐ ADD THIS: Store the family members here
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
        tempHouseholdList.clear(); // ⭐ Clear the list too
    }
}