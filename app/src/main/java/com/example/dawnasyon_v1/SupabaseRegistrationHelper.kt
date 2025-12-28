package com.example.dawnasyon_v1

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

object SupabaseRegistrationHelper {

    interface RegistrationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    // 1. MATCHES YOUR 'profiles' TABLE
    @Serializable
    data class Profile(
        val id: String,
        val full_name: String,
        val contact_number: String,
        val house_number: String,
        val street: String,
        val barangay: String,
        val city: String,
        val province: String,
        val zip_code: String,
        val id_image_url: String
    )

    // 2. MATCHES YOUR 'household_members' TABLE
    @Serializable
    data class HouseholdMember(
        val head_id: String,
        val full_name: String,
        val relation: String,
        val age: Int,
        val gender: String
    )

    // --- REGISTRATION FUNCTION ---
    @JvmStatic
    fun registerCompleteUser(context: Context, callback: RegistrationCallback) {
        val email = RegistrationCache.tempEmail
        val password = RegistrationCache.tempPassword
        val fullName = RegistrationCache.tempFullName
        val contact = RegistrationCache.tempContact

        // Address Parts
        val house = RegistrationCache.tempHouseNo
        val street = RegistrationCache.tempStreet
        val brgy = RegistrationCache.tempBrgy
        val city = RegistrationCache.tempCity
        val prov = RegistrationCache.tempProvince
        val zip = RegistrationCache.tempZip

        // ID Image
        val idUriString = RegistrationCache.tempIdImageUri ?: ""
        val idUri = if (idUriString.isNotEmpty()) Uri.parse(idUriString) else null

        Log.d("SupabaseRegister", "Starting Registration for: $email")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // --- STEP A: Create Auth Account ---
                SupabaseManager.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val userId = SupabaseManager.client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Auth created, but User ID is null.")

                // --- STEP B: Upload ID Image (If exists) ---
                var uploadedImageUrl = ""
                if (idUri != null) {
                    try {
                        val byteArray = context.contentResolver.openInputStream(idUri)?.use { it.readBytes() }
                        if (byteArray != null) {
                            val fileName = "$userId/valid_id.jpg"
                            val bucket = SupabaseManager.client.storage.from("images")

                            // Correct syntax for upsert
                            bucket.upload(fileName, byteArray) {
                                upsert = true
                            }

                            uploadedImageUrl = bucket.publicUrl(fileName)
                        }
                    } catch (e: Exception) {
                        Log.e("SupabaseRegister", "ID Upload Failed", e)
                    }
                }

                // --- STEP C: Insert into 'profiles' ---
                val newProfile = Profile(
                    id = userId,
                    full_name = fullName,
                    contact_number = contact,
                    house_number = house,
                    street = street,
                    barangay = brgy,
                    city = city,
                    province = prov,
                    zip_code = zip,
                    id_image_url = uploadedImageUrl
                )
                SupabaseManager.client.from("profiles").insert(newProfile)

                // --- STEP D: Insert Family Members ---
                val familyList = FamilyDataRepository.getFamilyMembers()
                if (familyList.isNotEmpty()) {
                    val membersToInsert = familyList.map { member ->
                        HouseholdMember(
                            head_id = userId,
                            full_name = "${member.firstName} ${member.lastName}",
                            relation = member.relationship,
                            age = member.age.toIntOrNull() ?: 0,
                            gender = member.gender
                        )
                    }
                    SupabaseManager.client.from("household_members").insert(membersToInsert)
                }

                Log.d("SupabaseRegister", "ALL DATA SAVED SUCCESSFULLY!")

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                Log.e("SupabaseRegister", "Registration Error", e)
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "Unknown Error")
                }
            }
        }
    }

    // --- LOGIN FUNCTION ---
    @JvmStatic
    fun loginUser(email: String, pass: String, callback: RegistrationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Attempt to Sign In
                SupabaseManager.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }

                Log.d("SupabaseLogin", "Login Successful: $email")

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                Log.e("SupabaseLogin", "Login Failed", e)
                withContext(Dispatchers.Main) {
                    // Check for common errors
                    val msg = e.message ?: ""
                    if (msg.contains("Invalid login credentials")) {
                        callback.onError("Incorrect email or password.")
                    } else if (msg.contains("Email not confirmed")) {
                        callback.onError("Please verify your email first.")
                    } else {
                        callback.onError("Login Failed: Please check your internet.")
                    }
                }
            }
        }
    }
    @JvmStatic
    fun fetchUserProfile(callback: (Profile?) -> Unit) {
        val userId = SupabaseManager.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            callback(null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the row where 'id' matches the logged-in User ID
                val profile = SupabaseManager.client.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                    }.decodeSingle<Profile>()

                withContext(Dispatchers.Main) {
                    callback(profile)
                }
            } catch (e: Exception) {
                Log.e("SupabaseFetch", "Error fetching profile", e)
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }
}