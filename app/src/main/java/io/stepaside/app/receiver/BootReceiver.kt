package io.stepaside.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.stepaside.app.service.StepCounterService

/**
 * Restarts the step counter service after device reboot, so step counting
 * resumes without the user needing to open the app.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, StepCounterService::class.java),
            )
        }.onFailure { it.printStackTrace() }
    }
}
