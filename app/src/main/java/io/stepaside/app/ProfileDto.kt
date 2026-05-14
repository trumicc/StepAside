<<<<<<< Updated upstream:app/src/main/java/com/example/stepaside/ProfileDto.kt
package com.example.stepaside
=======
package io.stepaside.app
>>>>>>> Stashed changes:app/src/main/java/io/stepaside/app/ProfileDto.kt

import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpsert(
    val id: String,
    val display_name: String,
    val avatar_color: String,
    val height_cm: Int,
    val weight_kg: Float,
<<<<<<< Updated upstream:app/src/main/java/com/example/stepaside/ProfileDto.kt
    val goal_steps: Int
)
=======
    val goal_steps: Int,
)
>>>>>>> Stashed changes:app/src/main/java/io/stepaside/app/ProfileDto.kt
