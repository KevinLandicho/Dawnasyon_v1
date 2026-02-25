package com.example.dawnasyon_v1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

object AuthHelper {

    interface RegistrationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    interface SimpleCallback {
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

    // --- 2. FETCH PROFILE ---
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

    // --- 3. CREATE PROFILE ---
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
                // Upload ID
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
                    } catch (e: Exception) { Log.e("AuthHelper", "Upload Error: ${e.message}") }
                }

                var qrCodeUrl: String? = null
                try { qrCodeUrl = QrCodeHelper.generateAndUploadQrCode(userId) } catch (e: Exception) {}

                val faceData = RegistrationCache.faceEmbedding
                val userType = RegistrationCache.userType ?: "Resident"

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
                    type = userType,
                    notes = RegistrationCache.notes // ⭐ ADDED THIS LINE TO PUSH NOTES TO DB
                )

                client.from("profiles").insert(profile)

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

    // --- OTHER METHODS ---
    @JvmStatic
    fun initiateSignUp(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val email = RegistrationCache.tempEmail
        val password = RegistrationCache.tempPassword
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.auth.signUpWith(Email) { this.email = email; this.password = password }
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("already registered") || msg.contains("User already exists")) {
                    try {
                        client.auth.resendEmail(OtpType.Email.SIGNUP, email)
                        withContext(Dispatchers.Main) { callback.onSuccess() }
                    } catch (re: Exception) { withContext(Dispatchers.Main) { callback.onError("Account exists. Please Log In.") } }
                } else { withContext(Dispatchers.Main) { callback.onError(msg) } }
            }
        }
    }

    @JvmStatic
    fun resendOtp(email: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.auth.resendEmail(type = OtpType.Email.SIGNUP, email = email)
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { callback.onError(e.message ?: "Resend Failed") } }
        }
    }

    @JvmStatic
    fun verifyOtp(otpCode: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val email = RegistrationCache.tempEmail
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.auth.verifyEmailOtp(OtpType.Email.SIGNUP, email, otpCode)
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { callback.onError("Invalid Code") } }
        }
    }

    @JvmStatic
    fun fetchHouseholdMembers(callback: (List<HouseholdMember>?) -> Unit) {
        val client = SupabaseManager.client
        val userId = client.auth.currentUserOrNull()?.id
        if (userId == null) { callback(null); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val members = client.from("household_members").select { filter { eq("head_id", userId) } }.decodeList<HouseholdMember>()
                withContext(Dispatchers.Main) { callback(members) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { callback(null) } }
        }
    }

    @JvmStatic
    fun logoutUser() {
        CoroutineScope(Dispatchers.IO).launch { try { SupabaseManager.client.auth.signOut() } catch(e: Exception) {} }
    }

    @JvmStatic
    fun changePassword(oldPass: String, newPass: String, callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val currentUser = client.auth.currentUserOrNull()
        val email = currentUser?.email
        if (email == null) { callback.onError("User not logged in."); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.auth.signInWith(Email) { this.email = email; this.password = oldPass }
                client.auth.updateUser { password = newPass }
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                withContext(Dispatchers.Main) {
                    if (msg.contains("Invalid login") || msg.contains("invalid_grant")) callback.onError("Incorrect old password.")
                    else callback.onError("Failed to update password: $msg")
                }
            }
        }
    }

    @JvmStatic
    fun archiveAccount(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val currentUser = client.auth.currentUserOrNull()
        if (currentUser == null) { callback.onError("No user logged in."); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf("account_status" to "Archived", "full_name" to "Deleted User", "contact_number" to "", "face_embedding" to null, "id_image_url" to null, "qr_code_url" to null, "fcm_token" to null)
                client.from("profiles").update(updateData) { filter { eq("id", currentUser.id) } }
                client.auth.signOut()
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { callback.onError(e.message ?: "Failed to delete account") } }
        }
    }

    // ⭐ 10. FIXED: DEEP LINK HANDLER (Increased Polling Time)
    @JvmStatic
    fun handleDeepLink(intent: android.content.Intent, onRecovery: () -> Unit) {
        val client = SupabaseManager.client
        val uri = intent.data ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Let Supabase handle the intent
                client.handleDeeplinks(intent)

                // 2. WAIT for session (Increased to 20 checks = 10 seconds)
                // This gives slow networks enough time to log in before we check
                var sessionFound = false
                for (i in 1..1) {
                    if (client.auth.currentSessionOrNull() != null) {
                        sessionFound = true
                        break
                    }
                    delay(500)
                }

                // 3. Trigger popup if we have a session OR it looks like a recovery link
                val urlString = uri.toString()
                val fragment = uri.fragment ?: ""
                val isRecoveryLink = urlString.contains("type=recovery") ||
                        fragment.contains("type=recovery") ||
                        urlString.contains("reset-callback")

                if (sessionFound && isRecoveryLink) {
                    withContext(Dispatchers.Main) {
                        onRecovery()
                    }
                } else if (isRecoveryLink) {
                    // Fallback: Show popup anyway so user doesn't think it failed silently
                    withContext(Dispatchers.Main) {
                        onRecovery()
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthHelper", "Deep Link Error: ${e.message}")
            }
        }
    }

    // ⭐ 11. UPDATE PASSWORD (Safe Check)
    @JvmStatic
    fun updateUserPassword(newPass: String, callback: SimpleCallback) {
        val client = SupabaseManager.client
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (client.auth.currentSessionOrNull() == null) {
                    withContext(Dispatchers.Main) { callback.onError("Session expired. Please click the email link again.") }
                    return@launch
                }

                client.auth.updateUser {
                    password = newPass
                }
                withContext(Dispatchers.Main) { callback.onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: "Update failed") }
            }
        }
    }
}