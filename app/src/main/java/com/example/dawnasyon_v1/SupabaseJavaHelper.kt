package com.example.dawnasyon_v1

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// --- CALLBACK INTERFACES ---

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

// ⭐ UPDATED CALLBACK: Added 'families' map ⭐
interface DashboardCallback {
    fun onDataLoaded(
        inventory: Map<String, Int>,
        areas: Map<String, Int>,
        donations: Map<String, Float>,
        families: Map<String, Int>, // <--- ADDED THIS
        metrics: DashboardMetrics
    )
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
                    .select { order("created_at", order = Order.DESCENDING) }
                    .data
                val type = object : TypeToken<List<Announcement>>() {}.type
                val dataList = Gson().fromJson<List<Announcement>>(jsonResponse, type)
                Handler(Looper.getMainLooper()).post { callback.onSuccess(dataList) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message ?: "Unknown Error") }
            }
        }
    }

    // 2. APPLY TO DONATION DRIVE
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

    // 3. FETCH NOTIFICATIONS
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

    // 4. SUBMIT SUGGESTION
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

    // 5. FETCH DONATION HISTORY
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

    // 6. FETCH DASHBOARD DATA
    @JvmStatic
    fun fetchDashboardData(callback: DashboardCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Inventory
                val invResult = SupabaseManager.client.postgrest.rpc("get_inventory_stats").data
                val invJson = invResult ?: "[]"
                val invMap = Gson().fromJson<List<ChartDataInt>>(invJson, object : TypeToken<List<ChartDataInt>>() {}.type)
                    .associate { it.label to it.value }

                // 2. Affected Areas
                val areaResult = SupabaseManager.client.postgrest.rpc("get_affected_areas_stats").data
                val areaJson = areaResult ?: "[]"
                val areaMap = Gson().fromJson<List<ChartDataInt>>(areaJson, object : TypeToken<List<ChartDataInt>>() {}.type)
                    .associate { it.label to it.value }

                // 3. Monthly Donations
                val donResult = SupabaseManager.client.postgrest.rpc("get_monthly_donations").data
                val donJson = donResult ?: "[]"
                val donMap = Gson().fromJson<List<ChartDataFloat>>(donJson, object : TypeToken<List<ChartDataFloat>>() {}.type)
                    .associate { it.label to it.value }

                // 4. ⭐ NEW: Family Registrations ⭐
                val famResult = SupabaseManager.client.postgrest.rpc("get_monthly_registrations").data
                val famJson = famResult ?: "[]"
                val famMap = Gson().fromJson<List<ChartDataInt>>(famJson, object : TypeToken<List<ChartDataInt>>() {}.type)
                    .associate { it.label to it.value }

                // 5. Key Metrics
                val metricsResult = SupabaseManager.client.postgrest.rpc("get_dashboard_metrics").data
                val metricsJson = metricsResult ?: "[]"
                val metricsList = Gson().fromJson<List<DashboardMetrics>>(metricsJson, object : TypeToken<List<DashboardMetrics>>() {}.type)
                val metrics = if (metricsList.isNotEmpty()) metricsList[0] else DashboardMetrics(0, 0, 0)

                Handler(Looper.getMainLooper()).post {
                    // Pass the new 'famMap' to the callback
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
}

// --- DTO CLASSES ---
@Serializable data class ApplicationDTO(val drive_id: Long, val user_id: String, val status: String)
@Serializable data class SuggestionDTO(val user_id: String, val message: String)
@Serializable data class ChartDataInt(val label: String, val value: Int)
@Serializable data class ChartDataFloat(val label: String, val value: Float)
@Serializable data class DashboardMetrics(val total_families: Int, val total_packs: Int, val total_affected: Int)