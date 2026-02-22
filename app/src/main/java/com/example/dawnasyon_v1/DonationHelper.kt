package com.example.dawnasyon_v1

import android.os.Handler
import android.os.Looper
import com.example.dawnasyon_v1.Summary_fragment.ItemForSummary // Import the item class
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

object DonationHelper {

    interface DonationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    // Main function called by Summary_fragment
    @JvmStatic
    fun submitDonation(
        refNumber: String,
        items: ArrayList<ItemForSummary>,
        type: String,
        itemDesc: String?, // ⭐ NEW: Accepts the Relief Pack contents
        amount: Double,
        isAnonymous: Boolean,
        callback: DonationCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get Current User
                val auth = SupabaseManager.client.pluginManager.getPlugin(io.github.jan.supabase.auth.Auth)
                val currentUser = auth.currentSessionOrNull()?.user

                if (currentUser == null) {
                    Handler(Looper.getMainLooper()).post {
                        callback.onError("You must be logged in.")
                    }
                    return@launch
                }

                // 2. Prepare Donation Data
                val donationData = DonationInsertDTO(
                    donor_id = currentUser.id,
                    type = type,
                    item_description = itemDesc, // ⭐ Maps directly to your DB column
                    amount = amount,
                    is_anonymous = isAnonymous,
                    status = "Pending",
                    reference_number = refNumber,
                    admin_notes = ""
                )

                // 3. Insert Donation & Get the new ID back
                val result = SupabaseManager.client
                    .from("donations")
                    .insert(donationData) {
                        select()
                    }
                    .decodeSingle<JsonElement>()

                // Extract the ID safely
                val donationId = result.jsonObject["donation_id"]?.jsonPrimitive?.long

                if (donationId == null) {
                    throw Exception("Failed to retrieve Donation ID")
                }

                // 4. Insert the Items (Rice, Canned Goods, etc.)
                val itemsToInsert = items.map { item ->
                    // Split "5 kg" into quantity (5) and unit (kg)
                    val (qty, unit) = parseQuantityUnit(item.quantityUnit)

                    DonationItemInsertDTO(
                        donation_id = donationId,
                        item_name = item.name,
                        quantity = qty,
                        unit = unit
                    )
                }

                SupabaseManager.client.from("donation_items").insert(itemsToInsert)

                // 5. Success
                Handler(Looper.getMainLooper()).post {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback.onError(e.message ?: "Donation failed")
                }
            }
        }
    }

    // Helper to split "5 kg" -> 5, "kg"
    private fun parseQuantityUnit(raw: String): Pair<Int, String> {
        try {
            val parts = raw.trim().split(" ")
            if (parts.isNotEmpty()) {
                val qty = parts[0].toIntOrNull() ?: 1
                val unit = if (parts.size > 1) parts[1] else ""
                return Pair(qty, unit)
            }
        } catch (e: Exception) {
            // Fallback
        }
        return Pair(1, "")
    }
}

// --- DTO CLASSES (For Inserting) ---

@Serializable
data class DonationInsertDTO(
    val donor_id: String,
    val type: String,
    val item_description: String?, // ⭐ NEW: Added to DTO to map to DB
    val amount: Double,
    val is_anonymous: Boolean,
    val status: String,
    val reference_number: String,
    val admin_notes: String?
)

@Serializable
data class DonationItemInsertDTO(
    val donation_id: Long,
    val item_name: String,
    val quantity: Int,
    val unit: String
)