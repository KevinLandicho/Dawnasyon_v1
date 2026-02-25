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
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmStatic

object SupabaseJavaHelper {

    private const val PREFS_NAME = "dawnasyon_cache"
    private val gson = Gson()

    // ====================================================
    // ⭐ SAFE CACHE SAVER
    // ====================================================
    private fun saveToCache(context: Context?, key: String, data: Any) {
        if (context == null) return
        try {
            val appContext = context.applicationContext
            val json = gson.toJson(data)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(key, json).apply()
        } catch (e: Exception) { Log.e("SupabaseCache", "Cache Error: ${e.message}") }
    }

    private fun <T> loadFromCache(context: Context?, key: String, type: java.lang.reflect.Type): T? {
        if (context == null) return null
        return try {
            val appContext = context.applicationContext
            val json = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, null)
            if (json != null) gson.fromJson(json, type) else null
        } catch (e: Exception) { null }
    }

    private fun runOnUi(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post { try { action() } catch (e: Exception) { Log.w("SupabaseSafe", "UI Error") } }
    }

    // INTERFACES
    interface SimpleCallback { fun onSuccess(); fun onError(message: String) }
    interface AddressCheckCallback { fun onResult(isDuplicate: Boolean); fun onError(message: String) }
    interface AnnouncementCallback { fun onSuccess(data: MutableList<Announcement>); fun onError(message: String) }
    interface ProfileCallback { fun onLoaded(profile: Profile?); fun onError(message: String) }
    interface DashboardCallback { fun onDataLoaded(inventory: MutableMap<String, Int>, areas: MutableMap<String, Int>, donations: MutableMap<String, Float>, families: MutableMap<String, Int>, metrics: DashboardMetrics, impact: MutableMap<String, Int>); fun onError(message: String) }
    interface ApplicationCallback { fun onSuccess(); fun onError(message: String) }
    interface NotificationCallback { fun onSuccess(data: MutableList<NotificationItem>); fun onError(message: String) }
    interface DonationHistoryCallback { fun onSuccess(data: MutableList<DonationHistoryItem>); fun onError(message: String) }
    interface RegistrationCallback { fun onSuccess(); fun onError(message: String) }
    interface ProfileUpdateCallback { fun onSuccess(); fun onError(message: String) }
    interface TrackingCallback { fun onLoaded(data: DonationTrackingDTO?) }
    interface ApplicationHistoryCallback { fun onLoaded(data: List<ApplicationHistoryDTO>); fun onError(message: String) }
    interface BrgyInfoCallback { fun onSuccess(info: BrgyInfoDTO); fun onError(message: String) }

    // ====================================================
    // ⭐ LOGIN FUNCTION
    // ====================================================
    @JvmStatic
    fun loginUser(email: String, pass: String, callback: RegistrationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                    this.email = email
                    this.password = pass
                }
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("Invalid login") == true) "Invalid email or password" else e.message ?: "Login failed"
                runOnUi { callback.onError(msg) }
            }
        }
    }

    // ====================================================
    // ⭐ LOGOUT FUNCTION
    // ====================================================
    @JvmStatic
    fun logoutUser(callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull()

                if (currentUser != null) {
                    try {
                        val updateData = mapOf("fcm_token" to null)
                        SupabaseManager.client.from("profiles").update(updateData) {
                            filter { eq("id", currentUser.id) }
                        }
                    } catch (e: Exception) {
                        Log.e("SupabaseLogout", "Failed to clear token: ${e.message}")
                    }
                }

                SupabaseManager.client.auth.signOut()
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) {
                try { SupabaseManager.client.auth.signOut() } catch (e2: Exception) {}
                runOnUi { callback.onSuccess() }
            }
        }
    }

    // ====================================================
    // ⭐ FORGOT PASSWORD
    // ====================================================
    @JvmStatic
    fun sendPasswordResetEmail(email: String, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.client.auth.resetPasswordForEmail(
                    email = email,
                    redirectUrl = "dawnasyon://reset-callback"
                )
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to send reset email"
                runOnUi { callback.onError(msg) }
            }
        }
    }

    // ====================================================
    // ⭐ FETCH DASHBOARD
    // ====================================================
    @JvmStatic
    fun fetchDashboardData(context: Context?, filterType: String, callback: DashboardCallback) {
        if (context == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val cacheKey = "cache_dashboard_$filterType"
            try {
                val cachedData: DashboardCacheDTO? = loadFromCache(context, cacheKey, DashboardCacheDTO::class.java)
                if (cachedData != null) runOnUi { callback.onDataLoaded(cachedData.inventory, cachedData.areas, cachedData.donations, cachedData.families, cachedData.metrics, cachedData.impact) }
            } catch (e: Exception) { }

            try {
                withTimeout(5000L) {
                    val params = mapOf("filter_type" to filterType)
                    val r = listOf(
                        async { SupabaseManager.client.postgrest.rpc("get_inventory_stats", params).data ?: "[]" },
                        async { SupabaseManager.client.postgrest.rpc("get_affected_areas_stats", params).data ?: "[]" },
                        async { SupabaseManager.client.postgrest.rpc("get_donation_impact_stats", params).data ?: "[]" },
                        async { SupabaseManager.client.postgrest.rpc("get_monthly_donations", params).data ?: "[]" },
                        async { SupabaseManager.client.postgrest.rpc("get_monthly_registrations", params).data ?: "[]" },
                        async { SupabaseManager.client.postgrest.rpc("get_dashboard_metrics_v2", params).data ?: "[]" }
                    ).awaitAll()

                    val invMap = gson.fromJson<List<ChartDataInt>>(r[0] as String, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                    val areaMap = gson.fromJson<List<ChartDataInt>>(r[1] as String, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                    val impactMap = gson.fromJson<List<ChartDataInt>>(r[2] as String, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                    val donMap = gson.fromJson<List<ChartDataFloat>>(r[3] as String, object : TypeToken<List<ChartDataFloat>>() {}.type).associate { it.label to it.value }.toMutableMap()
                    val famMap = gson.fromJson<List<ChartDataInt>>(r[4] as String, object : TypeToken<List<ChartDataInt>>() {}.type).associate { it.label to it.value }.toMutableMap()
                    val metricsList = gson.fromJson<List<DashboardMetrics>>(r[5] as String, object : TypeToken<List<DashboardMetrics>>() {}.type)
                    val metrics = if (metricsList.isNotEmpty()) metricsList[0] else DashboardMetrics(0, 0, 0)
                    val cacheData = DashboardCacheDTO(invMap, areaMap, donMap, famMap, metrics, impactMap)
                    saveToCache(context, cacheKey, cacheData)
                    runOnUi { callback.onDataLoaded(invMap, areaMap, donMap, famMap, metrics, impactMap) }
                }
            } catch (e: Exception) { runOnUi { callback.onError("Network slow or unavailable") } }
        }
    }

    // ====================================================
    // ⭐ FETCH PROFILE
    // ====================================================
    @JvmStatic
    fun fetchUserProfile(context: Context?, callback: ProfileCallback) {
        if (context == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            if (currentUser == null) { runOnUi { callback.onError("No user logged in") }; return@launch }
            val cacheKey = "cache_profile_${currentUser.id}"

            try {
                val cachedProfile = loadFromCache<Profile>(context, cacheKey, Profile::class.java)
                if (cachedProfile != null) { runOnUi { callback.onLoaded(cachedProfile) } }
            } catch (e: Exception) { }

            try {
                withTimeout(15000L) {
                    val result = SupabaseManager.client.from("profiles").select { filter { eq("id", currentUser.id) } }.decodeSingleOrNull<Profile>()
                    if (result != null) { saveToCache(context, cacheKey, result); runOnUi { callback.onLoaded(result) } }
                    else { runOnUi { callback.onError("Profile not found") } }
                }
            } catch (e: Exception) { runOnUi { callback.onError("Sync failed: ${e.message}") } }
        }
    }

    // ====================================================
    // ⭐ FETCH ANNOUNCEMENTS
    // ====================================================
    @JvmStatic
    fun fetchAnnouncements(context: Context?, callback: AnnouncementCallback) {
        if (context == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val type = object : TypeToken<MutableList<Announcement>>() {}.type
                val cachedData: MutableList<Announcement>? = loadFromCache(context, "cache_announcements", type)
                if (cachedData != null && cachedData.isNotEmpty()) { runOnUi { callback.onSuccess(cachedData) } }
            } catch (e: Exception) { }

            try {
                withTimeout(5000L) {
                    val announcementsDeferred = async {
                        SupabaseManager.client.from("announcements").select(
                            columns = Columns.raw("*, relief_drives(start_date, end_date, relief_item_list)")
                        ) {
                            order("created_at", order = Order.DESCENDING)
                        }.data
                    }

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

                        val appliedDriveIds = if (appsJson != null) {
                            gson.fromJson<List<ApplicationDTO>>(appsJson, object : TypeToken<List<ApplicationDTO>>() {}.type)
                                .mapNotNull { it.drive_id }
                                .toSet()
                        } else {
                            emptySet()
                        }

                        for (item in dataList) {
                            if (likedIds.contains(item.getPostId())) item.setLiked(true)
                            if (bookmarkedIds.contains(item.getPostId())) item.setBookmarked(true)
                            if (item.getLinkedDriveId() != null && appliedDriveIds.contains(item.getLinkedDriveId())) item.setApplied(true)
                        }
                    }
                    saveToCache(context, "cache_announcements", dataList)
                    runOnUi { callback.onSuccess(dataList) }
                }
            } catch (e: Exception) { runOnUi { callback.onError("Network error") } }
        }
    }

    // ====================================================
    // ⭐ FETCH APPLICATION HISTORY (Updated with Direct Join)
    // ====================================================
    @JvmStatic
    fun fetchUserApplications(context: Context?, callback: ApplicationHistoryCallback) {
        if (context == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            if (currentUser == null) return@launch
            try {
                // ⭐ JOIN: We pull relief_drives(name) AND relief_transactions(proof_photo)
                // This works because of the Foreign Key you added (application_id -> app_id)
                val result = SupabaseManager.client.from("relief_applications")
                    .select(columns = Columns.raw("status, created_at, drive_id, relief_drives(name), relief_transactions(proof_photo)")) {
                        filter { eq("user_id", currentUser.id) }
                        order("created_at", order = Order.DESCENDING)
                    }.data
                val type = object : TypeToken<List<ApplicationHistoryDTO>>() {}.type
                val historyList = gson.fromJson<List<ApplicationHistoryDTO>>(result, type) ?: emptyList()
                runOnUi { callback.onLoaded(historyList) }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Failed to load history") } }
        }
    }

    @JvmStatic
    fun validateHouseClaim(driveId: Long, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            if (currentUser == null) { runOnUi { callback.onError("No user") }; return@launch }
            try {
                val profile = SupabaseManager.client.from("profiles").select { filter { eq("id", currentUser.id) } }.decodeSingleOrNull<Profile>()
                if (profile == null) { runOnUi { callback.onError("Profile error") }; return@launch }

                val params = mapOf(
                    "check_drive_id" to driveId,
                    "check_house_no" to (profile.house_number ?: ""),
                    "check_street" to (profile.street ?: "")
                )
                val alreadyClaimed = SupabaseManager.client.postgrest.rpc("check_address_claim_status", params).decodeAs<Boolean>()
                runOnUi { if (alreadyClaimed) callback.onError("🚫 This household has already received a pack for this drive.") else callback.onSuccess() }
            } catch (e: Exception) { runOnUi { callback.onError("Validation failed: " + e.message) } }
        }
    }

    @JvmStatic
    fun uploadApplicationImage(bytes: ByteArray, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "app_${System.currentTimeMillis()}.jpg"
                val bucket = SupabaseManager.client.storage.from("application-images")
                bucket.upload(fileName, bytes)
                val publicUrl = bucket.publicUrl(fileName)
                runOnUi { callback(publicUrl) }
            } catch (e: Exception) {
                Log.e("Upload", "Error: ${e.message}")
                runOnUi { callback(null) }
            }
        }
    }

    @JvmStatic
    fun applyToDrive(driveId: Long, imageUrl: String?, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val applicationData = ApplicationDTO(driveId, currentUser.id, "Pending", imageUrl)
                SupabaseManager.client.from("relief_applications").insert(applicationData)
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) {
                if (e.message?.contains("duplicate") == true) runOnUi { callback.onSuccess() }
                else runOnUi { callback.onError(e.message ?: "Application failed") }
            }
        }
    }

    @JvmStatic
    fun toggleLike(postId: Long, isLiked: Boolean, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                if (isLiked) { try { SupabaseManager.client.from("post_likes").insert(LikeDTO(currentUser.id, postId)) } catch (e: Exception) {} }
                else { SupabaseManager.client.from("post_likes").delete { filter { eq("user_id", currentUser.id); eq("post_id", postId) } } }
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Like failed") } }
        }
    }

    @JvmStatic
    fun toggleBookmark(postId: Long, isBookmarked: Boolean, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                if (isBookmarked) { try { SupabaseManager.client.from("bookmarks").insert(BookmarkDTO(currentUser.id, postId)) } catch (e: Exception) {} }
                else { SupabaseManager.client.from("bookmarks").delete { filter { eq("user_id", currentUser.id); eq("post_id", postId) } } }
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Bookmark failed") } }
        }
    }

    @JvmStatic
    fun updateUserProfile(
        context: Context,
        fullName: String,
        contactNumber: String,
        province: String,
        city: String,
        barangay: String,
        street: String,
        avatarName: String,
        callback: ProfileUpdateCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch

                var lat: Double? = null
                var long: Double? = null

                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val fullAddress = "$street, $barangay, $city, Philippines"
                    val addresses = geocoder.getFromLocationName(fullAddress, 1)

                    if (addresses != null && addresses.isNotEmpty()) {
                        lat = addresses[0].latitude
                        long = addresses[0].longitude
                    }
                } catch (e: Exception) { Log.e("GeoProfile", "Geocoding failed") }

                val jsonPayload = buildJsonObject {
                    put("full_name", fullName)
                    put("contact_number", contactNumber)
                    put("province", province)
                    put("city", city)
                    put("barangay", barangay)
                    put("street", street)
                    put("avatar_name", avatarName)

                    if (lat != null && long != null) {
                        put("latitude", lat)
                        put("longitude", long)
                    }
                }

                SupabaseManager.client.from("profiles").update(jsonPayload) {
                    filter { eq("id", currentUser.id) }
                }

                runOnUi { callback.onSuccess() }
            } catch (e: Exception) {
                runOnUi { callback.onError(e.message ?: "Failed to update profile") }
            }
        }
    }

    @JvmStatic
    fun updatePriorityScore(userId: String, score: Int, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try { SupabaseManager.client.from("profiles").update(mapOf("priority_score" to score)) { filter { eq("id", userId) } }; runOnUi { callback.onSuccess() } }
            catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Failed to update priority score") } }
        }
    }

    @JvmStatic
    fun assignHouseholdProxy(headId: String, memberId: Long, callback: SimpleCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task1 = async { SupabaseManager.client.from("household_members").update(mapOf("is_authorized_proxy" to false)) { filter { eq("head_id", headId) } } }
                val task2 = async { SupabaseManager.client.from("household_members").update(mapOf("is_authorized_proxy" to true)) { filter { eq("member_id", memberId) } } }
                awaitAll(task1, task2)
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Failed to assign proxy") } }
        }
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
                    runOnUi { callback.onError("This Email is already registered.") }
                    return@launch
                }

                val memberCount = SupabaseManager.client.from("household_members").select {
                    filter { ilike("full_name", fullName) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0

                if (memberCount > 0) {
                    runOnUi { callback.onError("This name is already registered as a member in another household.") }
                    return@launch
                }

                val profileNameCount = SupabaseManager.client.from("profiles").select {
                    filter { ilike("full_name", fullName) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0

                if (profileNameCount > 0) {
                    runOnUi { callback.onError("This name is already registered as an account owner.") }
                    return@launch
                }

                runOnUi { callback.onSuccess() }
            } catch (e: Exception) {
                runOnUi { callback.onError(e.message ?: "Validation error") }
            }
        }
    }

    @JvmStatic
    fun checkAddressExists(houseNo: String, street: String, brgy: String, city: String, callback: AddressCheckCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = SupabaseManager.client.from("profiles").select(columns = Columns.list("house_number", "street")) { filter { ilike("barangay", brgy); ilike("city", city) } }.data
                val type = object : TypeToken<List<AddressDTO>>() {}.type
                val existingAddresses = gson.fromJson<List<AddressDTO>>(response, type) ?: emptyList()
                var duplicateFound = false
                for (item in existingAddresses) {
                    if (item.house_number.equals(houseNo, ignoreCase = true) && item.street.equals(street, ignoreCase = true)) {
                        duplicateFound = true; break
                    }
                }
                runOnUi { callback.onResult(duplicateFound) }
            } catch (e: Exception) {
                runOnUi { callback.onError(e.message ?: "Address validation error") }
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
                runOnUi { callback.onSuccess(dataList) }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Fetch failed") } }
        }
    }

    @JvmStatic
    fun submitSuggestion(message: String, callback: ApplicationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                val suggestionData = SuggestionDTO(currentUser.id, message)
                SupabaseManager.client.from("suggestions").insert(suggestionData)
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Failed to send suggestion") } }
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
                runOnUi { callback.onSuccess(dataList) }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Failed to load history") } }
        }
    }

    @JvmStatic
    fun fetchDonationTracking(donationId: Long, callback: TrackingCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseManager.client.from("donation_tracking_view").select { filter { eq("donation_id", donationId) }; limit(1) }.decodeSingleOrNull<DonationTrackingDTO>()
                runOnUi { callback.onLoaded(result) }
            } catch (e: Exception) { runOnUi { callback.onLoaded(null) } }
        }
    }

    @JvmStatic
    fun fetchBrgyInfo(callback: BrgyInfoCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseManager.client.from("barangay_info")
                    .select { filter { eq("id", 1) } }
                    .decodeSingleOrNull<BrgyInfoDTO>()

                if (result != null) {
                    runOnUi { callback.onSuccess(result) }
                } else {
                    runOnUi { callback.onError("No Barangay information found.") }
                }
            } catch (e: Exception) {
                runOnUi { callback.onError(e.message ?: "Failed to load Barangay Info") }
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
                runOnUi { callback.onSuccess() }
            } catch (e: Exception) { runOnUi { callback.onError(e.message ?: "Failed to delete account") } }
        }
    }
}

// --- DTO CLASSES ---
@Serializable data class ApplicationDTO(
    val drive_id: Long? = null,
    val user_id: String? = null,
    val status: String? = null,
    val image_url: String? = null
)

@Serializable data class DashboardCacheDTO(val inventory: MutableMap<String, Int>, val areas: MutableMap<String, Int>, val donations: MutableMap<String, Float>, val families: MutableMap<String, Int>, val metrics: DashboardMetrics, val impact: MutableMap<String, Int>)
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

// ⭐ DTO CLASSES FOR APPLICATION HISTORY
@Serializable
data class ApplicationHistoryDTO(
    val status: String,
    val created_at: String,
    val relief_drives: ReliefDriveNameDTO?,
    // Supabase returns joined rows as a List
    val relief_transactions: List<TransactionProofDTO>? = null
) {
    // ⭐ Helper method for Java code to easily get the photo URL
    fun getProof_photo(): String? = relief_transactions?.firstOrNull()?.proof_photo
}

@Serializable data class ReliefDriveNameDTO(val name: String)
@Serializable data class TransactionProofDTO(val proof_photo: String?)

@Serializable
data class BrgyInfoDTO(
    val office_address: String?,
    val office_hours: String?,
    val contact_numbers: String?,
    val email: String?,
    val punong_barangay: String?,
    val councilors: String?,
    val sk_chairperson: String?
)