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

        // Load today's existing steps
        serviceScope.launch {
            val existing = db.dailyStepsDao().getByDate(today)
            if (existing != null) {
                stepsToday = existing.steps
                goalSteps = existing.goalSteps
            } else {
                db.dailyStepsDao().upsert(DailySteps(dateStr = today, goalSteps = goalSteps))
            }
            // Init lifetime stats if first run
            if (db.lifetimeStatsDao().get() == null) {
                db.lifetimeStatsDao().upsert(LifetimeStats())
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val totalSensorSteps = event.values[0].toInt()

        if (sensorStepsAtBoot < 0) {
            // First reading — calibrate baseline
            sensorStepsAtBoot = totalSensorSteps - stepsToday
        }

        stepsToday = totalSensorSteps - sensorStepsAtBoot
        val goalReached = stepsToday >= goalSteps

        serviceScope.launch {
            db.dailyStepsDao().updateSteps(today, stepsToday, goalReached)
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
            .setSmallIcon(android.R.drawable.ic_menu_directions)            .setContentIntent(pi)
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