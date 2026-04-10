package com.example.stepaside

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsentScreen(onConsentGiven: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("stepaside_prefs", android.content.Context.MODE_PRIVATE)

    var analyticsConsent by remember { mutableStateOf(false) }
    var dataShareConsent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text("👋", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Welcome to StepAside",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Before you start, we'd like to be transparent about how we handle your data.",
                color = Color(0xFF8B949E),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // What we always collect
            ConsentSection(
                title = "What we always collect",
                items = listOf(
                    "📍 Location during walks (stored locally on your device)",
                    "👣 Step count (stored locally on your device)",
                    "📅 Activity history (stored locally on your device)"
                ),
                note = "This data never leaves your device without your consent."
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Optional consent 1
            ConsentToggle(
                emoji = "📊",
                title = "Anonymous usage analytics",
                description = "Help us improve the app by sharing anonymous usage data. No personal information is included.",
                checked = analyticsConsent,
                onCheckedChange = { analyticsConsent = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional consent 2
            ConsentToggle(
                emoji = "🔬",
                title = "Anonymous movement data",
                description = "Contribute anonymized, aggregated movement data to research. This data cannot be used to identify you.",
                checked = dataShareConsent,
                onCheckedChange = { dataShareConsent = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "You can change these settings at any time in the app.",
                color = Color(0xFF8B949E),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    prefs.edit()
                        .putBoolean("consent_given", true)
                        .putBoolean("consent_analytics", analyticsConsent)
                        .putBoolean("consent_data_share", dataShareConsent)
                        .apply()
                    onConsentGiven()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39D353)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Get Started", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "By continuing you agree to our Privacy Policy. We are GDPR compliant.",
                color = Color(0xFF8B949E),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ConsentSection(title: String, items: List<String>, note: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        items.forEach { item ->
            Text(item, color = Color(0xFF8B949E), fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(note, color = Color(0xFF39D353), fontSize = 12.sp)
    }
}

@Composable
fun ConsentToggle(emoji: String, title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text("$emoji $title", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color(0xFF8B949E), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF39D353),
                uncheckedThumbColor = Color(0xFF8B949E),
                uncheckedTrackColor = Color(0xFF21262D)
            )
        )
    }
}