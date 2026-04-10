package com.example.stepaside.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {

    @Query("SELECT * FROM daily_steps WHERE dateStr = :date")
    suspend fun getByDate(date: String): DailySteps?

    @Query("SELECT * FROM daily_steps ORDER BY dateStr DESC")
    fun getAllFlow(): Flow<List<DailySteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailySteps: DailySteps)

    @Query("UPDATE daily_steps SET steps = :steps, goalReached = :goalReached WHERE dateStr = :date")
    suspend fun updateSteps(date: String, steps: Int, goalReached: Boolean)
}