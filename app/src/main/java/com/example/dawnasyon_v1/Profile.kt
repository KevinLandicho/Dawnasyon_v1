package com.example.dawnasyon_v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String? = null,
    val contact_number: String? = null,
    val email: String? = null,

    // ⭐ CRITICAL: This generates 'getCurrent_evacuation_center()' for Java
    val current_evacuation_center: String? = null,

    // Address Fields
    val house_number: String? = null,
    val street: String? = null,
    val barangay: String? = null,
    val city: String? = null,
    val province: String? = null,
    val zip_code: String? = null,

    // Security & Verification
    val id_image_url: String? = null,
    val qr_code_url: String? = null,
    val face_embedding: String? = null,

    // ⭐ Mapped Fields
    @SerialName("is_verified")
    val verified: Boolean? = false,

    @SerialName("avatar_name")
    val avatarName: String? = "avatar1",

    val type: String? = "Resident",

    // ⭐ NEW FIELDS (Matches your Database Table)
    val priority_score: Int? = 0,
    val risk_zone: String? = null,
    val account_status: String? = "Pending",
    val rejection_reason: String? = null
)