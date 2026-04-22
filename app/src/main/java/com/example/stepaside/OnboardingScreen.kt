package com.example.stepaside

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as StepAsideApp).database

    var selectedAvatarId by remember { mutableStateOf("lilo") }
    var displayName by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var goalInput by remember { mutableStateOf("10000") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161B22)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = getAvatarRes(selectedAvatarId)),
                    contentDescription = "Selected avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Welcome to StepAside",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Set up your profile to get started",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                "Choose your avatar",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val rows = AVATAR_LIST.chunked(5)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { avatar ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(0xFF161B22))
                                .border(
                                    width = if (selectedAvatarId == avatar.id) 2.dp else 0.dp,
                                    color = if (selectedAvatarId == avatar.id) Color(0xFF39D353) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatarId = avatar.id },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatar.imageRes),
                                contentDescription = avatar.id,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    repeat(5 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Your name",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = displayName,
                singleLine = true,
                onValueChange = { displayName = it },
                placeholder = { Text("Display name", color = Color(0xFF8B949E)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF39D353),
                    unfocusedBorderColor = Color(0xFF21262D)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Body stats (optional)",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = heightInput,
                    singleLine = true,
                    onValueChange = { heightInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Height (cm)", color = Color(0xFF8B949E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353), unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weightInput,
                    singleLine = true,
                    onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Weight (kg)", color = Color(0xFF8B949E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353), unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Daily step goal",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = goalInput,
                singleLine = true,
                onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                label = { Text("Steps per day", color = Color(0xFF8B949E)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF39D353), unfocusedBorderColor = Color(0xFF21262D)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFCF2626),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = ""
                        try {
                            val userId = supabase.auth.currentUserOrNull()?.id
                            if (userId == null) {
                                errorMessage = "Error: Not logged in"
                                isSaving = false
                                return@launch
                            }

                            supabase.postgrest["profiles"].upsert(
                                ProfileUpsert(
                                    id = userId,
                                    display_name = displayName,
                                    avatar_color = selectedAvatarId,
                                    height_cm = heightInput.toIntOrNull() ?: 0,
                                    weight_kg = weightInput.toFloatOrNull() ?: 0f,
                                    goal_steps = goalInput.toIntOrNull() ?: 10000
                                )
                            ) {
                                onConflict = "id"
                            }

                            val today = java.time.LocalDate.now().toString()
                            val existing = db.dailyStepsDao().getByDate(today)
                            val newGoal = goalInput.toIntOrNull() ?: 10000
                            if (existing != null) {
                                db.dailyStepsDao().upsert(existing.copy(goalSteps = newGoal))
                            }

                            val prefs = context.getSharedPreferences(
                                "stepaside_prefs",
                                android.content.Context.MODE_PRIVATE
                            )
                            prefs.edit().putBoolean("onboarding_done", true).apply()
                            onDone()

                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39D353)),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text("Let's Go! 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}