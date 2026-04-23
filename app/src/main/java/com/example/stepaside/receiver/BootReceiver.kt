package com.example.stepaside.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.stepaside.service.StepCounterService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            context.startForegroundService(
                Intent(context, StepCounterService::class.java)
            )
        }
    }
}