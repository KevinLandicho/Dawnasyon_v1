package com.example.dawnasyon_v1

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
data class HouseholdMember(
    // ⭐ @get:JvmName fixes the "Ambiguous method call" and "cannot find symbol" errors
    @get:JvmName("getMember_id")
    val member_id: Long = 0,

    @get:JvmName("getHead_id")
    val head_id: String? = null,

    @get:JvmName("getFull_name")
    val full_name: String? = null,

    @get:JvmName("getRelation")
    val relation: String? = null,

    @get:JvmName("getAge")
    val age: Int? = 0,

    @get:JvmName("getGender")
    val gender: String? = null,

    @get:JvmName("getIs_registered_census")
    val is_registered_census: Boolean? = false,

    @get:JvmName("getIs_authorized_proxy")
    val is_authorized_proxy: Boolean? = false
)