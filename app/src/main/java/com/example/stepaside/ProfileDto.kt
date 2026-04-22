package com.example.stepaside

import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpsert(
    val id: String,
    val display_name: String,
    val avatar_color: String,
    val height_cm: Int,
    val weight_kg: Float,
    val goal_steps: Int
)