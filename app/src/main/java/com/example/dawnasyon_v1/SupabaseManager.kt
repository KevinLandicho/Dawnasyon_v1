package com.example.dawnasyon_v1

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage // ⭐ Import this

object SupabaseManager {

    private const val SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi"

    @JvmStatic
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth) {}
        install(Postgrest) {}
        install(Realtime) {}
        install(Storage) {} // ⭐ ADD THIS LINE
    }
}