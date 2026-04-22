package com.example.stepaside

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database

    var totalSteps by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        db.lifetimeStatsDao().getFlow().collectLatest { stats ->
            totalSteps = stats?.totalSteps ?: 0L
            totalDistance = stats?.totalDistanceMeters ?: 0f
        }
    }

    val earthCircumference = 40_075_000f
    val percentOfEarth = (totalDistance / earthCircumference * 100)
    val unlockedAchievements = ALL_ACHIEVEMENTS.filter { totalSteps >= it.requiredSteps }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text("Stats", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (totalSteps == 0L) "You've walked 0 steps — let's get started! 👟"
            else "You've walked %,d steps so far. Keep going! 💪".format(totalSteps),
            color = Color(0xFF8B949E),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Lifetime", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "👣", "Total Steps", "%,d".format(totalSteps))
            StatCard(Modifier.weight(1f), "📍", "Distance", "%.1f km".format(totalDistance / 1000f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "🌍", "% of Earth", "%.4f%%".format(percentOfEarth))
            StatCard(
                Modifier.weight(1f), "🏔️", "Everest",
                if (totalSteps >= 11800) "✓ Done!" else "%,d left".format((11800 - totalSteps).coerceAtLeast(0))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Medals section
        Text(
            "Medals",
            color = Color(0xFF8B949E),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "${unlockedAchievements.size} / ${ALL_ACHIEVEMENTS.size} earned",
            color = Color(0xFF39D353),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (unlockedAchievements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Start walking to earn medals! 🚶",
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Grid 4 per rad
            val rows = unlockedAchievements.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { achievement ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (achievement.imageRes != null) {
                                Image(
                                    painter = painterResource(id = achievement.imageRes),
                                    contentDescription = achievement.title,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF161B22)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(achievement.emoji, fontSize = 28.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = achievement.title,
                                color = Color(0xFF8B949E),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                    // Fyll tomma platser
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, emoji: String, label: String, value: String) {
    Column(
        modifier = modifier
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8B949E), fontSize = 12.sp)
    }
}