package io.stepaside.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WalkSession): Long

    @Update
    suspend fun update(session: WalkSession)

    @Query("SELECT * FROM walk_sessions WHERE dateStr = :date ORDER BY startTime DESC")
    fun getSessionsByDate(date: String): Flow<List<WalkSession>>

    @Query("SELECT * FROM walk_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkSession>>

    @Query("SELECT * FROM walk_sessions WHERE id = :id")
    suspend fun getById(id: Long): WalkSession?

    @Query("SELECT * FROM walk_sessions WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<WalkSession?>
}
