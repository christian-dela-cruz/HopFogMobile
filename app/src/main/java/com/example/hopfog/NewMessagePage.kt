package com.example.hopfog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.example.hopfog.ui.theme.HopFogGreen
import kotlinx.coroutines.launch

@Composable
fun NewMessagePage(
    onUserSelected: (conversationId: Int, otherUserId: Int, contactName: String) -> Unit
) {
    val context = LocalContext.current
    // Full fetched list (admin already excluded on server side via role filter)
    var allUsers by remember { mutableStateOf<List<SelectableUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showOnlineOnly by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Fetch the list of users when the page is first displayed, excluding admin accounts
    LaunchedEffect(Unit) {
        isLoading = true
        allUsers = NetworkManager.getAllUsers(context).filter { !it.role.equals("admin", ignoreCase = true) }
        isLoading = false
    }

    // Derive the displayed list from filter state
    val displayedUsers = remember(allUsers, showOnlineOnly, searchQuery) {
        allUsers.filter { user ->
            (!showOnlineOnly || user.isOnline) &&
            (searchQuery.isBlank() || user.username.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name…", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HopFogBlue,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = HopFogBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Online-only / All-users filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showOnlineOnly,
                    onClick = { showOnlineOnly = true },
                    label = { Text("Online Only", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HopFogGreen,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = !showOnlineOnly,
                    onClick = { showOnlineOnly = false },
                    label = { Text("All Users", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HopFogBlue,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayedUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showOnlineOnly) "No users are online right now." else "No other users found.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayedUsers) { user ->
                        UserSelectItem(
                            user = user,
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val response = NetworkManager.findOrCreateChatWithUser(context, user.id)
                                    if (response != null) {
                                        onUserSelected(response.conversationId, user.id, response.contactName)
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.username,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (user.isOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(HopFogGreen)
                )
            }
        }
    }
}