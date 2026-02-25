package com.example.dawnasyon_v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String? = null,
    val contact_number: String? = null,
    val email: String? = null,
    val current_evacuation_center: String? = null,

    // Address
    val house_number: String? = null,
    val street: String? = null,
    val barangay: String? = null,
    val city: String? = null,
    val province: String? = null,
    val zip_code: String? = null,

    // Security
    val id_image_url: String? = null,
    val qr_code_url: String? = null,

    // ⭐ REVERTED: Back to String to match your Cache and prevent Type Mismatch
    val face_embedding: String? = null,

    @SerialName("is_verified")
    val verified: Boolean? = false,

    @SerialName("avatar_name")
    val avatarName: String? = "avatar1",

    val type: String? = "Resident",
    val priority_score: Int? = 0,
    val risk_zone: String? = null,
    val account_status: String? = "Pending",
    val rejection_reason: String? = null,

    // ⭐ ADDED NOTES FIELD HERE TO SAVE ID MISMATCH WARNINGS
    val notes: String? = null
)