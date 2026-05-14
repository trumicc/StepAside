<<<<<<< Updated upstream:app/src/main/java/com/example/stepaside/receiver/BootReceiver.kt
package com.example.stepaside.receiver
=======
package io.stepaside.app.receiver
>>>>>>> Stashed changes:app/src/main/java/io/stepaside/app/receiver/BootReceiver.kt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
<<<<<<< Updated upstream:app/src/main/java/com/example/stepaside/receiver/BootReceiver.kt
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
=======
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
>>>>>>> Stashed changes:app/src/main/java/io/stepaside/app/receiver/BootReceiver.kt
