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
import kotlinx.serialization.Serializable

// ✅ INTERFACES ARE DEFINED HERE (Top Level)
// This makes them visible to Java as "NotificationCallback", not "SupabaseJavaHelper.NotificationCallback"

interface AnnouncementCallback {
    fun onSuccess(data: List<Announcement>)
    fun onError(message: String)
}

interface ApplicationCallback {
    fun onSuccess()
    fun onError(message: String)
}

interface NotificationCallback {
    fun onSuccess(data: List<NotificationItem>)
    fun onError(message: String)
}

object SupabaseJavaHelper {

    // 1. FETCH ANNOUNCEMENTS
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

    // 2. APPLY TO DRIVE
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

                val applicationData = ApplicationDTO(
                    drive_id = driveId,
                    user_id = currentUser.id,
                    status = "Pending"
                )

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

    // 3. FETCH NOTIFICATIONS
    @JvmStatic
    fun fetchNotifications(callback: NotificationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user

                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post {
                        callback.onError("User not logged in.")
                    }
                    return@launch
                }

                val jsonResponse = SupabaseManager.client
                    .from("notifications")
                    .select {
                        filter {
                            eq("user_id", currentUser.id)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .data

                val type = object : TypeToken<List<NotificationItem>>() {}.type
                val dataList = Gson().fromJson<List<NotificationItem>>(jsonResponse, type)

                Handler(Looper.getMainLooper()).post {
                    callback.onSuccess(dataList)
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    callback.onError(e.message ?: "Fetch failed")
                }
            }
        }
    }
}

@Serializable
data class ApplicationDTO(
    val drive_id: Long,
    val user_id: String,
    val status: String
)