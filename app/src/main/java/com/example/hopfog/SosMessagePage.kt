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

val SosBackgroundColor = Color(0xFFD9534F)

@Composable
fun SosMessagePage(
    chatViewModel: ChatViewModel,
    conversationId: Int
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }

    // --- NEW: Get cooldown state from ViewModel ---
    val cooldownState by chatViewModel.cooldownState.collectAsState()
    val isSendingEnabled = cooldownState is CooldownState.Ready
    // ---

    val quickReplies = listOf(
        "I am safe.", "Need medical help.",
        "We are all safe here.", "Need water and food.",
        "Need rescue.", "Floodwaters rising.",
        "Danger here. Stay Away.","Share Location."
    )

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
        containerColor = SosBackgroundColor,
        bottomBar = {
            Column(modifier = Modifier.background(Color.Black)) {
                // Quick Replies Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Make the grid take up less space
                    modifier = Modifier.height(180.dp)
                ) {
                    items(quickReplies) { reply ->
                        Button(
                            onClick = {
                                // The check happens in the ViewModel, but good practice to have it here too
                                if (isSendingEnabled) {
                                    chatViewModel.sendMessage(context, conversationId, reply, "sos_request")
                                }
                            },
                            // --- NEW: Disable button during cooldown ---
                            enabled = isSendingEnabled,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White,
                                // Make disabled buttons visually distinct
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

                    // --- NEW: Updated IconButton with cooldown logic ---
                    IconButton(
                        onClick = {
                            if (isSendingEnabled && text.isNotBlank()) {
                                chatViewModel.sendMessage(context, conversationId, text, "sos_request")
                                text = ""
                            }
                        },
                        enabled = isSendingEnabled && text.isNotBlank()
                    ) {
                        if (isSendingEnabled) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = HopFogBlue)
                        } else {
                            // If on cooldown, show the remaining seconds
                            val secondsLeft = (cooldownState as? CooldownState.CoolingDown)?.secondsRemaining ?: 0
                            Text("$secondsLeft", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    // --- END OF NEW LOGIC ---
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