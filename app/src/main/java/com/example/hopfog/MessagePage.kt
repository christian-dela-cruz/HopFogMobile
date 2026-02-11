package com.example.hopfog

import android.widget.Toast
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
// import kotlinx.coroutines.launch

@Composable
fun MessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int
) {
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // --- We can comment out the cooldown logic for now, as BLE sending doesn't have a cooldown yet ---
    // val cooldownState by chatViewModel.cooldownState.collectAsState()
    // val isSendingEnabled = cooldownState is CooldownState.Ready
    val isSendingEnabled = true // For BLE, we'll just enable it by default
    // ---

    // --- Cooldown logic is commented out ---
    /*
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.cancelCooldown()
        }
    }
    */
    // ---

    // Scroll to the bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            MessageInput(
                // The onSendMessage lambda from the ViewModel is no longer used by the input field
                onSendMessage = { /* chatViewModel.sendMessage(context, conversationId, it) */ },
                isEnabled = isSendingEnabled,
                cooldownSeconds = 0 // (cooldownState as? CooldownState.CoolingDown)?.secondsRemaining ?: 0
            )
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
    onSendMessage: (String) -> Unit, // This parameter is no longer directly used but kept for structure
    isEnabled: Boolean,
    cooldownSeconds: Int
) {
    var text by remember { mutableStateOf("") }

    val context = LocalContext.current

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

        Button(
            onClick = {
                if (text.isNotBlank()) {
                    // --- THIS IS THE PRIMARY CHANGE ---

                    // 1. Call BleManager directly to send the message
                    val success = BleManager.sendMessage(text)

                    // 2. Provide user feedback
                    if (success) {
                        Toast.makeText(context, "Message sent via BLE", Toast.LENGTH_SHORT).show()
                        text = "" // Clear input after sending
                    } else {
                        Toast.makeText(context, "Failed to send: Not connected via BLE.", Toast.LENGTH_LONG).show()
                    }

                    /*
                    onSendMessage(text)
                    text = "" // Clear input after sending
                    */
                }
            },
            enabled = isEnabled && text.isNotBlank(), // Cooldown is disabled for now, so isEnabled is always true
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isEnabled) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            } else {
                Text(text = "$cooldownSeconds")
            }
        }
    }
}


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

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.messageText, color = textColor)
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