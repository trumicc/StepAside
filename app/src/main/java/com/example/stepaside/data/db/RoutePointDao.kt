package com.example.stepaside.data.db

import androidx.room.*

@Dao
interface RoutePointDao {

    @Insert
    suspend fun insert(point: RoutePoint)

    @Insert
    suspend fun insertAll(points: List<RoutePoint>)

    @Query("SELECT * FROM route_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<RoutePoint>

    @Query("DELETE FROM route_points WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}