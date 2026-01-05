package com.example.hopfog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContentPage(title: String, body: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Text(
                text = body,
                fontSize = 16.sp,
                color = Color.LightGray,
                lineHeight = 24.sp
            )
        }
    }
}

// --- Specific Pages ---

@Composable
fun HelpPage() {
    ContentPage(
        title = "Help",
        body = "If you are experiencing issues with the app, please try restarting it.\n\nFor SOS emergencies, always contact your local emergency services first. This app is a supplementary tool.\n\nFor further assistance, please contact our support team at support@hopfog.com."
    )
}

@Composable
fun TermsOfServicePage() {
    ContentPage(
        title = "Terms of Service",
        body = "Welcome to HopFog.\n\nBy using our app, you agree to these terms. Please read them carefully.\n\n1. Use of Service: HopFog provides a decentralized communication service. You agree not to misuse the service.\n\n2. SOS Feature: The SOS feature is intended for emergency situations but is not a replacement for official emergency services.\n\n3. Privacy: Your privacy is important to us. Our Privacy Policy explains how we handle your personal data.\n\n(Example lang muna.)"
    )
}

@Composable
fun PrivacyPolicyPage() {
    ContentPage(
        title = "Privacy Policy",
        body = "HopFog is built with privacy in mind.\n\n1. Data Collection: We do not store your messages on our servers. Communication is peer-to-peer.\n\n2. Location Data: Location data is used only to find nearby users and is not stored or tracked.\n\n3. Personal Information: Your username is public to nearby users when you are online. Your email is used for account recovery and is not shared.\n\n(Example lang muna.)"
    )
}