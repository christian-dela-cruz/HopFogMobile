package com.example.hopfog

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hopfog.ui.theme.HopFogBlue
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int, // We still need this to load initial mock data
    contactName: String, // We need this to set the title
) {
    val messages by chatViewModel.messages.collectAsState()
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // --- NEW: Connection Lifecycle Management ---
    LaunchedEffect(key1 = Unit) {
        // When the page first loads, tell the ViewModel to prepare the chat
        // and connect to the BLE device.
        chatViewModel.prepareChat(conversationId, contactName)
        chatViewModel.connectToHub()
    }

    // NEW: Ensure we disconnect when the user leaves the page
    DisposableEffect(key1 = Unit) {
        onDispose {
            chatViewModel.disconnectFromHub()
        }
    }

//    LaunchedEffect(connectionStatus) {
//        if (connectionStatus is ConnectionStatus.Error) {
//            Toast.makeText(context, (connectionStatus as ConnectionStatus.Error).message, Toast.LENGTH_LONG).show()
//        } else if (connectionStatus is ConnectionStatus.Connected) {
//            // Let's log in automatically once connected
//            val username = SessionManager.getUsername(context)
//            val password = "" // Password can be empty for now
//            val json = org.json.JSONObject().apply {
//                put("action", "login")
//                put("username", username)
//                put("password", password)
//            }
//            BleManager.sendJson(json.toString())
//        }
//    }

    // Scroll to the bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // You can add a top bar to show connection status
            TopAppBar(
                title = { Text(contactName) },
                actions = {
                    val statusText = when(connectionStatus) {
                        ConnectionStatus.Connected -> "Connected"
                        ConnectionStatus.Connecting -> "Connecting..."
                        ConnectionStatus.Scanning -> "Scanning..."
                        is ConnectionStatus.Error -> "Error"
                        else -> "Offline"
                    }
                    Text(statusText, modifier = Modifier.padding(end=16.dp))
                }
            )
        },
        bottomBar = {
            MessageInput(chatViewModel = chatViewModel)
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }
    }
}


@Composable
fun MessageInput(
    chatViewModel: ChatViewModel
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()

    // Determine if the send button should be enabled
    val isSendEnabled = text.isNotBlank() && connectionStatus == ConnectionStatus.Connected

    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HopFogBlue,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = HopFogBlue
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        // --- REVISED onClick LOGIC ---
        // It's much simpler now. It just calls the ViewModel.
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    chatViewModel.sendMessage(context, text)
                    text = "" // Clear the input field immediately
                }
            },
            enabled = isSendEnabled, // Use the new enabled state
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
        }
    }
}

// --- MessageBubble and UserInitialIcon are unchanged ---

@Composable
fun MessageBubble(
    message: Message,
    iconContent: @Composable (() -> Unit)? = null
) {
    val alignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start

    val backgroundColor = when {
        message.senderUsername.equals("admin", ignoreCase = true) -> Color.White
        message.isFromCurrentUser -> HopFogBlue
        else -> Color.DarkGray
    }

    val textColor = if (message.senderUsername.equals("admin", ignoreCase = true) || message.isFromCurrentUser) {
        Color.Black
    } else {
        Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment),
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isFromCurrentUser) {
            if (iconContent != null) {
                iconContent()
            } else {
                val initial = message.senderUsername.firstOrNull()?.uppercaseChar() ?: 'U'
                UserInitialIcon(initial = initial)
            }
        }

        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(backgroundColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = message.messageText, color = textColor)
            }
            if (message.isFromCurrentUser) {
                Text(
                    text = message.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End).padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun UserInitialIcon(initial: Char, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}