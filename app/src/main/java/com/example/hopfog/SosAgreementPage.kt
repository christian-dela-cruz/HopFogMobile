package com.example.hopfog

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogRed

@Composable
fun SosAgreementPage(onAgreed: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)) // Dark background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SOS Feature Terms of Use",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState) // Make the terms scrollable
        ) {
            TermItem(
                title = "For Emergency Use Only",
                description = "This feature is intended for genuine emergencies where you require immediate assistance. Misuse may result in being unable to access the service."
            )
            TermItem(
                title = "Clear and Concise Messages",
                description = "Send essential information. Use the quick reply buttons whenever possible. Avoid spamming the chat with repeated or unnecessary messages."
            )
            TermItem(
                title = "Respect the System",
                description = "This is an offline, low-bandwidth system. Sending many messages in a short period can slow it down for everyone. Please be patient and responsible."
            )
            TermItem(
                title = "Location Sharing",
                description = "When you share your location, it will be sent to the emergency operator. Only use this when you need to be found."
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Use the new SessionManager to save the agreement for the current user.
                SessionManager.setHasAgreedToSos(context, true)
                // Then, tell the AppMainPage to continue.
                onAgreed()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HopFogRed)
        ) {
            Text("I Understand and Agree", fontSize = 16.sp)
        }
    }
}

@Composable
fun TermItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = HopFogRed, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(description, color = Color.LightGray, fontSize = 16.sp, lineHeight = 22.sp)
    }
}