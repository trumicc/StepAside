package com.example.stepaside.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LifetimeStatsDao {

    @Query("SELECT * FROM lifetime_stats WHERE id = 1")
    suspend fun get(): LifetimeStats?

    @Query("SELECT * FROM lifetime_stats WHERE id = 1")
    fun getFlow(): Flow<LifetimeStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: LifetimeStats)

    @Query("UPDATE lifetime_stats SET totalSteps = totalSteps + :steps, totalDistanceMeters = totalDistanceMeters + :distance WHERE id = 1")
    suspend fun addSteps(steps: Long, distance: Float)
}