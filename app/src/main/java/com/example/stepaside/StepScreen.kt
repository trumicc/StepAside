package com.example.stepaside

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StepScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database

    var steps by remember { mutableStateOf(0) }
    var goal by remember { mutableStateOf(10000) }
    var popupAchievement by remember { mutableStateOf<Achievement?>(null) }
    val prefs = context.getSharedPreferences("stepaside_prefs", android.content.Context.MODE_PRIVATE)

    val today = LocalDate.now().toString()

    LaunchedEffect(Unit) {
        while (true) {
            val todayStr = LocalDate.now().toString()
            val todayData = db.dailyStepsDao().getByDate(todayStr)
            steps = todayData?.steps ?: 0
            goal = todayData?.goalSteps ?: 10000
            delay(60_000)
        }
    }

    // Achievement check
    var lifetimeSteps by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        db.lifetimeStatsDao().getFlow().collectLatest { stats ->
            lifetimeSteps = stats?.totalSteps ?: 0L
        }
    }

    LaunchedEffect(lifetimeSteps) {
        val newlyUnlocked = ALL_ACHIEVEMENTS.filter { achievement ->
            lifetimeSteps >= achievement.requiredSteps &&
                    !prefs.getBoolean("achievement_shown_${achievement.id}", false)
        }
        if (newlyUnlocked.isNotEmpty()) {
            val first = newlyUnlocked.first()
            prefs.edit().putBoolean("achievement_shown_${first.id}", true).apply()
            popupAchievement = first
        }
    }

    popupAchievement?.let { achievement ->
        AchievementPopup(achievement = achievement, onDismiss = { popupAchievement = null })
    }

    val remaining = (goal - steps).coerceAtLeast(0)
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val goalReached = steps >= goal
    val percent = (progress * 100).toInt()

    val todayFormatted = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)
    )

    // Pulse animation when goal reached
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (goalReached) Color(0xFF0A1F0A) else Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = todayFormatted,
                color = Color(0xFF8B949E),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    val stroke = 22.dp.toPx()
                    val inset = stroke / 2

                    // Background ring
                    drawArc(
                        color = Color(0xFF21262D),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )

                    // Progress ring
                    if (progress > 0f) {
                        drawArc(
                            color = if (goalReached)
                                Color(0xFF39D353).copy(alpha = if (goalReached) pulseAlpha else 1f)
                            else Color(0xFF39D353),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (goalReached) {
                        Text("🎉", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "%,d".format(steps),
                        color = Color.White,
                        fontSize = if (goalReached) 48.sp else 56.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "steps",
                        color = Color(0xFF8B949E),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$percent%",
                        color = if (goalReached) Color(0xFF39D353) else Color(0xFF8B949E),
                        fontSize = 13.sp,
                        fontWeight = if (goalReached) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (goalReached) {
                Text(
                    text = "Goal reached! 🎉",
                    color = Color(0xFF39D353),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Amazing work today!",
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "%,d to goal".format(remaining),
                    color = Color(0xFF39D353),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Goal: %,d steps".format(goal),
                    color = Color(0xFF8B949E),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 90.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val distanceKm = steps * 0.000762f
                val calories = (steps * 0.04f).toInt()

                BottomStat("%.1f km".format(distanceKm), "Distance")
                BottomStat("🔥 ${calories}kcal", "Calories")
            }
        }
    }
}

@Composable
fun BottomStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color(0xFF8B949E), fontSize = 12.sp)
    }
}