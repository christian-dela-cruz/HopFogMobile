package com.example.hopfog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hopfog.ui.theme.HopFogBackground

@Composable
fun AccountPage(userViewModel: UserViewModel, navController: NavController) {
    val user by userViewModel.user.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        UserProfileCard(
            name = user?.username ?: "Loading...",
            email = user?.email ?: "..."
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup {
            SettingsItem(
                icon = Icons.Default.Lock,
                text = "Change Password"
            ) {
                navController.navigate("change_password")
            }
        }
    }
}