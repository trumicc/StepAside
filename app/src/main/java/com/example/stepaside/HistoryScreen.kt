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
import com.example.stepaside.data.db.DailySteps
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database
    var history by remember { mutableStateOf<List<DailySteps>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.dailyStepsDao().getAllFlow().collectLatest { list ->
            history = list.sortedByDescending { it.dateStr }
        }
    }

    // Calculate streak
    val streak = remember(history) {
        val sorted = history.sortedByDescending { it.dateStr }
        var count = 0
        var expectedDate = LocalDate.now()
        for (day in sorted) {
            val date = try { LocalDate.parse(day.dateStr) } catch (e: Exception) { break }
            if (date == expectedDate && day.goalReached) {
                count++
                expectedDate = expectedDate.minusDays(1)
            } else if (date == expectedDate && !day.goalReached) {
                break
            } else {
                break
            }
        }
        count
    }

    // Best day
    val bestDay = remember(history) {
        history.maxByOrNull { it.steps }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text("History", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Streak + Best day cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streak card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔥", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$streak",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (streak == 1) "day streak" else "day streak",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp
                )
                if (streak == 0) {
                    Text(
                        text = "Hit your goal to start!",
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp
                    )
                }
            }

            // Best day card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⭐", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (bestDay != null) "%,d".format(bestDay.steps) else "—",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "best day",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp
                )
                if (bestDay != null) {
                    val date = try {
                        LocalDate.parse(bestDay.dateStr).format(
                            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                        )
                    } catch (e: Exception) { "" }
                    Text(
                        text = date,
                        color = Color(0xFF39D353),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet — start walking!",
                    color = Color(0xFF8B949E),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(history) { day ->
                    DayCard(day)
                }
            }
        }
    }
}

@Composable
fun DayCard(day: DailySteps) {
    val date = try {
        LocalDate.parse(day.dateStr).format(
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
        )
    } catch (e: Exception) { day.dateStr }

    val progress = (day.steps.toFloat() / day.goalSteps).coerceIn(0f, 1f)
    val isToday = day.dateStr == LocalDate.now().toString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isToday) "Today" else date,
                color = if (isToday) Color(0xFF39D353) else Color(0xFF8B949E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (day.goalReached) {
                Text(
                    "✓ Goal",
                    color = Color(0xFF39D353),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "%,d".format(day.steps),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "of %,d steps".format(day.goalSteps),
            color = Color(0xFF8B949E),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF21262D), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(Color(0xFF39D353), RoundedCornerShape(2.dp))
            )
        }
    }
}