package com.example.hopfog

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogGreen
import com.example.hopfog.ui.theme.HopFogRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainPage(
    userViewModel: UserViewModel,
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current

    var isOnline by remember { mutableStateOf(false) }
    var showStatusPopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isOnline = NetworkManager.checkStatus()
            delay(10_000)
        }
    }

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf("home_content", "chats_list", "settings_content")
    val subLevelRoutes = setOf("account", "notifications", "help", "terms_of_service", "privacy_policy", "change_password", "new_message_page")

    val isTopLevelDestination = currentRoute in topLevelRoutes
    val isMessagePage = currentRoute?.startsWith("messages/") == true || currentRoute?.startsWith("sos_messages/") == true


    // --- THIS IS THE CORRECT LOCATION FOR THIS LOGIC ---
    LaunchedEffect(key1 = true) {
        // --- START OF FIX: START THE SERVICE HERE ---
        Log.d("AppMainPage", "Launching background services setup.")
        val userId = SessionManager.getUserId(context)
        if (userId != -1) {
            val serviceIntent = Intent(context, MessageCheckService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
        // --- END OF FIX ---

        // The cleanup task logic can remain here as well.
        val prefs = context.getSharedPreferences("HopFogPrefs", Context.MODE_PRIVATE)
        val lastCleanupTime = prefs.getLong("last_cleanup_time", 0L)
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = TimeUnit.DAYS.toMillis(1)

        if ((currentTime - lastCleanupTime) > oneDayInMillis) {
            launch(Dispatchers.IO) {
                val success = NetworkManager.runMessageCleanup(context)
                if (success) {
                    prefs.edit().putLong("last_cleanup_time", currentTime).apply()
                    Log.d("Cleanup", "Successfully ran message cleanup task.")
                }
            }
        } else {
            Log.d("Cleanup", "Not time for cleanup yet.")
        }
    }

    // --- REMOVED FROM HERE ---
    // The service starting code that was here has been moved up into the LaunchedEffect block.

    Scaffold(
        containerColor = HopFogBackground,
        topBar = {
            if (isTopLevelDestination) {
                CenterAlignedTopAppBar(
                    title = { Text("HopFog", fontWeight = FontWeight.Bold, fontSize = 32.sp) },
                    actions = {
                        IconButton(onClick = {
                            showStatusPopup = !showStatusPopup
                        }) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                contentDescription = "Connection Status",
                                tint = if (isOnline) HopFogGreen else HopFogRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = HopFogBackground,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }  else if (currentRoute in subLevelRoutes) {
                TopAppBar(
                    title = {
                        val titleText = when(currentRoute) {
                            "account" -> "Account"
                            "notifications" -> "Notifications"
                            "help" -> "Help"
                            "change_password" -> "Change Password"
                            "new_message_page" -> "New Message"
                            else -> ""
                        }
                        Text(titleText, color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { innerNavController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HopFogBackground)
                )
            }
            else if (isMessagePage) {
                // Determine background color based on SOS or regular chat
                val sosBackgroundColor = Color(0xFF5C1B1B)
                val backgroundColor = if (currentRoute?.startsWith("sos_messages/") == true) sosBackgroundColor else HopFogBackground
                val contactName = navBackStackEntry?.arguments?.getString("contactName") ?: "Chat"

                TopAppBar(
                    title = { Text(contactName, color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { innerNavController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                AppBottomNavigation(navController = innerNavController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = innerNavController,
                startDestination = "home_content",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
            ) {
                composable("home_content") {
                    HomePageContent(
                        isOnline = isOnline,
                        onSendSosClick = {
                            val hasAgreed = SessionManager.hasAgreedToSos(context)
                            if (hasAgreed) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sosResponse = NetworkManager.findOrCreateSosChat(context)
                                    if (sosResponse != null) {
                                        innerNavController.navigate("sos_messages/${sosResponse.conversationId}/${sosResponse.contactName}")
                                    }
                                }
                            } else {
                                innerNavController.navigate("sos_agreement")
                            }
                        },
                        onNewMessageClick = {
                            innerNavController.navigate("new_message_page")
                        }
                    )
                }

                composable("sos_agreement") {
                    SosAgreementPage(
                        onAgreed = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val sosResponse = NetworkManager.findOrCreateSosChat(context)
                                if (sosResponse != null) {
                                    innerNavController.navigate("sos_messages/${sosResponse.conversationId}/${sosResponse.contactName}") {
                                        popUpTo("sos_agreement") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Could not create SOS chat. Please try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onBack = { innerNavController.popBackStack() }
                    )
                }

                composable("chats_list") {
                    ChatsListPage(
                        chatViewModel = chatViewModel,
                        onConversationClick = { conversationId, contactName ->
                            innerNavController.navigate("messages/$conversationId/$contactName")
                        },
                        onNewMessageClick = {
                            innerNavController.navigate("new_message_page")
                        }
                    )
                }

                composable("settings_content") {
                    SettingsPage(
                        userViewModel = userViewModel,
                        navController = innerNavController,
                        onLogoutClicked = {
                            val serviceIntent = Intent(context, MessageCheckService::class.java)
                            context.stopService(serviceIntent)
                            SessionManager.clearSession(context)
                            onLogout()
                        }
                    )
                }
                composable(
                    "messages/{conversationId}/{contactName}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.IntType },
                        navArgument("contactName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: 0
                    val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
                    LaunchedEffect(conversationId) {
                        chatViewModel.loadMessages(context, conversationId, contactName)
                    }
                    MessagePage(chatViewModel = chatViewModel, conversationId = conversationId)
                }
                composable(
                    "sos_messages/{conversationId}/{contactName}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.IntType },
                        navArgument("contactName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: 0
                    val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
                    LaunchedEffect(conversationId) {
                        chatViewModel.loadMessages(context, conversationId, contactName)
                    }
                    SosMessagePage(chatViewModel = chatViewModel, conversationId = conversationId)
                }
                composable("new_message_page") {
                    NewMessagePage(
                        onUserSelected = { conversationId, contactName ->
                            // Navigate to the message page with the new conversation details
                            innerNavController.navigate("messages/$conversationId/$contactName") {
                                // Remove the "New Message" page from the back stack
                                popUpTo("new_message_page") { inclusive = true }
                            }
                        }
                    )
                }
                composable("notifications") {
                    NotificationsPage()
                }
                composable("account") { AccountPage(userViewModel = userViewModel, navController = innerNavController) }
                composable("change_password") { ChangePasswordPage(onPasswordChanged = { innerNavController.popBackStack() }) }
                composable("help") { HelpPage() }
                composable("terms_of_service") { TermsOfServicePage() }
                composable("privacy_policy") { PrivacyPolicyPage() }
            }

            ConnectionStatusPopup(
                isVisible = showStatusPopup,
                isOnline = isOnline,
                onDismiss = { showStatusPopup = false }
            )
        }
    }
}

@Composable
fun ConnectionStatusPopup(isVisible: Boolean, isOnline: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .padding(top = 40.dp, start = 80.dp, end = 80.dp)
            .fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
            border = BorderStroke(2.dp, if (isOnline) HopFogGreen else HopFogRed)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connection Status", fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Status", tint = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Status: ${if (isOnline) "Connected" else "Disconnected"}", color = Color.DarkGray)
                Text("• Access Point: ${if (isOnline) ESP32ConnectionManager.ESP32_SSID else "None"}", color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = Color.Black) {
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == "home_content" } == true,
            onClick = { navController.navigate("home_content") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == "chats_list" } == true,
            onClick = { navController.navigate("chats_list") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
            label = { Text("Chats") }
        )
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == "settings_content" } == true,
            onClick = { navController.navigate("settings_content") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
fun PlaceholderPage(pageName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$pageName Page", fontSize = 32.sp, color = Color.White)
    }
}