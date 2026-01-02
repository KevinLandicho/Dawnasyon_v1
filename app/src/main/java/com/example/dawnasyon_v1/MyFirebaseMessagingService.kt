package com.example.dawnasyon_v1

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // This runs automatically whenever Google generates a new token for this phone
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token generated: $token")
        saveTokenToSupabase(token)
    }

    // This runs if a notification arrives while the app is OPEN
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: ${message.notification?.body}")
    }

    private fun saveTokenToSupabase(token: String) {
        val client = SupabaseManager.client
        // Check if user is logged in
        val user = client.auth.currentUserOrNull()

        if (user != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Update the 'fcm_token' column in Supabase
                    client.from("profiles").update(
                        { set("fcm_token", token) }
                    ) {
                        filter { eq("id", user.id) }
                    }
                    Log.d("FCM", "Token successfully saved to Supabase!")
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to save token: ${e.message}")
                }
            }
        }
    }
}