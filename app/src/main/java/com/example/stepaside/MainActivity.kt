package com.example.stepaside

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.stepaside.ui.theme.StepAsideTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
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
                val prefs2 = getSharedPreferences("stepaside_prefs", MODE_PRIVATE)
                var consentDone by remember { mutableStateOf(prefs2.getBoolean("consent_given", false)) }

                if (!consentDone) {
                    ConsentScreen(onConsentGiven = {
                        consentDone = true
                        requestPermissionsAndStart()
                    })
                } else {
                    var currentScreen by remember { mutableStateOf("home") }
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "home" -> StepScreen()
                            "walk" -> WalkScreen()
                            "history" -> HistoryScreen()
                            "achievements" -> AchievementsScreen()
                            "stats" -> StatsScreen()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0xFF161B22))
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { currentScreen = "home" }) {
                                Text("🏠", color = if (currentScreen == "home") Color(0xFF39D353) else Color(0xFF8B949E), fontSize = 20.sp)
                            }
                            TextButton(onClick = { currentScreen = "walk" }) {
                                Text("🗺️", color = if (currentScreen == "walk") Color(0xFF39D353) else Color(0xFF8B949E), fontSize = 20.sp)
                            }
                            TextButton(onClick = { currentScreen = "history" }) {
                                Text("📅", color = if (currentScreen == "history") Color(0xFF39D353) else Color(0xFF8B949E), fontSize = 20.sp)
                            }
                            TextButton(onClick = { currentScreen = "achievements" }) {
                                Text("🏆", color = if (currentScreen == "achievements") Color(0xFF39D353) else Color(0xFF8B949E), fontSize = 20.sp)
                            }
                            TextButton(onClick = { currentScreen = "stats" }) {
                                Text("📊", color = if (currentScreen == "stats") Color(0xFF39D353) else Color(0xFF8B949E), fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) startStepService()
        else permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startStepService() {
        val intent = Intent(this, com.example.stepaside.service.StepCounterService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}