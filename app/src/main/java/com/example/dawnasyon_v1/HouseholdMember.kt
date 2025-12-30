package com.example.dawnasyon_v1

import kotlinx.serialization.Serializable

@Serializable
data class HouseholdMember(
    val head_id: String? = null,
    val full_name: String,
    val relation: String,
    val age: Int,
    val gender: String,
    val is_registered_census: Boolean =true, // This is FALSE in your DB
    val is_authorized_proxy: Boolean = false
) {
    // ⭐ ADD THIS so Java can read the boolean easily
    fun getCensusStatus(): Boolean {
        return is_registered_census
    }
}