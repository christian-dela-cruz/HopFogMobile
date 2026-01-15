package com.example.hopfog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.hopfog.ui.theme.HopFogBlue

@Composable
fun MessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }

    // Scroll to the bottom when new messages arrive
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
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
                        unfocusedTextColor = Color.White
                    )
                )
                IconButton(onClick = {
                    chatViewModel.sendMessage(context, conversationId, text)
                    text = "" // Clear the input field
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = HopFogBlue)
                }
            }
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
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isFromCurrentUser) HopFogBlue else Color.DarkGray
    val textColor = if (message.isFromCurrentUser) Color.Black else Color.White

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.messageText, color = textColor)
        }
    }
}