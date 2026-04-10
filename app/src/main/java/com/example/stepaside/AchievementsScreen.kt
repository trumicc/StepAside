package com.example.stepaside

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AchievementsScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database
    var totalSteps by remember { mutableStateOf(0L) }
    var popupAchievement by remember { mutableStateOf<Achievement?>(null) }
    val prefs = context.getSharedPreferences("stepaside_prefs", android.content.Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        db.lifetimeStatsDao().getFlow().collectLatest { stats ->
            totalSteps = stats?.totalSteps ?: 0L
        }
    }

    LaunchedEffect(totalSteps) {
        val newlyUnlocked = ALL_ACHIEVEMENTS.filter { achievement ->
            totalSteps >= achievement.requiredSteps &&
                    !prefs.getBoolean("achievement_shown_${achievement.id}", false)
        }
        if (newlyUnlocked.isNotEmpty()) {
            val first = newlyUnlocked.first()
            prefs.edit().putBoolean("achievement_shown_${first.id}", true).apply()
            popupAchievement = first
        }
    }

    popupAchievement?.let { achievement ->
        AchievementPopup(
            achievement = achievement,
            onDismiss = { popupAchievement = null }
        )
    }

    val achievements = ALL_ACHIEVEMENTS.map { it.copy(unlocked = totalSteps >= it.requiredSteps) }
    val unlocked = achievements.count { it.unlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text("Achievements", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "$unlocked / ${achievements.size} unlocked",
            color = Color(0xFF8B949E),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(achievements) { achievement ->
                AchievementCard(achievement, totalSteps)
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, totalSteps: Long) {
    val progress = (totalSteps.toFloat() / achievement.requiredSteps).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (achievement.unlocked) Color(0xFF161B22) else Color(0xFF0D1117),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = achievement.emoji,
            fontSize = 32.sp,
            modifier = Modifier.padding(end = 16.dp),
            color = if (achievement.unlocked) Color.White else Color(0xFF8B949E)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = achievement.title,
                    color = if (achievement.unlocked) Color.White else Color(0xFF8B949E),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (achievement.unlocked) {
                    Text("✓", color = Color(0xFF39D353), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = achievement.description,
                color = Color(0xFF8B949E),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF21262D), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(
                            if (achievement.unlocked) Color(0xFF39D353)
                            else Color(0xFF39D353).copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}