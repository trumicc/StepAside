package com.example.stepaside.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stepaside.StepAsideApp
import com.example.stepaside.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyStepsSyncDto(
    val user_id: String,
    val date_str: String,
    val steps: Int,
    val goal_steps: Int,
    val goal_reached: Boolean
)

@Serializable
data class LifetimeStatsSyncDto(
    val user_id: String,
    val total_steps: Long,
    val total_distance_meters: Float
)

class SupabaseSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val user = supabase.auth.currentUserOrNull() ?: return Result.success()
            val userId = user.id
            val db = (applicationContext as StepAsideApp).database

            // Sync today's steps
            val today = LocalDate.now().toString()
            val todaySteps = db.dailyStepsDao().getByDate(today)
            if (todaySteps != null) {
                supabase.postgrest["daily_steps_sync"].upsert(
                    DailyStepsSyncDto(
                        user_id = userId,
                        date_str = todaySteps.dateStr,
                        steps = todaySteps.steps,
                        goal_steps = todaySteps.goalSteps,
                        goal_reached = todaySteps.goalReached
                    )
                )
            }

            // Sync lifetime stats
            val lifetime = db.lifetimeStatsDao().get()
            if (lifetime != null) {
                supabase.postgrest["lifetime_stats_sync"].upsert(
                    LifetimeStatsSyncDto(
                        user_id = userId,
                        total_steps = lifetime.totalSteps,
                        total_distance_meters = lifetime.totalDistanceMeters
                    )
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}