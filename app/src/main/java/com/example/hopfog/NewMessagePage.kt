package com.example.hopfog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun NewMessagePage(
    onUserSelected: (conversationId: Int, contactName: String) -> Unit
) {
    val context = LocalContext.current
    var userList by remember { mutableStateOf<List<SelectableUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch the list of users when the page is first displayed
    LaunchedEffect(Unit) {
        isLoading = true
        userList = NetworkManager.getAllUsers(context)
        isLoading = false
    }

    // --- The only change is wrapping the Column in a Scaffold ---
    // The AppMainPage will provide the TopAppBar, this just makes space for it.
    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Use padding from the scaffold
                .padding(horizontal = 16.dp) // Add our own horizontal padding
        ) {
            // We can remove the Title Text from here, as the TopAppBar will show it.
            // Text("Start New Chat", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (userList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No other users found.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(userList) { user ->
                        UserSelectItem(
                            user = user,
                            onClick = {
                                // When a user is clicked, we need to create the conversation
                                coroutineScope.launch {
                                    // Show a loading indicator while creating chat
                                    isLoading = true
                                    val response = NetworkManager.findOrCreateChatWithUser(context, user.id)
                                    if (response != null) {
                                        // Once chat is created, navigate to the message page
                                        onUserSelected(response.conversationId, response.contactName)
                                    }
                                    isLoading = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSelectItem(user: SelectableUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Text(
            text = user.username,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}