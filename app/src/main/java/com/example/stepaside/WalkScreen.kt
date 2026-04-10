package com.example.stepaside

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    var calories by remember { mutableStateOf(0) }

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
                    calories = (sessionSteps * 0.04f).toInt()
                }
            }
        } else {
            elapsedSeconds = 0
            sessionSteps = 0
            sessionDistance = 0f
            calories = 0
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeStr = if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Pace calculation
    val pace = if (elapsedSeconds > 0 && sessionDistance > 0) {
        val minutesPerKm = (elapsedSeconds / 60f) / (sessionDistance / 1000f)
        val paceMin = minutesPerKm.toInt()
        val paceSec = ((minutesPerKm - paceMin) * 60).toInt()
        "%d:%02d /km".format(paceMin, paceSec)
    } else "—"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isTracking) "Walk in progress" else "Ready to walk?",
                color = Color(0xFF8B949E),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Animated ring with timer inside
            Box(contentAlignment = Alignment.Center) {
                if (isTracking) {
                    Canvas(modifier = Modifier.size(220.dp)) {
                        val stroke = 6.dp.toPx()
                        val inset = stroke / 2
                        drawArc(
                            color = Color(0xFF39D353).copy(alpha = pulseAlpha),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(
                                width = (stroke * pulseScale),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isTracking) "elapsed" else "tap to start",
                        color = Color(0xFF8B949E),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Stats grid
            if (isTracking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WalkStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "👣",
                        value = "%,d".format(sessionSteps),
                        label = "Steps"
                    )
                    WalkStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📍",
                        value = "%.0f m".format(sessionDistance),
                        label = "Distance"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WalkStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🔥",
                        value = "$calories",
                        label = "Calories"
                    )
                    WalkStatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "⚡",
                        value = pace,
                        label = "Pace"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                modifier = Modifier
                    .height(64.dp)
                    .width(200.dp)
            ) {
                Text(
                    text = if (isTracking) "⏹ Stop" else "▶ Walk",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun WalkStatCard(modifier: Modifier = Modifier, emoji: String, value: String, label: String) {
    Column(
        modifier = modifier
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8B949E), fontSize = 12.sp)
    }
}