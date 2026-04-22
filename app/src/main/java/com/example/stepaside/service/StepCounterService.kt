package com.example.stepaside.service

import android.app.*
import android.content.Intent
import android.hardware.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.stepaside.MainActivity
import com.example.stepaside.StepAsideApp
import com.example.stepaside.data.db.DailySteps
import com.example.stepaside.data.db.LifetimeStats
import kotlinx.coroutines.*
import java.time.LocalDate
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.stepaside.widget.StepWidget

class StepCounterService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var sensorStepsAtBoot = -1
    private var stepsToday = 0
    private var goalSteps = 10000
    private var lastKnownDate = ""
    private var isInitialized = false

    private val db get() = (application as StepAsideApp).database
    private val today get() = LocalDate.now().toString()

    companion object {
        const val CHANNEL_ID = "step_counter_channel"
        const val NOTIF_ID = 1
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
                        this@StepCounterService, it,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
                isInitialized = true
            }
        }
    }

    private suspend fun initForToday() {
        val currentDay = today
        lastKnownDate = currentDay

        val existing = db.dailyStepsDao().getByDate(currentDay)
        if (existing != null) {
            stepsToday = existing.steps
            goalSteps = existing.goalSteps
        } else {
            stepsToday = 0
            db.dailyStepsDao().upsert(DailySteps(dateStr = currentDay, goalSteps = goalSteps))
        }

        // Reset sensor baseline so new day starts fresh
        sensorStepsAtBoot = -1

        if (db.lifetimeStatsDao().get() == null) {
            db.lifetimeStatsDao().upsert(LifetimeStats())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isInitialized) return
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalSensorSteps = event.values[0].toInt()

        // Check for midnight — new day!
        val currentDay = today
        if (currentDay != lastKnownDate) {
            serviceScope.launch {
                initForToday()
            }
            return
        }

        if (sensorStepsAtBoot < 0) {
            sensorStepsAtBoot = totalSensorSteps - stepsToday
        }

        val newStepsToday = (totalSensorSteps - sensorStepsAtBoot).coerceAtLeast(0)
        val stepDelta = newStepsToday - stepsToday
        stepsToday = newStepsToday
        val goalReached = stepsToday >= goalSteps

        serviceScope.launch {
            // Säkerställ att raden finns
            val existing = db.dailyStepsDao().getByDate(currentDay)
            if (existing == null) {
                db.dailyStepsDao().upsert(
                    DailySteps(dateStr = currentDay, steps = stepsToday, goalSteps = goalSteps, goalReached = goalReached)
                )
            } else {
                db.dailyStepsDao().updateSteps(currentDay, stepsToday, goalReached)
            }

            if (stepDelta > 0) {
                val distanceDelta = stepDelta * 0.762f
                val lifetimeExisting = db.lifetimeStatsDao().get()
                if (lifetimeExisting != null) {
                    db.lifetimeStatsDao().addSteps(stepDelta.toLong(), distanceDelta)
                } else {
                    db.lifetimeStatsDao().upsert(
                        LifetimeStats(
                            totalSteps = stepDelta.toLong(),
                            totalDistanceMeters = distanceDelta
                        )
                    )
                }
            }

            updateNotification(stepsToday, goalSteps)
            updateWidget()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
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
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StepAside")
            .setContentText("$steps steps · $remaining left to goal")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetComponent = ComponentName(this, StepWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        for (id in widgetIds) {
            StepWidget.updateWidget(this, appWidgetManager, id)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step Counter",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}