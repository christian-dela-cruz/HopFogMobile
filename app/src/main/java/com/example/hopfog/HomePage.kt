package com.example.hopfog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.hopfog.ui.theme.*

@Composable
fun HomePageContent(
    isOnline: Boolean,
    onSendSosClick: () -> Unit,
    onNewMessageClick: () -> Unit // <-- CHANGE #1: Add the new parameter here
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = HopFogLightBlue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- CHANGE #2: Pass the new click handler down ---
                ActionButtons(
                    onSendSosClick = onSendSosClick,
                    onNewMessageClick = onNewMessageClick // <-- Pass it here
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Nearby",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isOnline) {
                    NearbyUserList()
                } else {
                    ReadyToHopMessage()
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onSendSosClick: () -> Unit,
    onNewMessageClick: () -> Unit // <-- CHANGE #3: Accept the new parameter here
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionButton(
            text = "New Message",
            icon = Icons.Default.Message,
            color = HopFogBlue,
            onClick = onNewMessageClick, // <-- CHANGE #4: Use the parameter here, replacing the TODO
            modifier = Modifier.weight(1f).aspectRatio(1f)
        )
        ActionButton(
            text = "Send SOS",
            icon = Icons.Default.Campaign,
            color = HopFogRed,
            onClick = onSendSosClick,
            modifier = Modifier.weight(1f).aspectRatio(1f)
        )
    }
}
@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
@Composable
private fun NearbyUserList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NearbyUserItem(name = "Chuckie")
    }
}

@Composable
private fun NearbyUserItem(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF9C4)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(imageVector = Icons.Default.Face, contentDescription = "User Avatar", modifier = Modifier.size(40.dp).clip(CircleShape).background(HopFogYellow))
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
private fun ReadyToHopMessage() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Ready to Hop?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Go online to discover and message other HopFog users within 47.6 meters, even without internet. Tap the icon above to get started.",
            textAlign = TextAlign.Center, color = Color.DarkGray, lineHeight = 20.sp
        )
    }
}

// --- Previews updated to satisfy the new parameter ---
@Preview(name = "Home Page (Online)", showBackground = true)
@Composable
fun HomePageContentOnlinePreview() {
    HopFogTheme {
        HomePageContent(isOnline = true, onSendSosClick = {}, onNewMessageClick = {}) // <-- Added empty click handler
    }
}

@Preview(name = "Home Page (Offline)", showBackground = true)
@Composable
fun HomePageContentOfflinePreview() {
    HopFogTheme {
        HomePageContent(isOnline = false, onSendSosClick = {}, onNewMessageClick = {}) // <-- Added empty click handler
    }
}