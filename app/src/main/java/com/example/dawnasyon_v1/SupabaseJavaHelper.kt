package com.example.dawnasyon_v1

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

object SupabaseJavaHelper {

    private const val PREFS_NAME = "dawnasyon_cache"
    private val gson = Gson()

    // ====================================================
    // ⭐ CACHE HELPERS
    // ====================================================

    private fun saveToCache(context: Context, key: String, data: Any) {
        try {
            val json = gson.toJson(data)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, json)
                .apply()
        } catch (e: Exception) {
            Log.e("SupabaseCache", "Failed to save cache: ${e.message}")
        }
    }

    private fun <T> loadFromCache(context: Context, key: String, type: java.lang.reflect.Type): T? {
        try {
            val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, null)
            return if (json != null) gson.fromJson(json, type) else null
        } catch (e: Exception) {
            return null
        }
    }

    // ====================================================
    // ⭐ CALLBACK INTERFACES
    // ====================================================

    interface SimpleCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    interface AnnouncementCallback {
        fun onSuccess(data: MutableList<Announcement>)
        fun onError(message: String)
    }

    interface ProfileCallback {
        fun onLoaded(profile: Profile?)
        fun onError(message: String)
    }

    interface DashboardCallback {
        fun onDataLoaded(
            inventory: MutableMap<String, Int>,
            areas: MutableMap<String, Int>,
            donations: MutableMap<String, Float>,
            families: MutableMap<String, Int>,
            metrics: DashboardMetrics,
            impact: MutableMap<String, Int>
        )
        fun onError(message: String)
    }

    interface ApplicationCallback { fun onSuccess(); fun onError(message: String) }
    interface NotificationCallback { fun onSuccess(data: MutableList<NotificationItem>); fun onError(message: String) }
    interface DonationHistoryCallback { fun onSuccess(data: MutableList<DonationHistoryItem>); fun onError(message: String) }
    interface RegistrationCallback { fun onSuccess(); fun onError(message: String) }
    interface ProfileUpdateCallback { fun onSuccess(); fun onError(message: String) }
    interface TrackingCallback { fun onLoaded(data: DonationTrackingDTO?) }

    // ====================================================
    // ⭐ FETCH WITH CACHING: PROFILE
    // ====================================================

    @JvmStatic
    fun fetchUserProfile(context: Context, callback: ProfileCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            if (currentUser == null) {
                Handler(Looper.getMainLooper()).post { callback.onError("No user logged in") }
                return@launch
            }

            // 1. INSTANT LOAD: Check Cache
            val cacheKey = "cache_profile_${currentUser.id}"
            try {
                val cachedProfile = loadFromCache<Profile>(context, cacheKey, Profile::class.java)
                if (cachedProfile != null) {
                    Handler(Looper.getMainLooper()).post { callback.onLoaded(cachedProfile) }
                }
            } catch (e: Exception) { /* Ignore */ }

            // 2. NETWORK LOAD
            try {
                val result = SupabaseManager.client.from("profiles")
                    .select { filter { eq("id", currentUser.id) } }
                    .decodeSingleOrNull<Profile>()

                if (result != null) {
                    saveToCache(context, cacheKey, result)
                    Handler(Looper.getMainLooper()).post { callback.onLoaded(result) }
                } else {
                    // Only error if cache was also null
                    Handler(Looper.getMainLooper()).post { callback.onError("Profile not found") }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Network error") }
            }
        }
    }

    // ====================================================
    // ⭐ FETCH WITH CACHING: ANNOUNCEMENTS
    // ====================================================

    @JvmStatic
    fun fetchAnnouncements(context: Context, callback: AnnouncementCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. INSTANT LOAD: Check Cache
            try {
                val type = object : TypeToken<MutableList<Announcement>>() {}.type
                val cachedData: MutableList<Announcement>? = loadFromCache(context, "cache_announcements", type)
                if (cachedData != null && cachedData.isNotEmpty()) {
                    Handler(Looper.getMainLooper()).post { callback.onSuccess(cachedData) }
                }
            } catch (e: Exception) { /* Ignore */ }

            // 2. NETWORK LOAD
            try {
                val announcementsDeferred = async { SupabaseManager.client.from("announcements").select { order("created_at", order = Order.DESCENDING) }.data }
                val currentUser = SupabaseManager.client.auth.currentUserOrNull()

                val likesDeferred = if (currentUser != null) async { SupabaseManager.client.from("post_likes").select(columns = Columns.list("post_id")) { filter { eq("user_id", currentUser.id) } }.data } else null
                val bookmarksDeferred = if (currentUser != null) async { SupabaseManager.client.from("bookmarks").select(columns = Columns.list("post_id")) { filter { eq("user_id", currentUser.id) } }.data } else null
                val appsDeferred = if (currentUser != null) async { SupabaseManager.client.from("relief_applications").select(columns = Columns.list("drive_id")) { filter { eq("user_id", currentUser.id) } }.data } else null

                val jsonResponse = announcementsDeferred.await()
                val likesJson = likesDeferred?.await()
                val bookmarksJson = bookmarksDeferred?.await()
                val appsJson = appsDeferred?.await()

                val type = object : TypeToken<MutableList<Announcement>>() {}.type
                val dataList = gson.fromJson<MutableList<Announcement>>(jsonResponse, type) ?: mutableListOf()

                if (currentUser != null) {
                    val likedIds = if (likesJson != null) gson.fromJson<List<LikeDTO>>(likesJson, object : TypeToken<List<LikeDTO>>() {}.type).map { it.post_id }.toSet() else emptySet()
                    val bookmarkedIds = if (bookmarksJson != null) gson.fromJson<List<BookmarkDTO>>(bookmarksJson, object : TypeToken<List<BookmarkDTO>>() {}.type).map { it.post_id }.toSet() else emptySet()
                    val appliedDriveIds = if (appsJson != null) gson.fromJson<List<ApplicationDTO>>(appsJson, object : TypeToken<List<ApplicationDTO>>() {}.type).map { it.drive_id }.toSet() else emptySet()

                    for (item in dataList) {
                        if (likedIds.contains(item.getPostId())) item.setLiked(true)
                        if (bookmarkedIds.contains(item.getPostId())) item.setBookmarked(true)
                        if (item.getLinkedDriveId() != null && appliedDriveIds.contains(item.getLinkedDriveId())) item.setApplied(true)
                    }
                }

                saveToCache(context, "cache_announcements", dataList)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }

            } catch (e: Exception) {
                // If we have cached data, suppress the error toast to avoid annoyance
                // Otherwise report it
            }
        }
    }

    // ====================================================
    // ⭐ FETCH WITH CACHING: DASHBOARD
    // ====================================================

    @JvmStatic
    fun fetchDashboardData(context: Context, filterType: String, callback: DashboardCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val cacheKey = "cache_dashboard_$filterType"

            // 1. INSTANT LOAD
            try {
                val cachedData: DashboardCacheDTO? = loadFromCache(context, cacheKey, DashboardCacheDTO::class.java)
                if (cachedData != null) {
                    Handler(Looper.getMainLooper()).post {
                        callback.onDataLoaded(cachedData.inventory, cachedData.areas, cachedData.donations, cachedData.families, cachedData.metrics, cachedData.impact)
                    }
                }
            } catch (e: Exception) { /* Ignore */ }

            // 2. NETWORK LOAD
            try {
                val params = mapOf("filter_type" to filterType)
                val invDeferred = async { SupabaseManager.client.postgrest.rpc("get_inventory_stats", params).data ?: "[]" }
                val areaDeferred = async { SupabaseManager.client.postgrest.rpc("get_affected_areas_stats", params).data ?: "[]" }
                val impactDeferred = async { SupabaseManager.client.postgrest.rpc("get_donation_impact_stats", params).data ?: "[]" }
                val donDeferred = async { SupabaseManager.client.postgrest.rpc("get_monthly_donations", params).data ?: "[]" }
                val famDeferred = async { SupabaseManager.client.postgrest.rpc("get_monthly_registrations", params).data ?: "[]" }
                val metricsDeferred = async { SupabaseManager.client.postgrest.rpc("get_dashboard_metrics_v2", params).data ?: "[]" }

                val invResult = invDeferred.await()
                val areaResult = areaDeferred.await()
                val impactResult = impactDeferred.await()
                val donResult = donDeferred.await()
                val famResult = famDeferred.await()
                val metricsResult = metricsDeferred.await()

                val invMap = gson.fromJson<List<ChartDataInt>>(invResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                val areaMap = gson.fromJson<List<ChartDataInt>>(areaResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                val impactMap = gson.fromJson<List<ChartDataInt>>(impactResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                val donMap = gson.fromJson<List<ChartDataFloat>>(donResult, object : TypeToken<List<ChartDataFloat>>() {}.type).associate { it.label to it.value }.toMutableMap()
                val famMap = gson.fromJson<List<ChartDataInt>>(famResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                val metricsList = gson.fromJson<List<DashboardMetrics>>(metricsResult, object : TypeToken<List<DashboardMetrics>>() {}.type)
                val metrics = if (metricsList.isNotEmpty()) metricsList[0] else DashboardMetrics(0, 0, 0)

                val cacheData = DashboardCacheDTO(invMap, areaMap, donMap, famMap, metrics, impactMap)
                saveToCache(context, cacheKey, cacheData)

                Handler(Looper.getMainLooper()).post {
                    callback.onDataLoaded(invMap, areaMap, donMap, famMap, metrics, impactMap)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError("Dashboard Error: " + e.message) }
            }
        }
    }

    // ====================================================
    // ⭐ PROXY ASSIGNMENT & OTHERS
    // ====================================================

    @JvmStatic
    fun assignHouseholdProxy(headId: String, memberId: Long, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task1 = async { SupabaseManager.client.from("household_members").update(mapOf("is_authorized_proxy" to false)) { filter { eq("head_id", headId) } } }
                val task2 = async { SupabaseManager.client.from("household_members").update(mapOf("is_authorized_proxy" to true)) { filter { eq("member_id", memberId) } } }
                awaitAll(task1, task2)
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to assign proxy") }
            }
        }
    }

    @JvmStatic
    fun checkUserExists(fullName: String, email: String, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emailCount = SupabaseManager.client.from("profiles").select { filter { eq("email", email) }; count(io.github.jan.supabase.postgrest.query.Count.EXACT) }.countOrNull() ?: 0
                if (emailCount > 0) {
                    Handler(Looper.getMainLooper()).post { callback.onError("This Email is already registered.") }
                    return@launch
                }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Validation error") }
            }
        }
    }

    @JvmStatic
    fun checkAddressExists(houseNo: String, street: String, brgy: String, city: String, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = SupabaseManager.client.from("profiles").select(columns = Columns.list("house_number", "street")) { filter { ilike("barangay", brgy); ilike("city", city); eq("type", "Resident") } }.data
                val type = object : TypeToken<List<AddressDTO>>() {}.type
                val existingAddresses = gson.fromJson<List<AddressDTO>>(response, type) ?: emptyList()

                var duplicateFound = false
                for (item in existingAddresses) {
                    if (item.house_number.equals(houseNo, ignoreCase = true) && item.street.equals(street, ignoreCase = true)) {
                        duplicateFound = true; break
                    }
                }
                Handler(Looper.getMainLooper()).post { if (duplicateFound) callback.onError("Address already registered") else callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Address validation error") }
            }
        }
    }

    @JvmStatic
    fun fetchNotifications(callback: NotificationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val jsonResponse = SupabaseManager.client.from("notifications").select { filter { eq("user_id", currentUser.id) }; order("created_at", order = Order.DESCENDING) }.data
                val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
                val dataList = gson.fromJson<MutableList<NotificationItem>>(jsonResponse, type)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Fetch failed") }
            }
        }
    }

    @JvmStatic
    fun submitSuggestion(message: String, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val suggestionData = SuggestionDTO(currentUser.id, message)
                SupabaseManager.client.from("suggestions").insert(suggestionData)
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to send suggestion") }
            }
        }
    }

    @JvmStatic
    fun fetchDonationHistory(callback: DonationHistoryCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val jsonResponse = SupabaseManager.client.from("donations").select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, donation_items(*)")) { filter { eq("donor_id", currentUser.id) }; order("created_at", order = Order.DESCENDING) }.data
                val type = object : TypeToken<MutableList<DonationHistoryItem>>() {}.type
                val dataList = gson.fromJson<MutableList<DonationHistoryItem>>(jsonResponse, type)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to load history") }
            }
        }
    }

    @JvmStatic
    fun fetchDonationTracking(donationId: Long, callback: TrackingCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseManager.client.from("donation_tracking_view").select { filter { eq("donation_id", donationId) }; limit(1) }.decodeSingleOrNull<DonationTrackingDTO>()
                Handler(Looper.getMainLooper()).post { callback.onLoaded(result) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onLoaded(null) }
            }
        }
    }

    @JvmStatic
    fun applyToDrive(driveId: Long, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val applicationData = ApplicationDTO(driveId, currentUser.id, "Pending")
                SupabaseManager.client.from("relief_applications").insert(applicationData)
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                if (e.message?.contains("duplicate") == true) Handler(Looper.getMainLooper()).post { callback.onSuccess() }
                else Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Application failed") }
            }
        }
    }

    @JvmStatic
    fun toggleLike(postId: Long, isLiked: Boolean, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                if (isLiked) {
                    try { SupabaseManager.client.from("post_likes").insert(LikeDTO(currentUser.id, postId)) } catch (e: Exception) {}
                } else {
                    SupabaseManager.client.from("post_likes").delete { filter { eq("user_id", currentUser.id); eq("post_id", postId) } }
                }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Like failed") }
            }
        }
    }

    @JvmStatic
    fun toggleBookmark(postId: Long, isBookmarked: Boolean, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                if (isBookmarked) {
                    try { SupabaseManager.client.from("bookmarks").insert(BookmarkDTO(currentUser.id, postId)) } catch (e: Exception) {}
                } else {
                    SupabaseManager.client.from("bookmarks").delete { filter { eq("user_id", currentUser.id); eq("post_id", postId) } }
                }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Bookmark failed") }
            }
        }
    }

    @JvmStatic
    fun updateUserProfile(fullName: String, contactNumber: String, province: String, city: String, barangay: String, street: String, avatarName: String, callback: ProfileUpdateCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val updateData = mapOf("full_name" to fullName, "contact_number" to contactNumber, "province" to province, "city" to city, "barangay" to barangay, "street" to street, "avatar_name" to avatarName)
                SupabaseManager.client.from("profiles").update(updateData) { filter { eq("id", currentUser.id) } }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to update profile") }
            }
        }
    }

    @JvmStatic
    fun updatePriorityScore(userId: String, score: Int, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.from("profiles").update(mapOf("priority_score" to score)) { filter { eq("id", userId) } }
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to update priority score") }
            }
        }
    }

    @JvmStatic
    fun archiveAccount(callback: RegistrationCallback) {
        val client = SupabaseManager.client
        val currentUser = client.auth.currentUserOrNull() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf("account_status" to "Archived", "full_name" to "Deleted User", "contact_number" to "", "face_embedding" to null, "id_image_url" to null, "qr_code_url" to null, "fcm_token" to null, "type" to "Archived")
                client.from("profiles").update(updateData) { filter { eq("id", currentUser.id) } }
                client.auth.signOut()
                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to delete account") }
            }
        }
    }
}

// --- DTO CLASSES ---

@Serializable data class DashboardCacheDTO(val inventory: MutableMap<String, Int>, val areas: MutableMap<String, Int>, val donations: MutableMap<String, Float>, val families: MutableMap<String, Int>, val metrics: DashboardMetrics, val impact: MutableMap<String, Int>)
@Serializable data class ApplicationDTO(val drive_id: Long, val user_id: String, val status: String)
@Serializable data class SuggestionDTO(val user_id: String, val message: String)
@Serializable data class ChartDataInt(val label: String, val value: Int)
@Serializable data class ChartDataFloat(val label: String, val value: Float)
@Serializable data class DashboardMetrics(val total_families: Int, val total_packs: Int, val total_affected: Int, val active_users: Int = 0, val total_users: Int = 0)
@Serializable data class LikeDTO(val user_id: String, val post_id: Long)
@Serializable data class BookmarkDTO(val user_id: String, val post_id: Long)
@Serializable data class AddressDTO(val house_number: String, val street: String)
@Serializable data class NameDTO(val full_name: String)
@Serializable data class ProfileDTO(val id: String, val email: String, val full_name: String, val contact_number: String, val house_number: String, val street: String, val barangay: String, val city: String, val province: String, val zip_code: String, val face_embedding: String?, val type: String, val avatar_name: String?)
@Serializable data class DonationTrackingDTO(val donation_id: Long, val donation_status: String?, val donation_date: String?, val inventory_status: String?, val quantity_on_hand: Int?, val date_claimed: String?, val batch_name: String?)