package com.example.hopfog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.*

private enum class AnnouncementSortMode(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    PRIORITY("Priority")
}

@Composable
fun HomePageContent(
    isOnline: Boolean,
    onSendSosClick: () -> Unit,
    onNewMessageClick: () -> Unit
) {
    val context = LocalContext.current
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var sortMode by remember { mutableStateOf(AnnouncementSortMode.NEWEST_FIRST) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        announcements = NetworkManager.getAnnouncements(context)
    }

    val sortedAnnouncements = remember(announcements, sortMode) {
        when (sortMode) {
            AnnouncementSortMode.NEWEST_FIRST ->
                announcements.sortedByDescending { parseTimestampToMillis(it.createdAt) }
            AnnouncementSortMode.OLDEST_FIRST ->
                announcements.sortedBy { parseTimestampToMillis(it.createdAt) }
            AnnouncementSortMode.PRIORITY ->
                announcements.sortedWith(
                    compareBy<Announcement> { announcementPriorityRank(it) }
                        .thenByDescending { parseTimestampToMillis(it.createdAt) }
                )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = HopFogLightBlue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionButtons(
                    onSendSosClick = onSendSosClick,
                    onNewMessageClick = onNewMessageClick
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Announcements",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort announcements",
                                tint = Color.DarkGray
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            AnnouncementSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.label,
                                            fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortMode = mode
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (mode == sortMode) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (sortedAnnouncements.isNotEmpty()) {
                    AnnouncementList(announcements = sortedAnnouncements)
                } else {
                    NoAnnouncementsMessage()
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onSendSosClick: () -> Unit,
    onNewMessageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionButton(
            text = "New Message",
            icon = Icons.Default.Message,
            color = HopFogBlue,
            onClick = onNewMessageClick,
            modifier = Modifier.weight(1f).aspectRatio(1f)
        )
        ActionButton(
            text = "Send SOS",
            icon = Icons.Default.Campaign,
            color = HopFogRed,
            onClick = onSendSosClick,
            modifier = Modifier.weight(1f).aspectRatio(1f)
        )
    }
}
@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AnnouncementList(announcements: List<Announcement>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(announcements) { announcement ->
            AnnouncementItem(announcement = announcement)
        }
    }
}

@Composable
private fun AnnouncementItem(announcement: Announcement) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF9C4)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Announcement",
                    tint = HopFogRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = announcement.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = announcement.message,
                color = Color.DarkGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            if (!announcement.createdAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(announcement.createdAt),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun NoAnnouncementsMessage() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No Announcements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "There are no SOS announcements from admins at this time. Stay safe and check back later.",
            textAlign = TextAlign.Center, color = Color.DarkGray, lineHeight = 20.sp
        )
    }
}

@Preview(name = "Home Page (Online)", showBackground = true)
@Composable
fun HomePageContentOnlinePreview() {
    HopFogTheme {
        HomePageContent(isOnline = true, onSendSosClick = {}, onNewMessageClick = {})
    }
}

@Preview(name = "Home Page (Offline)", showBackground = true)
@Composable
fun HomePageContentOfflinePreview() {
    HopFogTheme {
        HomePageContent(isOnline = false, onSendSosClick = {}, onNewMessageClick = {})
    }
}