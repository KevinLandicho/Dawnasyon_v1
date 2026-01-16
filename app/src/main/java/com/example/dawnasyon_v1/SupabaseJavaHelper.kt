package com.example.dawnasyon_v1

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// --- CALLBACK INTERFACES ---

interface SimpleCallback {
    fun onSuccess()
    fun onError(message: String)
}

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

interface DonationHistoryCallback {
    fun onSuccess(data: List<DonationHistoryItem>)
    fun onError(message: String)
}

interface DashboardCallback {
    fun onDataLoaded(
        inventory: Map<String, Int>,
        areas: Map<String, Int>,
        donations: Map<String, Float>,
        families: Map<String, Int>,
        metrics: DashboardMetrics
    )
    fun onError(message: String)
}

object SupabaseJavaHelper {

    // ⭐ 1. INTERFACE FOR JAVA (Needed for archiveAccount)
    interface RegistrationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    // ⭐ 2. FETCH ANNOUNCEMENTS (Likes, Bookmarks, Applications + Sorting)
    @JvmStatic
    fun fetchAnnouncements(callback: AnnouncementCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A. Fetch Announcements
                val jsonResponse = SupabaseManager.client
                    .from("announcements")
                    .select { order("created_at", order = Order.DESCENDING) }
                    .data
                val type = object : TypeToken<List<Announcement>>() {}.type
                // Mutable list needed for sorting later
                val dataList = Gson().fromJson<List<Announcement>>(jsonResponse, type).toMutableList()

                // B. Check if User is Logged In
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user

                if (currentUser != null) {
                    // C. Fetch User's Likes
                    val likesJson = SupabaseManager.client
                        .from("post_likes")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("post_id")) {
                            filter { eq("user_id", currentUser.id) }
                        }
                        .data
                    val likedPostIds = Gson().fromJson<List<LikeDTO>>(likesJson, object : TypeToken<List<LikeDTO>>() {}.type)
                        .map { it.post_id }
                        .toSet()

                    // D. Fetch User's Bookmarks
                    val bookmarksJson = SupabaseManager.client
                        .from("bookmarks")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("post_id")) {
                            filter { eq("user_id", currentUser.id) }
                        }
                        .data
                    val bookmarkedPostIds = Gson().fromJson<List<BookmarkDTO>>(bookmarksJson, object : TypeToken<List<BookmarkDTO>>() {}.type)
                        .map { it.post_id }
                        .toSet()

                    // E. Fetch User's Applications
                    val appsJson = SupabaseManager.client
                        .from("relief_applications")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("drive_id")) {
                            filter { eq("user_id", currentUser.id) }
                        }
                        .data
                    val appliedDriveIds = Gson().fromJson<List<ApplicationDTO>>(appsJson, object : TypeToken<List<ApplicationDTO>>() {}.type)
                        .map { it.drive_id }
                        .toSet()

                    // F. MATCH DATA
                    for (item in dataList) {
                        if (likedPostIds.contains(item.postId)) item.isLiked = true
                        if (bookmarkedPostIds.contains(item.postId)) item.isBookmarked = true
                        if (item.linkedDriveId != null && appliedDriveIds.contains(item.linkedDriveId)) item.isApplied = true
                    }

                    // ⭐ G. SORT: BOOKMARKS FIRST
                    dataList.sortWith(Comparator { a, b ->
                        when {
                            a.isBookmarked == b.isBookmarked -> 0
                            a.isBookmarked -> -1
                            else -> 1
                        }
                    })
                }

                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Unknown Error") }
            }
        }
    }

    // 3. APPLY TO DONATION DRIVE
    @JvmStatic
    fun applyToDrive(driveId: Long, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user
                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post { callback.onError("You must be logged in.") }
                    return@launch
                }
                val applicationData = ApplicationDTO(driveId, currentUser.id, "Pending")
                SupabaseManager.client.from("relief_applications").insert(applicationData)
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
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

    // 4. FETCH NOTIFICATIONS
    @JvmStatic
    fun fetchNotifications(callback: NotificationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user
                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post { callback.onError("User not logged in.") }
                    return@launch
                }
                val jsonResponse = SupabaseManager.client
                    .from("notifications")
                    .select {
                        filter { eq("user_id", currentUser.id) }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .data
                val type = object : TypeToken<List<NotificationItem>>() {}.type
                val dataList = Gson().fromJson<List<NotificationItem>>(jsonResponse, type)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Fetch failed") }
            }
        }
    }

    // 5. SUBMIT SUGGESTION
    @JvmStatic
    fun submitSuggestion(message: String, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user
                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post { callback.onError("You must be logged in.") }
                    return@launch
                }
                val suggestionData = SuggestionDTO(currentUser.id, message)
                SupabaseManager.client.from("suggestions").insert(suggestionData)
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to send suggestion") }
            }
        }
    }

    // 6. FETCH DONATION HISTORY
    @JvmStatic
    fun fetchDonationHistory(callback: DonationHistoryCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user
                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post { callback.onError("User not logged in.") }
                    return@launch
                }
                val jsonResponse = SupabaseManager.client
                    .from("donations")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, donation_items(*)")) {
                        filter { eq("donor_id", currentUser.id) }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .data
                val type = object : TypeToken<List<DonationHistoryItem>>() {}.type
                val dataList = Gson().fromJson<List<DonationHistoryItem>>(jsonResponse, type)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to load history") }
            }
        }
    }

    // 7. FETCH DASHBOARD DATA
    @JvmStatic
    fun fetchDashboardData(callback: DashboardCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val invResult = SupabaseManager.client.postgrest.rpc("get_inventory_stats").data
                val invJson = invResult ?: "[]"
                val invMap = Gson().fromJson<List<ChartDataInt>>(invJson, object : TypeToken<List<ChartDataInt>>() {}.type)
                    .associate { it.label to it.value }

                val areaResult = SupabaseManager.client.postgrest.rpc("get_affected_areas_stats").data
                val areaJson = areaResult ?: "[]"
                val areaMap = Gson().fromJson<List<ChartDataInt>>(areaJson, object : TypeToken<List<ChartDataInt>>() {}.type)
                    .associate { it.label to it.value }

                val donResult = SupabaseManager.client.postgrest.rpc("get_monthly_donations").data
                val donJson = donResult ?: "[]"
                val donMap = Gson().fromJson<List<ChartDataFloat>>(donJson, object : TypeToken<List<ChartDataFloat>>() {}.type)
                    .associate { it.label to it.value }

                val famResult = SupabaseManager.client.postgrest.rpc("get_monthly_registrations").data
                val famJson = famResult ?: "[]"
                val famMap = Gson().fromJson<List<ChartDataInt>>(famJson, object : TypeToken<List<ChartDataInt>>() {}.type)
                    .associate { it.label to it.value }

                val metricsResult = SupabaseManager.client.postgrest.rpc("get_dashboard_metrics").data
                val metricsJson = metricsResult ?: "[]"
                val metricsList = Gson().fromJson<List<DashboardMetrics>>(metricsJson, object : TypeToken<List<DashboardMetrics>>() {}.type)
                val metrics = if (metricsList.isNotEmpty()) metricsList[0] else DashboardMetrics(0, 0, 0)

                Handler(Looper.getMainLooper()).post {
                    callback.onDataLoaded(invMap, areaMap, donMap, famMap, metrics)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback.onError(e.message ?: "Failed to load dashboard")
                }
            }
        }
    }

    // ⭐ 8. ARCHIVE ACCOUNT (Soft Delete)
    @JvmStatic
    fun archiveAccount(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val currentUser = client.auth.currentUserOrNull()

        if (currentUser == null) {
            Handler(Looper.getMainLooper()).post { callback.onError("No user logged in.") }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A. Prepare Anonymized Data
                val updateData = mapOf(
                    "account_status" to "Archived",
                    "full_name" to "Deleted User",
                    "contact_number" to "",
                    "face_embedding" to null,
                    "id_image_url" to null,
                    "qr_code_url" to null,
                    "fcm_token" to null,
                    "type" to "Archived"
                )

                // B. Update Database
                client.from("profiles").update(updateData) {
                    filter { eq("id", currentUser.id) }
                }

                // C. Sign Out
                client.auth.signOut()

                Handler(Looper.getMainLooper()).post { callback.onSuccess() }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    callback.onError(e.message ?: "Failed to delete account")
                }
            }
        }
    }

    // 9. TOGGLE LIKE (With Duplicate Fix)
    @JvmStatic
    fun toggleLike(postId: Long, isLiked: Boolean, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user
                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post { callback.onError("User not logged in.") }
                    return@launch
                }

                if (isLiked) {
                    val dto = LikeDTO(currentUser.id, postId)
                    try {
                        SupabaseManager.client.from("post_likes").insert(dto)
                    } catch (e: Exception) {
                        if (e.message?.contains("duplicate key") == true || e.message?.contains("unique constraint") == true) {
                            Handler(Looper.getMainLooper()).post { callback.onSuccess() }
                            return@launch
                        } else {
                            throw e
                        }
                    }
                } else {
                    SupabaseManager.client.from("post_likes").delete {
                        filter {
                            eq("user_id", currentUser.id)
                            eq("post_id", postId)
                        }
                    }
                }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Like failed") }
            }
        }
    }

    // 10. TOGGLE BOOKMARK (With Duplicate Fix)
    @JvmStatic
    fun toggleBookmark(postId: Long, isBookmarked: Boolean, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user
                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post { callback.onError("User not logged in.") }
                    return@launch
                }

                if (isBookmarked) {
                    val dto = BookmarkDTO(currentUser.id, postId)
                    try {
                        SupabaseManager.client.from("bookmarks").insert(dto)
                    } catch (e: Exception) {
                        if (e.message?.contains("duplicate key") == true || e.message?.contains("unique constraint") == true) {
                            Handler(Looper.getMainLooper()).post { callback.onSuccess() }
                            return@launch
                        } else {
                            throw e
                        }
                    }
                } else {
                    SupabaseManager.client.from("bookmarks").delete {
                        filter {
                            eq("user_id", currentUser.id)
                            eq("post_id", postId)
                        }
                    }
                }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Bookmark failed") }
            }
        }
    }
}

// --- DTO CLASSES ---
@Serializable data class ApplicationDTO(val drive_id: Long, val user_id: String, val status: String)
@Serializable data class SuggestionDTO(val user_id: String, val message: String)
@Serializable data class ChartDataInt(val label: String, val value: Int)
@Serializable data class ChartDataFloat(val label: String, val value: Float)
@Serializable data class DashboardMetrics(val total_families: Int, val total_packs: Int, val total_affected: Int)
@Serializable data class LikeDTO(val user_id: String, val post_id: Long)
@Serializable data class BookmarkDTO(val user_id: String, val post_id: Long)
// Add ProfileDTO if not already in another file, just in case:
@Serializable data class ProfileDTO(
    val id: String,
    val email: String,
    val full_name: String,
    val contact_number: String,
    val house_number: String,
    val street: String,
    val barangay: String,
    val city: String,
    val province: String,
    val zip_code: String,
    val face_embedding: String?,
    val type: String
)