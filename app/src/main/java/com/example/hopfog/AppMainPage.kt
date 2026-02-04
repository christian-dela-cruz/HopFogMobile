package com.example.hopfog

import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.navigation.navArgument
import android.widget.Toast



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainPage(
    userViewModel: UserViewModel,
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current

    var isOnline by remember { mutableStateOf(true) }
    var showStatusPopup by remember { mutableStateOf(false) }

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf("home_content", "chats_list", "settings_content")
    val subLevelRoutes = setOf("account", "notifications", "help", "terms_of_service", "privacy_policy", "change_password")

    val isTopLevelDestination = currentRoute in topLevelRoutes
    val isMessagePage = currentRoute?.startsWith("messages/") == true || currentRoute?.startsWith("sos_messages/") == true


    LaunchedEffect(key1 = true) {
        val prefs = context.getSharedPreferences("HopFogPrefs", Context.MODE_PRIVATE)
        val lastCleanupTime = prefs.getLong("last_cleanup_time", 0L)
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = TimeUnit.DAYS.toMillis(1)

        // Check if more than 24 hours have passed since the last cleanup
        if ((currentTime - lastCleanupTime) > oneDayInMillis) {
            // It's time to run the cleanup. Launch a background task.
            launch(Dispatchers.IO) {
                val success = NetworkManager.runMessageCleanup(context)
                if (success) {
                    // If successful, save the current time as the new last cleanup time.
                    prefs.edit().putLong("last_cleanup_time", currentTime).apply()
                    Log.d("Cleanup", "Successfully ran message cleanup task.")
                }
            }
        } else {
            Log.d("Cleanup", "Not time for cleanup yet.")
        }
    }



    Scaffold(
        containerColor = HopFogBackground,
        topBar = {
            if (isTopLevelDestination) {
                CenterAlignedTopAppBar(
                    title = { Text("HopFog", fontWeight = FontWeight.Bold, fontSize = 32.sp) },
                    actions = {
                        IconButton(onClick = {
                            isOnline = !isOnline
                            showStatusPopup = true
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
                val contactName = navBackStackEntry?.arguments?.getString("contactName") ?: "Chat"
                TopAppBar(
                    title = { Text(contactName, color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { innerNavController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (currentRoute?.startsWith("sos_messages/") == true) SosBackgroundColor else HopFogBackground
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
            ) {
                composable("home_content") {
                    HomePageContent(
                        isOnline = isOnline,
                        onSendSosClick = {
                            // Check the current user's agreement status using the new SessionManager.
                            val hasAgreed = SessionManager.hasAgreedToSos(context)

                            if (hasAgreed) {
                                // If already agreed, go straight to creating the chat
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sosResponse = NetworkManager.findOrCreateSosChat(context)
                                    if (sosResponse != null) {
                                        innerNavController.navigate("sos_messages/${sosResponse.conversationId}/${sosResponse.contactName}")
                                    }
                                }
                            } else {
                                // If not agreed, go to the agreement page
                                innerNavController.navigate("sos_agreement")
                            }
                        }
                    )
                }

                // This is inside your NavHost in AppMainPage.kt

                composable("sos_agreement") {
                    SosAgreementPage(
                        onAgreed = {
                            // The SosAgreementPage already told the server the user agreed.
                            // All we need to do now is create the chat and navigate there.
                            CoroutineScope(Dispatchers.Main).launch {
                                val sosResponse = NetworkManager.findOrCreateSosChat(context)
                                if (sosResponse != null) {
                                    innerNavController.navigate("sos_messages/${sosResponse.conversationId}/${sosResponse.contactName}") {
                                        // This is important: it removes the agreement page from the back stack
                                        // so the user can't press "back" and go to it again.
                                        popUpTo("sos_agreement") { inclusive = true }
                                    }
                                } else {
                                    // Optional: Show a toast if for some reason the chat can't be created
                                    Toast.makeText(context, "Could not create SOS chat. Please try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }


                composable("chats_list") {
                    ChatsListPage(
                        chatViewModel = chatViewModel,
                        // Pass the navController to handle clicks
                        onConversationClick = { conversationId, contactName ->
                            innerNavController.navigate("messages/$conversationId/$contactName")
                        }
                    )
                }

                composable("settings_content") {
                    SettingsPage(
                        userViewModel = userViewModel,
                        navController = innerNavController,
                        onLogoutClicked = onLogout
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
                    val context = LocalContext.current

                    LaunchedEffect(conversationId) {
                        chatViewModel.loadMessages(context, conversationId, contactName)
                    }

                    MessagePage(
                        chatViewModel = chatViewModel,
                        conversationId = conversationId
                    )
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

                    // Load messages for this conversation
                    LaunchedEffect(conversationId) {
                        chatViewModel.loadMessages(context, conversationId, contactName)
                    }

                    SosMessagePage(
                        chatViewModel = chatViewModel,
                        conversationId = conversationId
                    )
                }

                composable("notifications") { NotificationsPage() }

                composable("account") {
                    // Pass the innerNavController to the AccountPage
                    AccountPage(userViewModel = userViewModel, navController = innerNavController)
                }

                composable("change_password") {
                    ChangePasswordPage(
                        onPasswordChanged = {
                            // When the password is changed successfully, pop back
                            innerNavController.popBackStack()
                        }
                    )
                }

                composable("help") {
                    HelpPage()
                }
                composable("terms_of_service") {
                    TermsOfServicePage()
                }
                composable("privacy_policy") {
                    PrivacyPolicyPage()
                }
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
                Text("• Access Point: ${if (isOnline) "HopFogAP1" else "None"}", color = Color.DarkGray)
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