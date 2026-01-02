package com.example.hopfog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage() {
    var isOnline by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = HopFogBackground,
        topBar = {
            // --- CHANGE 1: Used CenterAlignedTopAppBar and increased title font size ---
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "HopFog",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp // Increased font size
                    )
                },
                navigationIcon = {
                    if (!isOnline) {
                        IconButton(onClick = { /* TODO: Handle 'X' click */ }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Connection")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isOnline = !isOnline }) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = "Connection Status",
                            tint = if (isOnline) HopFogGreen else HopFogRed
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = HopFogBackground,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.Black) {
                NavigationBarItem(
                    selected = true, onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false, onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = false, onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = HopFogLightBlue
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ActionButtons()
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
}

@Composable
private fun ActionButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionButton(
            text = "New Message",
            icon = Icons.Default.Message,
            color = HopFogBlue,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f) // Makes the button a square
        )
        ActionButton(
            text = "Send SOS",
            icon = Icons.Default.Campaign,
            color = HopFogRed,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f) // Makes the button a square
        )
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Button(
        onClick = { /*TODO*/ },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- CHANGE 2: Increased Icon and Text size ---
            Icon(
                icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(40.dp) // Increased icon size
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp // Increased text size
            )
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
            Image(
                imageVector = Icons.Default.Face,
                contentDescription = "User Avatar",
                modifier = Modifier.size(40.dp).clip(CircleShape).background(HopFogYellow)
            )
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

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    HopFogTheme {
        HomePage()
    }
}