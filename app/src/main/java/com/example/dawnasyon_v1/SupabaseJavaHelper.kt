package com.example.dawnasyon_v1

import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

object SupabaseJavaHelper {

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

    interface ApplicationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    interface NotificationCallback {
        fun onSuccess(data: MutableList<NotificationItem>)
        fun onError(message: String)
    }

    interface DonationHistoryCallback {
        fun onSuccess(data: MutableList<DonationHistoryItem>)
        fun onError(message: String)
    }

    interface DashboardCallback {
        fun onDataLoaded(
            inventory: MutableMap<String, Int>,
            areas: MutableMap<String, Int>,
            donations: MutableMap<String, Float>,
            families: MutableMap<String, Int>,
            metrics: DashboardMetrics
        )
        fun onError(message: String)
    }

    interface RegistrationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    interface ProfileUpdateCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    // ====================================================
    // ⭐ PROXY ASSIGNMENT
    // ====================================================

    @JvmStatic
    fun assignHouseholdProxy(headId: String, memberId: Long, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Reset ALL members of this head to false
                SupabaseManager.client.from("household_members").update(mapOf("is_authorized_proxy" to false)) {
                    filter { eq("head_id", headId) }
                }

                // 2. Set the selected member to true
                SupabaseManager.client.from("household_members").update(mapOf("is_authorized_proxy" to true)) {
                    filter { eq("member_id", memberId) }
                }

                Handler(Looper.getMainLooper()).post { callback.onSuccess() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to assign proxy") }
            }
        }
    }

    // ====================================================
    // ⭐ NORMALIZATION & VALIDATION
    // ====================================================

    private fun normalizeName(input: String): String {
        return input.lowercase().replace(".", "").replace(",", "").replace("-", "").replace("ñ", "n").replace("\\s+".toRegex(), "").trim()
    }

    private fun normalizeStreet(input: String): String {
        return input.lowercase().replace(".", "").replace(",", "").replace(" street", "").replace(" str", "").replace(" st", "").replace(" avenue", "").replace(" ave", "").replace(" road", "").replace(" rd", "").replace(" boulevard", "").replace(" blvd", "").replace("\\s+".toRegex(), "").trim()
    }

    private fun normalizeHouseNo(input: String): String {
        return input.lowercase().replace("\\s+".toRegex(), "").trim()
    }

    @JvmStatic
    fun checkUserExists(fullName: String, email: String, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emailCount = SupabaseManager.client.from("profiles").select {
                    filter { eq("email", email) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0

                if (emailCount > 0) {
                    Handler(Looper.getMainLooper()).post { callback.onError("This Email is already registered.") }
                    return@launch
                }

                val profilesResponse = SupabaseManager.client.from("profiles").select(columns = Columns.list("full_name")).data
                val membersResponse = SupabaseManager.client.from("household_members").select(columns = Columns.list("full_name")).data

                val type = object : TypeToken<List<NameDTO>>() {}.type
                val existingProfiles = Gson().fromJson<List<NameDTO>>(profilesResponse, type) ?: emptyList()
                val existingMembers = Gson().fromJson<List<NameDTO>>(membersResponse, type) ?: emptyList()

                val inputNameNorm = normalizeName(fullName)
                var duplicateFound = false
                var duplicateSource = ""

                for (item in existingProfiles) {
                    if (normalizeName(item.full_name) == inputNameNorm) {
                        duplicateFound = true; duplicateSource = "User/Head"; break
                    }
                }
                if (!duplicateFound) {
                    for (item in existingMembers) {
                        if (normalizeName(item.full_name) == inputNameNorm) {
                            duplicateFound = true; duplicateSource = "Household Member"; break
                        }
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    if (duplicateFound) callback.onError("This Name is already registered as a $duplicateSource.")
                    else callback.onSuccess()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Validation error") }
            }
        }
    }

    @JvmStatic
    fun checkAddressExists(houseNo: String, street: String, brgy: String, city: String, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = SupabaseManager.client.from("profiles").select(columns = Columns.list("house_number", "street")) {
                    filter { ilike("barangay", brgy); ilike("city", city); eq("type", "Resident") }
                }.data
                val type = object : TypeToken<List<AddressDTO>>() {}.type
                val existingAddresses = Gson().fromJson<List<AddressDTO>>(response, type) ?: emptyList()

                val inputHouseNorm = normalizeHouseNo(houseNo)
                val inputStreetNorm = normalizeStreet(street)
                var duplicateFound = false

                for (item in existingAddresses) {
                    if (normalizeHouseNo(item.house_number) == inputHouseNorm && normalizeStreet(item.street) == inputStreetNorm) {
                        duplicateFound = true; break
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    if (duplicateFound) callback.onError("This address ($houseNo $street) is already registered.")
                    else callback.onSuccess()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Address validation error") }
            }
        }
    }

    // ====================================================
    // ⭐ HELPER FUNCTIONS
    // ====================================================

    @JvmStatic
    fun fetchAnnouncements(callback: AnnouncementCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonResponse = SupabaseManager.client.from("announcements").select { order("created_at", order = Order.DESCENDING) }.data
                val type = object : TypeToken<MutableList<Announcement>>() {}.type
                val dataList = Gson().fromJson<MutableList<Announcement>>(jsonResponse, type) ?: mutableListOf()

                val currentUser = SupabaseManager.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val likesJson = SupabaseManager.client.from("post_likes").select(columns = Columns.list("post_id")) { filter { eq("user_id", currentUser.id) } }.data
                    val likedIds = Gson().fromJson<List<LikeDTO>>(likesJson, object : TypeToken<List<LikeDTO>>() {}.type).map { it.post_id }.toSet()
                    val bookmarksJson = SupabaseManager.client.from("bookmarks").select(columns = Columns.list("post_id")) { filter { eq("user_id", currentUser.id) } }.data
                    val bookmarkedIds = Gson().fromJson<List<BookmarkDTO>>(bookmarksJson, object : TypeToken<List<BookmarkDTO>>() {}.type).map { it.post_id }.toSet()
                    val appsJson = SupabaseManager.client.from("relief_applications").select(columns = Columns.list("drive_id")) { filter { eq("user_id", currentUser.id) } }.data
                    val appliedDriveIds = Gson().fromJson<List<ApplicationDTO>>(appsJson, object : TypeToken<List<ApplicationDTO>>() {}.type).map { it.drive_id }.toSet()

                    for (item in dataList) {
                        if (likedIds.contains(item.postId)) item.isLiked = true
                        if (bookmarkedIds.contains(item.postId)) item.isBookmarked = true
                        if (item.linkedDriveId != null && appliedDriveIds.contains(item.linkedDriveId)) item.isApplied = true
                    }
                }
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Unknown Error") }
            }
        }
    }

    @JvmStatic
    fun fetchDashboardData(filterType: String, callback: DashboardCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = mapOf("filter_type" to filterType)
                // ⭐ FIXED VARIABLE NAMES
                val invResult = SupabaseManager.client.postgrest.rpc("get_inventory_stats", params).data ?: "[]"
                val invMap = Gson().fromJson<List<ChartDataInt>>(invResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()

                val areaResult = SupabaseManager.client.postgrest.rpc("get_affected_areas_stats", params).data ?: "[]"
                val areaMap = Gson().fromJson<List<ChartDataInt>>(areaResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()

                val donResult = SupabaseManager.client.postgrest.rpc("get_monthly_donations", params).data ?: "[]"
                val donMap = Gson().fromJson<List<ChartDataFloat>>(donResult, object : TypeToken<List<ChartDataFloat>>() {}.type).associate { it.label to it.value }.toMutableMap()

                val famResult = SupabaseManager.client.postgrest.rpc("get_monthly_registrations", params).data ?: "[]"
                val famMap = Gson().fromJson<List<ChartDataInt>>(famResult, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()

                val metricsResult = SupabaseManager.client.postgrest.rpc("get_dashboard_metrics", params).data ?: "[]"
                val metricsList = Gson().fromJson<List<DashboardMetrics>>(metricsResult, object : TypeToken<List<DashboardMetrics>>() {}.type)
                val metrics = if (metricsList.isNotEmpty()) metricsList[0] else DashboardMetrics(0, 0, 0)

                Handler(Looper.getMainLooper()).post { callback.onDataLoaded(invMap, areaMap, donMap, famMap, metrics) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError("Dashboard Error: " + e.message) }
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
                val dataList = Gson().fromJson<MutableList<NotificationItem>>(jsonResponse, type)
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
                val dataList = Gson().fromJson<MutableList<DonationHistoryItem>>(jsonResponse, type)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Failed to load history") }
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
@Serializable data class ApplicationDTO(val drive_id: Long, val user_id: String, val status: String)
@Serializable data class SuggestionDTO(val user_id: String, val message: String)
@Serializable data class ChartDataInt(val label: String, val value: Int)
@Serializable data class ChartDataFloat(val label: String, val value: Float)
@Serializable data class DashboardMetrics(val total_families: Int, val total_packs: Int, val total_affected: Int)
@Serializable data class LikeDTO(val user_id: String, val post_id: Long)
@Serializable data class BookmarkDTO(val user_id: String, val post_id: Long)
@Serializable data class AddressDTO(val house_number: String, val street: String)
@Serializable data class NameDTO(val full_name: String)
@Serializable data class ProfileDTO(val id: String, val email: String, val full_name: String, val contact_number: String, val house_number: String, val street: String, val barangay: String, val city: String, val province: String, val zip_code: String, val face_embedding: String?, val type: String, val avatar_name: String?)