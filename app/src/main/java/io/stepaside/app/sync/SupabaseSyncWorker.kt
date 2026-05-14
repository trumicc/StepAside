package io.stepaside.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.stepaside.app.StepAsideApp
import io.stepaside.app.supabase
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyStepsSyncDto(
    val user_id: String,
    val date_str: String,
    val steps: Int,
    val goal_steps: Int,
    val goal_reached: Boolean,
)

@Serializable
data class LifetimeStatsSyncDto(
    val user_id: String,
    val total_steps: Long,
    val total_distance_meters: Float,
)

/**
 * Periodically syncs today's step row and lifetime stats to Supabase.
 *
 * Respects the user's opt-out: if `consent_data_share` is false, this worker
 * returns success without uploading anything. This is the core of our GDPR
 * compliance — consent must be honored throughout the sync path.
 */
class SupabaseSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Respect user consent. No consent → no upload. Period.
            val prefs = applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )
            val dataShareConsent = prefs.getBoolean(PREF_DATA_SHARE_CONSENT, false)
            if (!dataShareConsent) return Result.success()

            val user = supabase.auth.currentUserOrNull() ?: return Result.success()
            val userId = user.id
            val db = (applicationContext as StepAsideApp).database

            val today = LocalDate.now().toString()
            val todaySteps = db.dailyStepsDao().getByDate(today)
            if (todaySteps != null) {
                supabase.postgrest["daily_steps_sync"].upsert(
                    DailyStepsSyncDto(
                        user_id = userId,
                        date_str = todaySteps.dateStr,
                        steps = todaySteps.steps,
                        goal_steps = todaySteps.goalSteps,
                        goal_reached = todaySteps.goalReached,
                    ),
                )
            }

            val lifetime = db.lifetimeStatsDao().get()
            if (lifetime != null) {
                supabase.postgrest["lifetime_stats_sync"].upsert(
                    LifetimeStatsSyncDto(
                        user_id = userId,
                        total_steps = lifetime.totalSteps,
                        total_distance_meters = lifetime.totalDistanceMeters,
                    ),
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val PREFS_NAME = "stepaside_prefs"
        const val PREF_DATA_SHARE_CONSENT = "consent_data_share"
    }
}
