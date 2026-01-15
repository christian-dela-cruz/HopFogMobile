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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape


val SosBackgroundColor = Color(0xFFD9534F)

@Composable
fun SosIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White), // White background for the icon
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SOS",
            color = SosBackgroundColor, // Red text color
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SosMessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }

    val quickReplies = listOf(
        "I am safe.", "Need medical help.",
        "We are all safe here.", "Need water and food.",
        "Danger here. Stay Away.", "Need rescue.",
        "Floodwaters rising.", "Share Location."
    )

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = SosBackgroundColor,
        bottomBar = {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickReplies) { reply ->
                        Button(
                            onClick = { chatViewModel.sendMessage(context, conversationId, reply) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.9f),
                                contentColor = Color.Black
                            )
                        ) { Text(reply, fontSize = 14.sp) }
                    }
                }
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Send Message", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.White,
                            focusedBorderColor = HopFogBlue,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            cursorColor = HopFogBlue,
                            unfocusedContainerColor = Color.Black.copy(alpha=0.3f),
                            focusedContainerColor = Color.Black.copy(alpha=0.3f)
                        )
                    )
                    IconButton(onClick = {
                        if (text.isNotBlank()) {
                            chatViewModel.sendMessage(context, conversationId, text)
                            text = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = HopFogBlue)
                    }
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