package io.stepaside.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.stepaside.app.MainActivity
import io.stepaside.app.R
import io.stepaside.app.StepAsideApp
import io.stepaside.app.data.db.RoutePoint
import io.stepaside.app.data.db.WalkSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var currentSessionId: Long = -1
    private var sessionStartTime: Long = 0
    private var sessionSteps: Int = 0
    private var lastElevation: Double = 0.0
    private var totalElevationGain: Float = 0f
    private var totalDistance: Float = 0f
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0

    private val db get() = (application as StepAsideApp).database

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIF_ID = 2
        const val ACTION_START = "io.stepaside.app.action.START_TRACKING"
        const val ACTION_STOP = "io.stepaside.app.action.STOP_TRACKING"

        // TODO(phase-2): Replace with a proper repository + StateFlow so the UI
        //   can observe tracking state reactively. These statics are a known
        //   architectural smell and will be removed in Phase 2.
        @Volatile var isTracking = false
        @Volatile var currentSessionId: Long = -1L
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        isTracking = true
        sessionStartTime = System.currentTimeMillis()
        val today = LocalDate.now().toString()

        startForeground(NOTIF_ID, buildNotification("Tracking walk..."))

        // Reset per-session counters in case service is reused.
        sessionSteps = 0
        totalDistance = 0f
        totalElevationGain = 0f
        lastLat = 0.0
        lastLon = 0.0
        lastElevation = 0.0

        serviceScope.launch {
            val session = WalkSession(
                startTime = sessionStartTime,
                dateStr = today,
            )
            currentSessionId = db.walkSessionDao().insert(session)
            Companion.currentSessionId = currentSessionId
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5_000L,
        ).setMinUpdateDistanceMeters(5f).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch {
                    if (currentSessionId < 0) return@launch

                    db.routePointDao().insert(
                        RoutePoint(
                            sessionId = currentSessionId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )

                    if (lastLat != 0.0 && lastLon != 0.0) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            lastLat, lastLon,
                            location.latitude, location.longitude,
                            results,
                        )
                        totalDistance += results[0]
                    }
                    lastLat = location.latitude
                    lastLon = location.longitude

                    if (lastElevation != 0.0) {
                        val gain = location.altitude - lastElevation
                        if (gain > 0) totalElevationGain += gain.toFloat()
                    }
                    lastElevation = location.altitude

                    val updated = WalkSession(
                        id = currentSessionId,
                        startTime = sessionStartTime,
                        endTime = null,
                        steps = sessionSteps,
                        distanceMeters = totalDistance,
                        elevationGainMeters = totalElevationGain,
                        dateStr = LocalDate.now().toString(),
                    )
                    db.walkSessionDao().update(updated)
                }
            }
        }
        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, callback, Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            // Location permission was revoked.
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopTracking() {
        isTracking = false
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null

        serviceScope.launch {
            if (currentSessionId >= 0) {
                val final = WalkSession(
                    id = currentSessionId,
                    startTime = sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    steps = sessionSteps,
                    distanceMeters = totalDistance,
                    elevationGainMeters = totalElevationGain,
                    dateStr = LocalDate.now().toString(),
                )
                db.walkSessionDao().update(final)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name) + " — Walk")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Tracks your walking route while a walk is in progress."
            setShowBadge(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
