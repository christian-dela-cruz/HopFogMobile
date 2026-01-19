package com.example.hopfog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.hopfog.ui.theme.HopFogBackground

// Update the function signature to accept NavController
@Composable
fun AccountPage(userViewModel: UserViewModel, navController: NavController) {
    val user by userViewModel.user.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = HopFogBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Your existing UserProfileCard
            UserProfileCard(
                name = user?.username ?: "Loading...",
                email = user?.email ?: "..."
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Add the clickable setting item for changing the password
            SettingItem(
                title = "Change Password",
                onClick = {
                    navController.navigate("change_password")
                }
            )
        }
    }
}

// A reusable composable for settings options
@Composable
fun SettingItem(title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go to $title",
                tint = Color.Gray
            )
        }
        Divider(color = Color.DarkGray)
    }
}