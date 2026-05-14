package io.stepaside.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import io.github.jan.supabase.auth.auth
import io.stepaside.app.service.StepCounterService
import io.stepaside.app.ui.theme.StepAsideTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
            startStepService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = getSharedPreferences("stepaside_prefs", MODE_PRIVATE)
        val consentGiven = prefs.getBoolean("consent_given", false)

        if (consentGiven) {
            requestPermissionsAndStart()
        }

        setContent {
            StepAsideTheme {
                val context = LocalContext.current
                val prefs2 = context.getSharedPreferences("stepaside_prefs", MODE_PRIVATE)
                var consentDone by remember { mutableStateOf(prefs2.getBoolean("consent_given", false)) }
                var isLoadingAuth by remember { mutableStateOf(true) }
                var authDone by remember { mutableStateOf(false) }
                var onboardingDone by remember { mutableStateOf(prefs2.getBoolean("onboarding_done", false)) }

                LaunchedEffect(Unit) {
                    runCatching { supabase.auth.loadFromStorage() }
                        .onFailure { it.printStackTrace() }
                    authDone = supabase.auth.currentUserOrNull() != null
                    isLoadingAuth = false
                }

                if (isLoadingAuth) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D1117)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color(0xFF39D353))
                    }
                } else if (!consentDone) {
                    ConsentScreen(onConsentGiven = {
                        consentDone = true
                        requestPermissionsAndStart()
                    })
                } else if (!authDone) {
                    AuthScreen(onAuthSuccess = { authDone = true })
                } else if (!onboardingDone) {
                    OnboardingScreen(onDone = { onboardingDone = true })
                } else {
                    var currentScreen by remember { mutableStateOf("home") }
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp),
                        ) {
                            when (currentScreen) {
                                "home" -> StepScreen()
                                "walk" -> WalkScreen()
                                "history" -> HistoryScreen()
                                "achievements" -> AchievementsScreen()
                                "stats" -> StatsScreen()
                                "profile" -> ProfileScreen(onLogout = {
                                    authDone = false
                                    onboardingDone = false
                                })
                            }
                        }

                        NavigationBar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            containerColor = Color(0xFF161B22),
                            tonalElevation = 0.dp,
                        ) {
                            val itemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF39D353),
                                selectedTextColor = Color(0xFF39D353),
                                unselectedIconColor = Color(0xFF8B949E),
                                unselectedTextColor = Color(0xFF8B949E),
                                indicatorColor = Color(0xFF39D353).copy(alpha = 0.15f),
                            )
                            NavigationBarItem(
                                selected = currentScreen == "home",
                                onClick = { currentScreen = "home" },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 10.sp) },
                                colors = itemColors,
                            )
                            NavigationBarItem(
                                selected = currentScreen == "walk",
                                onClick = { currentScreen = "walk" },
                                icon = { Icon(Icons.Filled.DirectionsWalk, contentDescription = "Walk") },
                                label = { Text("Walk", fontSize = 10.sp) },
                                colors = itemColors,
                            )
                            NavigationBarItem(
                                selected = currentScreen == "history",
                                onClick = { currentScreen = "history" },
                                icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "History") },
                                label = { Text("History", fontSize = 10.sp) },
                                colors = itemColors,
                            )
                            NavigationBarItem(
                                selected = currentScreen == "achievements",
                                onClick = { currentScreen = "achievements" },
                                icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = "Achievements") },
                                label = { Text("Awards", fontSize = 10.sp) },
                                colors = itemColors,
                            )
                            NavigationBarItem(
                                selected = currentScreen == "stats",
                                onClick = { currentScreen = "stats" },
                                icon = { Icon(Icons.Filled.InsertChart, contentDescription = "Stats") },
                                label = { Text("Stats", fontSize = 10.sp) },
                                colors = itemColors,
                            )
                            NavigationBarItem(
                                selected = currentScreen == "profile",
                                onClick = { currentScreen = "profile" },
                                icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                                label = { Text("Profile", fontSize = 10.sp) },
                                colors = itemColors,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) startStepService() else permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startStepService() {
        val intent = Intent(this, StepCounterService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
