package com.example.stepaside

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.stepaside.service.LocationTrackingService
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun WalkScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database

    var isTracking by remember { mutableStateOf(LocationTrackingService.isTracking) }
    var sessionSteps by remember { mutableStateOf(0) }
    var sessionDistance by remember { mutableStateOf(0f) }
    var elapsedSeconds by remember { mutableStateOf(0L) }

    // Timer
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (isTracking) {
                delay(1000)
                elapsedSeconds++
                val id = LocationTrackingService.currentSessionId
                if (id > 0) {
                    val session = db.walkSessionDao().getById(id)
                    sessionSteps = session?.steps ?: 0
                    sessionDistance = session?.distanceMeters ?: 0f
                }
            }
        } else {
            elapsedSeconds = 0
            sessionSteps = 0
            sessionDistance = 0f
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeStr = if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isTracking) "Walk in progress" else "Ready to walk?",
            color = Color(0xFF8B949E),
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Timer
        Text(
            text = timeStr,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WalkStat("👣", "%,d".format(sessionSteps), "Steps")
            WalkStat("📍", "%.0f m".format(sessionDistance), "Distance")
        }

        Spacer(modifier = Modifier.height(64.dp))

        // Start/Stop button
        Button(
            onClick = {
                val intent = Intent(context, LocationTrackingService::class.java)
                if (!isTracking) {
                    intent.action = LocationTrackingService.ACTION_START
                    ContextCompat.startForegroundService(context, intent)
                    isTracking = true
                } else {
                    intent.action = LocationTrackingService.ACTION_STOP
                    context.startService(intent)
                    isTracking = false
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color(0xFFCF2626) else Color(0xFF39D353)
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(64.dp).width(200.dp)
        ) {
            Text(
                text = if (isTracking) "Stop" else "Start Walk",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun WalkStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8B949E), fontSize = 13.sp)
    }
}