package com.example.hopfog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogBlue

// Define the color here if it's specific to this page
val SosBackgroundColor = Color(0xFF5C1B1B)

@Composable
fun SosMessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int, // Keep for preparing the chat
    contactName: String // Keep for preparing the chat
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }

    // Get the connection status from the ViewModel
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()
    val isConnected = connectionStatus is ConnectionStatus.Connected

    val quickReplies = listOf(
        "I am safe.", "Need medical help.",
        "We are all safe here.", "Need water and food.",
        "Need rescue.", "Floodwaters rising.",
        "Danger here. Stay Away.","Share Location."
    )

    // --- NEW LIFECYCLE MANAGEMENT (Same as MessagePage) ---
    LaunchedEffect(key1 = Unit) {
        chatViewModel.prepareChat(conversationId, contactName)
        chatViewModel.connectToHub()
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            chatViewModel.disconnectFromHub()
        }
    }
    // ---

    // Scroll to the bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        // Use the SOS color for the main container
        containerColor = SosBackgroundColor,
        bottomBar = {
            Column(modifier = Modifier.background(Color.Black)) {
                // Quick Replies Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(quickReplies) { reply ->
                        Button(
                            onClick = {
                                // REVISED: Use the new sendMessage signature
                                if (isConnected) {
                                    chatViewModel.sendMessage(context, reply)
                                }
                            },
                            enabled = isConnected, // Button is enabled only when connected
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF3A3A3C),
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Text(reply, fontSize = 14.sp)
                        }
                    }
                }
                // Standard Message Input
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Send Message", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF1C1C1E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            // REVISED: Use the new sendMessage signature
                            if (isConnected && text.isNotBlank()) {
                                chatViewModel.sendMessage(context, text)
                                text = ""
                            }
                        },
                        enabled = isConnected && text.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = HopFogBlue)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }
    }
}