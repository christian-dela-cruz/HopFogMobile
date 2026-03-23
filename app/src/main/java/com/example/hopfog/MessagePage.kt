package com.example.hopfog

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
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTimestampLight

private const val MAX_MESSAGE_LENGTH = 60
private const val COUNTER_DISPLAY_THRESHOLD = MAX_MESSAGE_LENGTH / 2

@Composable
fun MessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int
) {
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // --- NEW: Get cooldown state from ViewModel ---
    val cooldownState by chatViewModel.cooldownState.collectAsState()
    val isSendingEnabled = cooldownState is CooldownState.Ready
    // ---

    // This makes sure the timer is cancelled when you navigate away from this screen
    DisposableEffect(conversationId) {
        chatViewModel.startPolling(context, conversationId)
        onDispose {
            chatViewModel.stopPolling()
            chatViewModel.cancelCooldown()
        }
    }

    // Scroll to the bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Use the new MessageInput composable
            MessageInput(
                onSendMessage = { messageText ->
                    chatViewModel.sendMessage(context, conversationId, messageText)
                },
                isEnabled = isSendingEnabled,
                cooldownSeconds = (cooldownState as? CooldownState.CoolingDown)?.secondsRemaining ?: 0
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
    onSendMessage: (String) -> Unit,
    isEnabled: Boolean,
    cooldownSeconds: Int
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.background(Color(0xFF2B2B2B))
    ) {
        // Show character counter only when over half the limit
        if (text.length > COUNTER_DISPLAY_THRESHOLD) {
            Text(
                text = "${text.length}/$MAX_MESSAGE_LENGTH",
                color = if (text.length >= MAX_MESSAGE_LENGTH) Color(0xFFFF6B6B) else Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, top = 4.dp)
            )
        }

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) text = it },
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

            // This is now a standard Button to easily show the countdown text
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = "" // Clear input after sending
                    }
                },
                enabled = isEnabled && text.isNotBlank(),
                shape = CircleShape,
                modifier = Modifier.size(50.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isEnabled) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                } else {
                    // If on cooldown, show the remaining seconds
                    Text(text = "$cooldownSeconds")
                }
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

        Column(
            horizontalAlignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(backgroundColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = message.messageText, color = textColor)
            }
            if (message.sentAt.isNotBlank()) {
                Text(
                    text = formatTimestamp(message.sentAt),
                    color = HopFogTimestampLight,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
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