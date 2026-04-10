package com.example.stepaside

import android.app.Application
import com.example.stepaside.data.db.AppDatabase

class StepAsideApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
}