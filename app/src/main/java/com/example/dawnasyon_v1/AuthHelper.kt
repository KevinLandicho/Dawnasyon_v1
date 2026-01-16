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
// ⭐ ADD THIS IMPORT TO FIX THE ERROR
import io.github.jan.supabase.auth.*

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

    // --- 2. SIGN UP (SMART LOGIC) ---
    @JvmStatic
    fun initiateSignUp(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val email = RegistrationCache.tempEmail
        val password = RegistrationCache.tempPassword

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A. Try standard signup first
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                withContext(Dispatchers.Main) { callback.onSuccess() }

            } catch (e: Exception) {
                // B. ⭐ CATCH "USER EXISTS" ERROR
                val msg = e.message ?: ""

                if (msg.contains("already registered") || msg.contains("User already exists")) {
                    Log.d("AuthHelper", "User exists (Unverified). Attempting to resend OTP.")

                    try {
                        // C. AUTOMATICALLY RESEND OTP
                        // This now works because we added the import 'io.github.jan.supabase.auth.resend'
                        client.auth.resendEmail(
                            type = OtpType.Email.SIGNUP,
                            email = email
                        )
                        // Treat as success so UI moves to OTP screen
                        withContext(Dispatchers.Main) { callback.onSuccess() }

                    } catch (resendError: Exception) {
                        // If resend fails, the account is likely Verified already
                        withContext(Dispatchers.Main) {
                            callback.onError("Account exists. Please Log In.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError(msg)
                    }
                }
            }
        }
    }

    // --- 2.5 RESEND OTP (MANUAL) ---
    @JvmStatic
    fun resendOtp(email: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // This now works with the import
                client.auth.resendEmail(
                    type = OtpType.Email.SIGNUP,
                    email = email
                )
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Resend Failed") }
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

    // --- 7. CREATE PROFILE (UPDATED WITH USER TYPE) ---
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
                            bucket.upload(fileName, bytes) { upsert = true }
                            uploadedIdUrl = bucket.publicUrl(fileName)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthHelper", "Upload Error: ${e.message}")
                    }
                }

                // C. Get Face Embedding & User Type from Cache
                val faceData = RegistrationCache.faceEmbedding

                // ⭐ GET THE USER TYPE HERE (Default to Resident if null)
                val userType = RegistrationCache.userType ?: "Resident"

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
                    face_embedding = if (faceData.isNotEmpty()) faceData else null,
                    type = userType
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
                    RegistrationCache.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Save Failed") }
            }
        }
    }

    // --- 8. CHANGE PASSWORD ---
    @JvmStatic
    fun changePassword(oldPass: String, newPass: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val currentUser = client.auth.currentUserOrNull()
        val email = currentUser?.email

        if (email == null) {
            callback.onError("User not logged in.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Verify Old Password by attempting to sign in again
                // This ensures the user actually knows the current password
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = oldPass
                }

                // 2. If login succeeds, update to the New Password
                client.auth.updateUser {
                    password = newPass
                }

                withContext(Dispatchers.Main) { callback.onSuccess() }

            } catch (e: Exception) {
                val msg = e.message ?: ""
                withContext(Dispatchers.Main) {
                    if (msg.contains("Invalid login") || msg.contains("invalid_grant")) {
                        callback.onError("Incorrect old password.")
                    } else {
                        callback.onError("Failed to update password: $msg")
                    }
                }
            }
        }
    }

    // --- 9. ARCHIVE ACCOUNT (SOFT DELETE) ---
    @JvmStatic
    fun archiveAccount(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val currentUser = client.auth.currentUserOrNull()

        if (currentUser == null) {
            callback.onError("No user logged in.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Prepare Anonymized Data
                // We keep the ID, but clear the personal info
                val updateData = mapOf(
                    "account_status" to "Archived",
                    "full_name" to "Deleted User",
                    "contact_number" to "",
                    "face_embedding" to null,
                    "id_image_url" to null,
                    "qr_code_url" to null,
                    "fcm_token" to null
                    // Note: We keep the ID so donation history is preserved
                )

                // 2. Update the Profile in Database
                client.from("profiles").update(updateData) {
                    filter { eq("id", currentUser.id) }
                }

                // 3. Sign Out the User from Auth
                client.auth.signOut()

                withContext(Dispatchers.Main) { callback.onSuccess() }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "Failed to delete account")
                }
            }
        }
    }
}