package com.example.hopfog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hopfog.ui.theme.HopFogBackground

@Composable //Unsure of what to do in the account page
fun AccountPage(userViewModel: UserViewModel) {
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
            UserProfileCard(
                name = user?.username ?: "Loading...",
                email = user?.email ?: "..."
            )
        }
    }
}