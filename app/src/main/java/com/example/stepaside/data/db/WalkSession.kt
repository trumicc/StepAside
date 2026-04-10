package com.example.stepaside.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walk_sessions")
data class WalkSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val steps: Int = 0,
    val distanceMeters: Float = 0f,
    val elevationGainMeters: Float = 0f,
    val dateStr: String             // "2026-04-07"
)