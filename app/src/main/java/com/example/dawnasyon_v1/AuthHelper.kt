package com.example.dawnasyon_v1

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- IMPORTS FOR KOTLIN 2.0 / SUPABASE v3.0 ---
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

object AuthHelper {

    interface RegistrationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    // --- 1. LOGIN ---
    @JvmStatic
    fun loginUser(email: String, pass: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Login Failed") }
            }
        }
    }

    // --- 2. SIGN UP ---
    @JvmStatic
    fun initiateSignUp(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val email = RegistrationCache.tempEmail
        val password = RegistrationCache.tempPassword

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // This triggers the email to be sent
                val response = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // If we get here, Supabase has accepted the request and sent the OTP
                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }
            } catch (e: Exception) {
                Log.e("AuthHelper", "Init Failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "Failed to send OTP")
                }
            }
        }
    }

    // --- 3. VERIFY OTP ---
    @JvmStatic
    fun verifyOtp(otpCode: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val email = RegistrationCache.tempEmail

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // This is the specific command to "answer" the OTP challenge
                client.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = email,
                    token = otpCode
                )
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Invalid Code") }
            }
        }
    }

    // --- 4. FETCH USER PROFILE ---
    @JvmStatic
    fun fetchUserProfile(callback: (Profile?) -> Unit) {
        val client = SupabaseManager.client
        val userId = client.auth.currentUserOrNull()?.id

        if (userId == null) {
            callback(null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = client.from("profiles").select {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<Profile>()
                withContext(Dispatchers.Main) { callback(profile) }
            } catch (e: Exception) {
                Log.e("AuthHelper", "Fetch Profile Error: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    // --- 5. FETCH HOUSEHOLD MEMBERS ---
    @JvmStatic
    fun fetchHouseholdMembers(callback: (List<HouseholdMember>?) -> Unit) {
        val client = SupabaseManager.client
        val userId = client.auth.currentUserOrNull()?.id

        if (userId == null) {
            callback(null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch members where head_id equals current user ID
                val members = client.from("household_members").select {
                    filter { eq("head_id", userId) }
                }.decodeList<HouseholdMember>()

                withContext(Dispatchers.Main) { callback(members) }
            } catch (e: Exception) {
                Log.e("AuthHelper", "Fetch Members Error: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    // --- 6. LOGOUT ---
    @JvmStatic
    fun logoutUser() {
        val client = SupabaseManager.client
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.auth.signOut()
            } catch (e: Exception) {
                Log.e("AuthHelper", "Logout Failed: ${e.message}")
            }
        }
    }

    // --- 7. CREATE PROFILE (UPDATED WITH FACE DATA) ---
    @JvmStatic
    fun createProfileAfterVerification(context: Context, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val userId = client.auth.currentUserOrNull()?.id

        if (userId == null) {
            callback.onError("Verification failed.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A. Generate QR Code
                var qrCodeUrl: String? = null
                try {
                    qrCodeUrl = QrCodeHelper.generateAndUploadQrCode(userId)
                } catch (e: Exception) {
                    Log.e("AuthHelper", "QR Error: ${e.message}")
                }

                // B. Upload ID Image
                var uploadedIdUrl = ""
                if (RegistrationCache.tempIdImageUri.isNotEmpty()) {
                    try {
                        val fileUri = Uri.parse(RegistrationCache.tempIdImageUri)
                        val fileName = "$userId/id_proof.jpg"
                        val contentResolver = context.contentResolver
                        val inputStream = contentResolver.openInputStream(fileUri)

                        if(inputStream != null) {
                            val bytes = inputStream.readBytes()
                            inputStream.close()
                            val bucket = client.storage.from("images")

                            // Kotlin 2.0 Syntax
                            bucket.upload(fileName, bytes) { upsert = true }

                            uploadedIdUrl = bucket.publicUrl(fileName)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthHelper", "Upload Error: ${e.message}")
                    }
                }

                // C. ⭐ NEW: Get Face Embedding from Cache
                // We grab the string we saved in FaceRegisterActivity
                val faceData = RegistrationCache.faceEmbedding

                // D. Create Profile Object
                val profile = Profile(
                    id = userId,
                    full_name = RegistrationCache.tempFullName,
                    contact_number = RegistrationCache.tempContact,
                    email = RegistrationCache.tempEmail,
                    house_number = RegistrationCache.tempHouseNo,
                    street = RegistrationCache.tempStreet,
                    barangay = RegistrationCache.tempBrgy,
                    city = RegistrationCache.tempCity,
                    province = RegistrationCache.tempProvince,
                    zip_code = RegistrationCache.tempZip,
                    id_image_url = uploadedIdUrl,
                    qr_code_url = qrCodeUrl,

                    // ⭐ PASS IT TO THE PROFILE HERE
                    // Make sure your Profile.kt data class has this field!
                    face_embedding = if (faceData.isNotEmpty()) faceData else null
                )

                // E. Insert into Supabase
                client.from("profiles").insert(profile)

                // F. Insert Household Members
                val members = RegistrationCache.tempHouseholdList
                if (members.isNotEmpty()) {
                    val linkedMembers = members.map { it.copy(head_id = userId) }
                    client.from("household_members").insert(linkedMembers)
                }

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                    RegistrationCache.clear() // Clear all temporary data
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Save Failed") }
            }
        }
    }
}