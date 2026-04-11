package com.example.stepaside

import android.app.Application
import androidx.work.*
import com.example.stepaside.data.db.AppDatabase
import com.example.stepaside.sync.SupabaseSyncWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import io.github.jan.supabase.auth.auth

class StepAsideApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Ladda sparad session vid start
        GlobalScope.launch {
            try {
                supabase.auth.loadFromStorage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        scheduleSyncWork()
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SupabaseSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "supabase_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}