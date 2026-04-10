package com.example.stepaside

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepaside.data.db.DailySteps
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database
    val scope = rememberCoroutineScope()

    var totalSteps by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0f) }
    var goalInput by remember { mutableStateOf("10000") }
    var showGoalSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.lifetimeStatsDao().getFlow().collectLatest { stats ->
            totalSteps = stats?.totalSteps ?: 0L
            totalDistance = stats?.totalDistanceMeters ?: 0f
        }
    }

    LaunchedEffect(Unit) {
        val today = LocalDate.now().toString()
        val todaySteps = db.dailyStepsDao().getByDate(today)
        goalInput = (todaySteps?.goalSteps ?: 10000).toString()
    }

    val earthCircumference = 40_075_000f
    val percentOfEarth = (totalDistance / earthCircumference * 100)

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

        // Story text
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
            StatCard(modifier = Modifier.weight(1f), emoji = "👣", label = "Total Steps", value = "%,d".format(totalSteps))
            StatCard(modifier = Modifier.weight(1f), emoji = "📍", label = "Distance", value = "%.1f km".format(totalDistance / 1000f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "🌍",
                label = "% of Earth",
                value = "%.4f%%".format(percentOfEarth)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "🏔️",
                label = "Everest",
                value = if (totalSteps >= 11800) "✓ Done!" else "%,d left".format((11800 - totalSteps).coerceAtLeast(0))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Daily Goal", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = goalInput,
                onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                label = { Text("Steps", color = Color(0xFF8B949E)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF39D353),
                    unfocusedBorderColor = Color(0xFF21262D)
                ),
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )

            Button(
                onClick = {
                    val newGoal = goalInput.toIntOrNull() ?: 10000
                    scope.launch {
                        val today = LocalDate.now().toString()
                        val existing = db.dailyStepsDao().getByDate(today)
                        if (existing != null) {
                            db.dailyStepsDao().upsert(existing.copy(goalSteps = newGoal))
                        } else {
                            db.dailyStepsDao().upsert(DailySteps(dateStr = today, goalSteps = newGoal))
                        }
                        showGoalSaved = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39D353)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (showGoalSaved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("✓ Goal saved!", color = Color(0xFF39D353), fontSize = 13.sp)
            LaunchedEffect(showGoalSaved) {
                kotlinx.coroutines.delay(2000)
                showGoalSaved = false
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