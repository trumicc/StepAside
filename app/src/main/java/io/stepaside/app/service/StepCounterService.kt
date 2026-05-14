package io.stepaside.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.stepaside.app.MainActivity
import io.stepaside.app.R
import io.stepaside.app.StepAsideApp
import io.stepaside.app.data.db.DailySteps
import io.stepaside.app.data.db.LifetimeStats
import io.stepaside.app.widget.StepWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import android.app.Service

class StepCounterService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    // -1 means "not yet baselined for the current day".
    @Volatile private var sensorStepsAtBoot = -1
    @Volatile private var stepsToday = 0
    @Volatile private var goalSteps = DEFAULT_GOAL
    @Volatile private var lastKnownDate = ""

    /**
     * Guard: when false, [onSensorChanged] returns immediately. Set false during
     * cold start and during the midnight day rollover, so we never compute a delta
     * against stale state.
     */
    @Volatile private var isInitialized = false

    private val db get() = (application as StepAsideApp).database
    private val today: String get() = LocalDate.now().toString()

    companion object {
        const val CHANNEL_ID = "step_counter_channel"
        const val NOTIF_ID = 1
        const val DEFAULT_GOAL = 10_000

        /** Approximate stride in meters per step. */
        private const val METERS_PER_STEP = 0.762f
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(0, goalSteps))

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        serviceScope.launch {
            initForToday()
            withContext(Dispatchers.Main) {
                stepSensor?.let {
                    sensorManager.registerListener(
                        this@StepCounterService,
                        it,
                        SensorManager.SENSOR_DELAY_NORMAL,
                    )
                }
                isInitialized = true
            }
        }
    }

    /**
     * Idempotent setup for the current calendar day. Called from [onCreate]
     * and again whenever a midnight rollover is detected in [onSensorChanged].
     *
     * Resets the sensor baseline so the next sensor event re-anchors against
     * fresh state.
     */
    private suspend fun initForToday() {
        val currentDay = today
        lastKnownDate = currentDay

        val existing = db.dailyStepsDao().getByDate(currentDay)
        if (existing != null) {
            stepsToday = existing.steps
            goalSteps = existing.goalSteps
        } else {
            stepsToday = 0
            db.dailyStepsDao().upsert(
                DailySteps(dateStr = currentDay, goalSteps = goalSteps),
            )
        }

        // Force re-baselining of the sensor counter on the very next event.
        sensorStepsAtBoot = -1

        if (db.lifetimeStatsDao().get() == null) {
            db.lifetimeStatsDao().upsert(LifetimeStats())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isInitialized) return
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalSensorSteps = event.values[0].toInt()
        val currentDay = today

        // Midnight rollover: pause sensor processing, reset state for the new day.
        if (currentDay != lastKnownDate) {
            isInitialized = false
            serviceScope.launch {
                initForToday()
                isInitialized = true
            }
            return
        }

        // First sensor event after (re)init — anchor the baseline.
        if (sensorStepsAtBoot < 0) {
            sensorStepsAtBoot = totalSensorSteps - stepsToday
            return
        }

        val newStepsToday = (totalSensorSteps - sensorStepsAtBoot).coerceAtLeast(0)
        val stepDelta = newStepsToday - stepsToday
        if (stepDelta < 0) return // sensor went backwards somehow, ignore.

        stepsToday = newStepsToday
        val goalReached = stepsToday >= goalSteps

        serviceScope.launch {
            val existing = db.dailyStepsDao().getByDate(currentDay)
            if (existing == null) {
                db.dailyStepsDao().upsert(
                    DailySteps(
                        dateStr = currentDay,
                        steps = stepsToday,
                        goalSteps = goalSteps,
                        goalReached = goalReached,
                    ),
                )
            } else {
                db.dailyStepsDao().updateSteps(currentDay, stepsToday, goalReached)
            }

            if (stepDelta > 0) {
                val distanceDelta = stepDelta * METERS_PER_STEP
                val lifetimeExisting = db.lifetimeStatsDao().get()
                if (lifetimeExisting != null) {
                    db.lifetimeStatsDao().addSteps(stepDelta.toLong(), distanceDelta)
                } else {
                    db.lifetimeStatsDao().upsert(
                        LifetimeStats(
                            totalSteps = stepDelta.toLong(),
                            totalDistanceMeters = distanceDelta,
                        ),
                    )
                }
            }

            updateNotification(stepsToday, goalSteps)
            StepWidget.updateAll(this@StepCounterService)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(steps: Int, goal: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(steps, goal))
    }

    private fun buildNotification(steps: Int, goal: Int): Notification {
        val remaining = (goal - steps).coerceAtLeast(0)
        val launchIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("$steps steps · $remaining left to goal")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pi)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step Counter",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Tracks your daily step count in the background."
            setShowBadge(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
