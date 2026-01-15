package com.example.dawnasyon_v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
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
    val qr_code_url: String? = null,
    val face_embedding: String? = null,

    // ⭐ FIX: We rename it to 'verified' for cleaner Java access
    // @SerialName ensures it still reads 'is_verified' from Supabase
    @SerialName("is_verified")
    val verified: Boolean? = false
)