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
import com.google.gson.Gson

@Composable
fun MessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int
) {
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to the bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Pass the ChatViewModel to the MessageInput
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
    chatViewModel: ChatViewModel // Use the ViewModel to add messages to the UI
) {
    var text by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
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
                    val messageContent = text
                    isLoading = true

                    // Add the message to the UI immediately with a "sending" status
                    // This assumes your Message data class has a status field.
                    // If not, you may need to adapt this part.
                    chatViewModel.addProvisionalMessage(messageContent)
                    text = "" // Clear input

                    // Create JSON for the message
                    val messageData = mapOf(
                        "action" to "sendMessage",
                        "text" to messageContent
                        // You could add conversationId, senderId, etc. here
                    )
                    val jsonString = Gson().toJson(messageData)

                    // Perform the BLE transaction
                    BleManager.performTransaction(jsonString, object : BleTransactionCallback {
                        override fun onTransactionSuccess(response: String) {
                            isLoading = false
                            // NEW: Update the message status to "delivered"
                            chatViewModel.updateLastMessageStatus("delivered")
                            Toast.makeText(context, "Message Delivered", Toast.LENGTH_SHORT).show()
                        }

                        override fun onTransactionFailure(error: String) {
                            isLoading = false
                            // NEW: Update the message status to "failed"
                            chatViewModel.updateLastMessageStatus("failed")
                            Toast.makeText(context, "Send Failed: $error", Toast.LENGTH_LONG).show()
                        }
                    })
                }
            },
            enabled = !isLoading && text.isNotBlank(),
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = HopFogBlue)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
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