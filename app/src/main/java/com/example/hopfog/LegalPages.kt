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
        body = "Welcome to HopFog.\n\nBy accessing or using the HopFog application, you agree to be bound by these Terms of Service. If you do not agree, please do not use the app.\n\n1. Use of Service\nHopFog provides a decentralized, offline-capable communication platform designed for emergency and community use. You agree to use the service responsibly, lawfully, and in good faith. You must not misuse the service to harass, threaten, or harm others.\n\n2. Account Registration\nAccounts are created by community administrators. You are responsible for maintaining the confidentiality of your login credentials. You must not share your account or allow unauthorized access.\n\n3. SOS Feature\nThe SOS feature is designed to help users communicate during emergencies such as natural disasters. It is a supplementary communication tool and is not a replacement for official emergency services (e.g., 911 or local equivalents). Always contact official emergency responders first.\n\n4. User Conduct\nYou agree not to:\n• Send false or misleading SOS alerts.\n• Transmit spam, abusive content, or illegal material.\n• Attempt to disrupt or interfere with the service or its infrastructure.\n\n5. Content Responsibility\nYou are solely responsible for the content you send through the app. HopFog does not monitor or endorse user-generated messages.\n\n6. Service Availability\nHopFog relies on local network infrastructure (e.g., ESP32 access points). Service availability depends on hardware deployment and is not guaranteed at all times or locations.\n\n7. Limitation of Liability\nHopFog and its developers are not liable for any damages, losses, or harm arising from the use or inability to use the service, including during emergencies.\n\n8. Changes to Terms\nWe may update these terms from time to time. Continued use of the app constitutes acceptance of the revised terms.\n\n9. Contact\nFor questions about these Terms of Service, please contact support@hopfog.com."
    )
}

@Composable
fun PrivacyPolicyPage() {
    ContentPage(
        title = "Privacy Policy",
        body = "HopFog is built with your privacy in mind. This policy explains how we collect, use, and protect your information.\n\n1. Data Collection\nHopFog collects minimal data necessary to provide the service:\n• Account information (username and email) provided during registration by your community administrator.\n• Messages sent through the app are transmitted via the local HopFog network and are not stored on external servers.\n\n2. Message Privacy\nMessages are transmitted over a local peer-to-peer network through the HopFog access point. We do not store, read, or analyze the content of your messages on any cloud server. Messages are subject to automatic cleanup to maintain system performance.\n\n3. Location Data\nHopFog does not track or store your GPS location. Proximity-based features operate solely through local network connectivity.\n\n4. Personal Information\nYour username is visible to other users within your community. Your email address is used solely for account recovery purposes and is never shared with third parties.\n\n5. Data Security\nWe implement reasonable security measures to protect your account information. However, no system is completely secure, and we cannot guarantee absolute security.\n\n6. Third-Party Services\nHopFog does not share your data with third-party advertisers, analytics providers, or data brokers.\n\n7. Data Retention\nAccount data is retained as long as your account is active. Messages are periodically cleaned up from the local system. You may request account deletion by contacting your community administrator.\n\n8. Children's Privacy\nHopFog is not intended for children under 13. We do not knowingly collect information from children under 13.\n\n9. Changes to This Policy\nWe may update this Privacy Policy from time to time. Any changes will be communicated through the app.\n\n10. Contact\nFor questions or concerns about this Privacy Policy, please contact support@hopfog.com."
    )
}