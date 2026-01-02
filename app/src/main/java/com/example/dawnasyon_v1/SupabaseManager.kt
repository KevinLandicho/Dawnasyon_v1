package com.example.dawnasyon_v1

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
// Add these imports for the saving logic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.from

object SupabaseManager {

    private const val SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi" // Use your real key

    @JvmStatic
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth) {}
        install(Postgrest) {}
        install(Realtime) {}
        install(Storage) {}
    }

    // ⭐ ADD THIS NEW FUNCTION ⭐
    // This handles the difficult "Suspend" and "JSON" work automatically
    @JvmStatic
    fun saveFcmToken(token: String) {
        // Run in background (IO) so UI doesn't freeze
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = client.pluginManager.getPlugin(Auth)
                val userId = auth.currentSessionOrNull()?.user?.id

                if (userId != null) {
                    // Simple map update - no JSON string needed!
                    val updateData = mapOf("fcm_token" to token)

                    client.from("profiles").update(updateData) {
                        filter {
                            eq("id", userId)
                        }
                    }
                    Log.d("FCM", "Token saved via SupabaseManager!")
                } else {
                    Log.e("FCM", "User not logged in.")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error in SupabaseManager: ${e.message}")
            }
        }
    }
}