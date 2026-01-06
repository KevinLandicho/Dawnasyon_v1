package com.example.dawnasyon_v1

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,

    // ⭐ Adding '? = null' makes these optional.
    // If the database sends nothing, the app won't crash anymore.
    val full_name: String? = null,
    val contact_number: String? = null,
    val email: String? = null,
    val house_number: String? = null,
    val street: String? = null,
    val barangay: String? = null,
    val city: String? = null,
    val province: String? = null,
    val zip_code: String? = null,
    val id_image_url: String? = null,
    val qr_code_url: String? = null, // ⭐ Add this new field
    val face_embedding: String? = null
)