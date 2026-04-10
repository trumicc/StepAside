package com.example.stepaside.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey
    val dateStr: String,
    val steps: Int = 0,
    val goalSteps: Int = 10000,
    val goalReached: Boolean = false
)