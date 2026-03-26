package com.example.hopfog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTimestampDark

private enum class ChatSortMode(val label: String, val icon: ImageVector) {
    NEWEST_FIRST("Newest", Icons.Default.KeyboardDoubleArrowDown),
    OLDEST_FIRST("Oldest", Icons.Default.KeyboardDoubleArrowUp)
}


@Composable
fun ChatsListPage(
    chatViewModel: ChatViewModel,
    onConversationClick: (conversationId: Int, contactName: String) -> Unit,
    onNewMessageClick: () -> Unit
) {
    val context = LocalContext.current
    val conversations by chatViewModel.conversations.collectAsState()

    val isLoading by chatViewModel.isLoading.collectAsState()
    var chatSortMode by remember { mutableStateOf(ChatSortMode.NEWEST_FIRST) }

    LaunchedEffect(Unit) {
        chatViewModel.loadConversations(context)
    }

    val sortedConversations = remember(conversations, chatSortMode) {
        when (chatSortMode) {
            ChatSortMode.NEWEST_FIRST ->
                conversations.sortedByDescending { parseTimestampToMillis(it.timestamp) }
            ChatSortMode.OLDEST_FIRST ->
                conversations.sortedBy { parseTimestampToMillis(it.timestamp) }
        }
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
            // Title and sort chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chats",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChatSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == chatSortMode,
                            onClick = { chatSortMode = mode },
                            label = { Text(mode.label, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HopFogBlue,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (mode == chatSortMode) HopFogBlue else Color.Gray
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                // If we are actively loading, show the progress circle
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (sortedConversations.isEmpty()) {
                // If we are NOT loading and the list is empty, show the message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You have no conversations yet.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                // Otherwise, show the list of conversations
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedConversations) { conversation ->
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
                text = formatTimestamp(conversation.timestamp),
                color = HopFogTimestampDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}