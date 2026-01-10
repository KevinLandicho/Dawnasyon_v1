package com.example.dawnasyon_v1

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable // ✅ Added Import

interface AnnouncementCallback {
    fun onSuccess(data: List<Announcement>)
    fun onError(message: String)
}

interface ApplicationCallback {
    fun onSuccess()
    fun onError(message: String)
}

object SupabaseJavaHelper {

    @JvmStatic
    fun fetchAnnouncements(callback: AnnouncementCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonResponse = SupabaseManager.client
                    .from("announcements")
                    .select {
                        order("created_at", order = Order.DESCENDING)
                    }
                    .data

                val type = object : TypeToken<List<Announcement>>() {}.type
                val dataList = Gson().fromJson<List<Announcement>>(jsonResponse, type)

                Handler(Looper.getMainLooper()).post {
                    callback.onSuccess(dataList)
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    callback.onError(e.message ?: "Unknown Error")
                }
            }
        }
    }

    @JvmStatic
    fun applyToDrive(driveId: Long, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user

                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post {
                        callback.onError("You must be logged in.")
                    }
                    return@launch
                }

                // ✅ FIX: Use a specific class instead of a generic Map
                val applicationData = ApplicationDTO(
                    drive_id = driveId,
                    user_id = currentUser.id,
                    status = "Pending"
                )

                // Insert the specific object
                SupabaseManager.client.from("relief_applications").insert(applicationData)

                Handler(Looper.getMainLooper()).post {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Application failed"
                Handler(Looper.getMainLooper()).post {
                    if (errorMsg.contains("unique_application_per_drive")) {
                        callback.onError("You have already applied to this drive.")
                    } else {
                        callback.onError(errorMsg)
                    }
                }
            }
        }
    }
}

// ✅ INTERNAL CLASS: This tells Supabase exactly what types to send
@Serializable
data class ApplicationDTO(
    val drive_id: Long,
    val user_id: String,
    val status: String
)