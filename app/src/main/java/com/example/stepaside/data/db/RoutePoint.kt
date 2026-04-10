package com.example.stepaside.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_points")
data class RoutePoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long
)