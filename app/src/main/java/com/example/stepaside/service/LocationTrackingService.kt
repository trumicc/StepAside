package com.example.stepaside.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.stepaside.MainActivity
import com.example.stepaside.StepAsideApp
import com.example.stepaside.data.db.RoutePoint
import com.example.stepaside.data.db.WalkSession
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.time.LocalDate

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

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
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        var isTracking = false
        var currentSessionId: Long = -1L
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

        // Skapa session i DB
        serviceScope.launch {
            val session = WalkSession(
                startTime = sessionStartTime,
                dateStr = today
            )
            currentSessionId = db.walkSessionDao().insert(session)
            LocationTrackingService.currentSessionId = currentSessionId
        }

        // GPS-inställningar
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateDistanceMeters(5f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch {
                    if (currentSessionId < 0) return@launch

                    // Spara punkt
                    db.routePointDao().insert(
                        RoutePoint(
                            sessionId = currentSessionId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    // Räkna distans
                    if (lastLat != 0.0 && lastLon != 0.0) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            lastLat, lastLon,
                            location.latitude, location.longitude,
                            results
                        )
                        totalDistance += results[0]
                    }
                    lastLat = location.latitude
                    lastLon = location.longitude

                    // Elevation gain
                    if (lastElevation != 0.0) {
                        val gain = location.altitude - lastElevation
                        if (gain > 0) totalElevationGain += gain.toFloat()
                    }
                    lastElevation = location.altitude

                    // Uppdatera session
                    val updated = WalkSession(
                        id = currentSessionId,
                        startTime = sessionStartTime,
                        endTime = null,
                        steps = sessionSteps,
                        distanceMeters = totalDistance,
                        elevationGainMeters = totalElevationGain,
                        dateStr = LocalDate.now().toString()
                    )
                    db.walkSessionDao().update(updated)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        serviceScope.launch {
            if (currentSessionId >= 0) {
                val final = WalkSession(
                    id = currentSessionId,
                    startTime = sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    steps = sessionSteps,
                    distanceMeters = totalDistance,
                    elevationGainMeters = totalElevationGain,
                    dateStr = LocalDate.now().toString()
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
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StepAside — Walk")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}