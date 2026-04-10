package com.example.stepaside

import android.app.Application
import androidx.work.*
import com.example.stepaside.data.db.AppDatabase
import com.example.stepaside.sync.SupabaseSyncWorker
import java.util.concurrent.TimeUnit

class StepAsideApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
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