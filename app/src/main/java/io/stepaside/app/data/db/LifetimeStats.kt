package io.stepaside.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lifetime_stats")
data class LifetimeStats(
    @PrimaryKey
    val id: Int = 1,                        // always a single row
    val totalSteps: Long = 0L,
    val totalDistanceMeters: Float = 0f,
    val totalElevationGainMeters: Float = 0f,
    val installDate: Long = System.currentTimeMillis(),
)
