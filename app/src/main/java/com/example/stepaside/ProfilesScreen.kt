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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
    val avatar_color: String = "lilo",
    val height_cm: Int = 0,
    val weight_kg: Float = 0f,
    val goal_steps: Int = 10000
)

data class AvatarItem(val id: String, val imageRes: Int)

val AVATAR_LIST = listOf(
    AvatarItem("lilo", R.drawable.profile_avatar_lilo),
    AvatarItem("avatar1", R.drawable.profile_avatar1),
    AvatarItem("avatar2", R.drawable.profile_avatar2),
    AvatarItem("avatar3", R.drawable.profile_avatar3),
    AvatarItem("avatar4", R.drawable.profile_avatar4),
    AvatarItem("avatar5", R.drawable.profile_avatar5),
    AvatarItem("avatar6", R.drawable.profile_avatar6),
    AvatarItem("avatar7", R.drawable.profile_avatar7),
    AvatarItem("avatar8", R.drawable.profile_avatar8),
    AvatarItem("avatar9", R.drawable.profile_avatar9),
)

fun getAvatarRes(id: String): Int =
    AVATAR_LIST.find { it.id == id }?.imageRes ?: R.drawable.profile_avatar_lilo

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val db = (context.applicationContext as StepAsideApp).database
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<Profile?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf("lilo") }
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var goalInput by remember { mutableStateOf("10000") }
    var isSaving by remember { mutableStateOf(false) }

    var totalSteps by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0f) }

    val userId = supabase.auth.currentUserOrNull()?.id

    LaunchedEffect(Unit) {
        try {
            if (userId != null) {
                val result = supabase.postgrest["profiles"]
                    .select(Columns.ALL) { filter { eq("id", userId) } }
                    .decodeList<Profile>()

                val p = result.firstOrNull()
                profile = p
                p?.let {
                    displayName = it.display_name ?: ""
                    selectedAvatarId = it.avatar_color.ifEmpty { "lilo" }
                    heightInput = if (it.height_cm > 0) it.height_cm.toString() else ""
                    weightInput = if (it.weight_kg > 0f) it.weight_kg.toString() else ""
                    goalInput = it.goal_steps.toString()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFF39D353))
                        }
                    }
                    Button(
                        onClick = { scope.launch { supabase.auth.signOut(); onLogout() } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Logout", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isEditing) {
                // READ MODE
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161B22))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = getAvatarRes(profile?.avatar_color ?: "lilo")),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(90.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile?.display_name?.ifEmpty { "No name set" } ?: "No name set",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoCard(Modifier.weight(1f), "Height", if ((profile?.height_cm ?: 0) > 0) "${profile?.height_cm} cm" else "—")
                    InfoCard(Modifier.weight(1f), "Weight", if ((profile?.weight_kg ?: 0f) > 0f) "${profile?.weight_kg} kg" else "—")
                    InfoCard(Modifier.weight(1f), "Daily Goal", "%,d".format(profile?.goal_steps ?: 10000))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Lifetime Stats", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "👣", "Total Steps", "%,d".format(totalSteps))
                    StatCard(Modifier.weight(1f), "📍", "Distance", "%.1f km".format(totalDistance / 1000f))
                }

            } else {
                // EDIT MODE
                Text("Choose avatar", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))

                // Avatar grid — 5 per rad
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
                        // Fyll ut tomma platser om raden inte är full
                        repeat(5 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name", color = Color(0xFF8B949E)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353), unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily goal (steps)", color = Color(0xFF8B949E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF39D353), unfocusedBorderColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = heightInput,
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                try {
                                    if (userId != null) {
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
                                        profile = profile?.copy(
                                            display_name = displayName,
                                            avatar_color = selectedAvatarId,
                                            height_cm = heightInput.toIntOrNull() ?: 0,
                                            weight_kg = weightInput.toFloatOrNull() ?: 0f,
                                            goal_steps = goalInput.toIntOrNull() ?: 10000
                                        )
                                        isEditing = false
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39D353)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        else Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8B949E), fontSize = 11.sp)
    }
}