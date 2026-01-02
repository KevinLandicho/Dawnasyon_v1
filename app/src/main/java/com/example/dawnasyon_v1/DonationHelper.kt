package com.example.dawnasyon_v1

import android.util.Log
import com.example.dawnasyon_v1.Summary_fragment.ItemForSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// --- SUPABASE IMPORTS ---
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

// --- DTOs ---
@Serializable
data class DonationRequest(
    val status: String,
    val donor_id: String,
    val type: String,
    val amount: Double,
    val admin_notes: String,
    val is_anonymous: Boolean // <--- NEW FIELD
)

@Serializable
data class DonationItemRequest(
    val donation_id: Long,
    val item_name: String,
    val quantity: Int,
    val unit: String
)

@Serializable
data class DonationResponse(val donation_id: Long)

object DonationHelper {

    interface DonationCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    fun submitDonation(
        referenceNumber: String,
        items: ArrayList<ItemForSummary>,
        donationType: String,
        amountValue: Double,
        isAnonymous: Boolean, // <--- NEW PARAMETER
        callback: DonationCallback
    ) {
        val client = SupabaseManager.client
        val user = client.auth.currentUserOrNull()

        if (user == null) {
            callback.onError("User not logged in.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Create Donation Record with Anonymous Flag
                val donation = DonationRequest(
                    status = "Pending",
                    donor_id = user.id,
                    type = donationType,
                    amount = amountValue,
                    admin_notes = "Ref: $referenceNumber",
                    is_anonymous = isAnonymous // <--- SAVE IT HERE
                )

                val response = client.from("donations")
                    .insert(donation) { select() }
                    .decodeSingle<DonationResponse>()

                val newDonationId = response.donation_id

                // 2. Save Items
                if (items.isNotEmpty()) {
                    val dbItems = items.map { item ->
                        val parts = item.quantityUnit.split(" ", limit = 2)
                        val qty = parts.getOrNull(0)?.toIntOrNull() ?: 1
                        val unitStr = parts.getOrNull(1) ?: "PCS"

                        DonationItemRequest(
                            donation_id = newDonationId,
                            item_name = item.name,
                            quantity = qty,
                            unit = unitStr
                        )
                    }
                    client.from("donation_items").insert(dbItems)
                }

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                Log.e("DonationHelper", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "Unknown error occurred.")
                }
            }
        }
    }
}