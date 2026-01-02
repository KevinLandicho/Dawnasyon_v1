package com.example.dawnasyon_v1

import android.util.Log
import com.example.dawnasyon_v1.Summary_fragment.ItemForSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// --- SUPABASE IMPORTS (v3.0 / Kotlin 2.0) ---
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

// --- DATA CLASSES (Matches your Database Schema) ---
@Serializable
data class DonationRequest(
    val status: String,     // "Pending"
    val donor_id: String,   // User ID
    val type: String,       // "In-Kind" or "Cash"
    val amount: Double,     // 0.00 for goods
    val admin_notes: String // Stores Reference Number
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

    // --- MAIN SUBMIT FUNCTION ---
    fun submitDonation(
        referenceNumber: String,
        items: ArrayList<ItemForSummary>,
        donationType: String, // Pass "In-Kind" or "Cash"
        amountValue: Double,  // Pass 0.0 or actual amount
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
                // STEP 1: Insert the Main Donation Record
                val donation = DonationRequest(
                    status = "Pending",
                    donor_id = user.id,
                    type = donationType,
                    amount = amountValue,
                    admin_notes = "Ref: $referenceNumber"
                )

                // Insert into 'donations' and retrieve the new ID
                val response = client.from("donations")
                    .insert(donation) { select() }
                    .decodeSingle<DonationResponse>()

                val newDonationId = response.donation_id

                // STEP 2: Insert the Items (if any)
                if (items.isNotEmpty()) {
                    val dbItems = items.map { item ->
                        // Logic: Splits "2 Kilos" into "2" and "Kilos"
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
                    // Batch Insert into 'donation_items'
                    client.from("donation_items").insert(dbItems)
                }

                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                Log.e("DonationHelper", "Submission Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "An unknown error occurred.")
                }
            }
        }
    }
}