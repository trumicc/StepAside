package com.example.stepaside

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val display_name: String? = null,
    val avatar_color: String = "#1D9E75",
    val height_cm: Int = 0,
    val weight_kg: Float = 0f,
    val goal_steps: Int = 10000
)

val AVATAR_OPTIONS = listOf(
    "🏃", "🚶", "⚡", "🌟", "🔥",
    "💪", "🎯", "🏆", "🌿", "❄️"
)

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("🏃") }
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var goalInput by remember { mutableStateOf("10000") }
    var totalSteps by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0f) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }

    val userId = supabase.auth.currentUserOrNull()?.id

    // Load profile
    LaunchedEffect(Unit) {
        try {
            if (userId != null) {
                val profile = supabase.postgrest["profiles"]
                    .select(Columns.ALL) {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<Profile>()

                profile?.let {
                    displayName = it.display_name ?: ""
                    selectedAvatar = it.avatar_color
                    heightInput = if (it.height_cm > 0) it.height_cm.toString() else ""
                    weightInput = if (it.weight_kg > 0) it.weight_kg.toString() else ""
                    goalInput = it.goal_steps.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        db.lifetimeStatsDao().getFlow().collectLatest { stats ->
            totalSteps = stats?.totalSteps ?: 0L
            totalDistance = stats?.totalDistanceMeters ?: 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                // Logout button
                Button(
                    onClick = {
                        scope.launch {
                            supabase.auth.signOut()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF2626)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Logout", color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar selection
            Text("Avatar", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AVATAR_OPTIONS.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedAvatar == emoji) Color(0xFF39D353).copy(alpha = 0.2f)
                                else Color(0xFF161B22)
                            )
                            .border(
                                width = if (selectedAvatar == emoji) 2.dp else 0.dp,
                                color = if (selectedAvatar == emoji) Color(0xFF39D353) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Name
            Text("Display name", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = { Text("Your name", color = Color(0xFF8B949E)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF39D353),
                    unfocusedBorderColor = Color(0xFF21262D)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Goal, height, weight
            Text("Settings", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily goal", color = Color(0xFF8B949E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353),
                        unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Height (cm)", color = Color(0xFF8B949E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353),
                        unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it.filter { c -> c.isDigit() || it == "." } },
                    label = { Text("Weight (kg)", color = Color(0xFF8B949E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353),
                        unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            if (userId != null) {
                                supabase.postgrest["profiles"].upsert(
                                    mapOf(
                                        "id" to userId,
                                        "display_name" to displayName,
                                        "avatar_color" to selectedAvatar,
                                        "height_cm" to (heightInput.toIntOrNull() ?: 0),
                                        "weight_kg" to (weightInput.toFloatOrNull() ?: 0f),
                                        "goal_steps" to (goalInput.toIntOrNull() ?: 10000)
                                    )
                                )
                                // Uppdatera dagligt mål i Room
                                val today = java.time.LocalDate.now().toString()
                                val existing = db.dailyStepsDao().getByDate(today)
                                val newGoal = goalInput.toIntOrNull() ?: 10000
                                if (existing != null) {
                                    db.dailyStepsDao().upsert(existing.copy(goalSteps = newGoal))
                                }
                                saveMessage = "✓ Saved!"
                            }
                        } catch (e: Exception) {
                            saveMessage = "Error: ${e.message}"
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39D353)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Profile", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (saveMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(saveMessage, color = Color(0xFF39D353), fontSize = 13.sp)
                LaunchedEffect(saveMessage) {
                    kotlinx.coroutines.delay(2000)
                    saveMessage = ""
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lifetime stats
            Text("Lifetime Stats", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), emoji = "👣", label = "Total Steps", value = "%,d".format(totalSteps))
                StatCard(modifier = Modifier.weight(1f), emoji = "📍", label = "Distance", value = "%.1f km".format(totalDistance / 1000f))
            }
        }
    }
}