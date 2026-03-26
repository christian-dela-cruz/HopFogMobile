package com.example.hopfog

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogBackground

@Composable
fun NotificationsPage() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(NotificationHelper.NOTIFICATION_PREFS, Context.MODE_PRIVATE)
    }

    var showNotifications by remember { mutableStateOf(prefs.getBoolean(NotificationHelper.KEY_SHOW_NOTIFICATIONS, true)) }
    var sound by remember { mutableStateOf(prefs.getBoolean(NotificationHelper.KEY_SOUND, true)) }
    var vibrate by remember { mutableStateOf(prefs.getBoolean(NotificationHelper.KEY_VIBRATE, true)) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = HopFogBackground
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "CHAT NOTIFICATIONS",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 32.dp, bottom = 4.dp, top = 8.dp)
            )
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    text = "Show Notifications",
                    checked = showNotifications,
                    onCheckedChange = {
                        showNotifications = it
                        prefs.edit().putBoolean(NotificationHelper.KEY_SHOW_NOTIFICATIONS, it).apply()
                    }
                )
                Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(
                    icon = Icons.Default.VolumeUp,
                    text = "Sound",
                    checked = sound,
                    onCheckedChange = {
                        sound = it
                        prefs.edit().putBoolean(NotificationHelper.KEY_SOUND, it).apply()
                    }
                )
                Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    text = "Vibrate",
                    checked = vibrate,
                    onCheckedChange = {
                        vibrate = it
                        prefs.edit().putBoolean(NotificationHelper.KEY_VIBRATE, it).apply()
                    }
                )
            }
        }
    }
}