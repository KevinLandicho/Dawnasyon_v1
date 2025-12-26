package com.example.dawnasyon_v1;

import java.util.ArrayList;
import java.util.List;

public class FamilyDataRepository {
    // This list holds the family members temporarily
    private static List<FamilyMember> familyMembers = new ArrayList<>();

    // Add a member
    public static void addMember(FamilyMember member) {
        familyMembers.add(member);
    }

    // ⭐ THIS WAS MISSING
    public static List<FamilyMember> getFamilyMembers() {
        return familyMembers;
    }

    // Clear data after registration
    public static void clearData() {
        familyMembers.clear();
    }

    // Inner class for the data structure
    public static class FamilyMember {
        public String firstName;
        public String lastName;
        public String relationship;
        public String age;
        public String gender;

        public FamilyMember(String fName, String lName, String rel, String age, String gen) {
            this.firstName = fName;
            this.lastName = lName;
            this.relationship = rel;
            this.age = age;
            this.gender = gen;
        }
    }
}