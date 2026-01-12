package com.example.hopfog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogRed
import com.example.hopfog.ui.theme.HopFogTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsPage(
    userViewModel: UserViewModel,
    navController: NavController,
    onLogoutClicked: () -> Unit
) {
    val user by userViewModel.user.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }

        item {
            UserProfileCard(
                name = user?.username ?: "Not Logged In",
                email = user?.email ?: "---")
        }

        // Account Settings Group
        item {
            SettingsGroup {
                SettingsItem(icon = Icons.Default.Person, text = "Account") { /* TODO */ }
                Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(icon = Icons.Default.Notifications, text = "Notifications") { /* TODO */ }
            }
        }

        // Help & Legal Group
        item {
            SettingsGroup {
                SettingsItem(icon = Icons.AutoMirrored.Filled.HelpOutline, text = "Help") {
                    navController.navigate("help")
                }
                Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(icon = Icons.Default.Gavel, text = "Terms of Service") {
                    navController.navigate("terms_of_service")
                }
                Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(icon = Icons.Default.Shield, text = "Privacy Policy") {
                    navController.navigate("privacy_policy")
                }
            }
        }

        item { InfoItem(text = "App Version: 1.0.0") }

        item {
            LogoutButton {
                userViewModel.onLogout()
                onLogoutClicked()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


// --- HELPER COMPOSABLES

@Composable
private fun UserProfileCard(name: String, email: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = email, fontSize = 14.sp, color = Color.LightGray)
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun InfoItem(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.DarkGray.copy(alpha = 0.4f),
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text("Logout", color = HopFogRed, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF2C3E50)
@Composable
fun SettingsPagePreview() {
    HopFogTheme {
        SettingsPage(
            userViewModel = viewModel(),
            navController = rememberNavController(),
            onLogoutClicked = {}
        )
    }
}