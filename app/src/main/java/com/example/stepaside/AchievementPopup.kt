package com.example.stepaside

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun AchievementPopup(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val emojiScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(Unit) {
        visible = true
        // Vibration pattern
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 100, 50, 100, 50, 200),
                intArrayOf(0, 255, 0, 200, 0, 150),
                -1
            )
        )
        // Auto-dismiss after 4 seconds
        delay(4000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .scale(scale)
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🏆",
                    fontSize = 20.sp,
                    color = Color(0xFF39D353),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Achievement Unlocked!",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = achievement.emoji,
                    fontSize = 72.sp,
                    modifier = Modifier.scale(emojiScale)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = achievement.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.description,
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFF39D353).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Tap to dismiss",
                        color = Color(0xFF39D353),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}