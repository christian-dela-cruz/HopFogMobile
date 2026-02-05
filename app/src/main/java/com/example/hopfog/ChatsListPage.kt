package com.example.hopfog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogBlue


@Composable
fun ChatsListPage(
    chatViewModel: ChatViewModel,
    onConversationClick: (conversationId: Int, contactName: String) -> Unit,
    onNewMessageClick: () -> Unit
) {
    val context = LocalContext.current
    val conversations by chatViewModel.conversations.collectAsState()

    val isLoading by chatViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        chatViewModel.loadConversations(context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNewMessageClick() },
                containerColor = HopFogBlue,
                contentColor = Color.Black
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Chat")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Your Title Text is fine
            Text(
                text = "Chats",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (isLoading) {
                // If we are actively loading, show the progress circle
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (conversations.isEmpty()) {
                // If we are NOT loading and the list is empty, show the message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You have no conversations yet.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                // Otherwise, show the list of conversations
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(conversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = {
                                onConversationClick(conversation.conversationId, conversation.contactName)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ConversationItem(conversation: ChatConversation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD180)),
                contentAlignment = Alignment.Center
            ) {
                Text(conversation.contactName.first().toString(), fontSize = 24.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.contactName,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = conversation.lastMessage ?: "No messages yet",
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                // A safer way to handle potential timestamp format issues
                text = conversation.timestamp?.let { ts ->
                    ts.split(" ").getOrNull(1)?.substringBeforeLast(":") ?: ""
                } ?: "",
                color = HopFogBlue,
                fontSize = 12.sp
            )
        }
    }
}