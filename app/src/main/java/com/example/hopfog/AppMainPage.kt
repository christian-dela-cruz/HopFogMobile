package com.example.hopfog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    // --- NEW: Simplified state for permissions ---
    // We only care if we have BLE permissions, not if we are "online".
    var hasBlePermissions by remember { mutableStateOf(false) }
    var showStatusPopup by remember { mutableStateOf(false) }

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf("home_content", "chats_list", "settings_content")
    val subLevelRoutes = setOf("account", "notifications", "help", "terms_of_service", "privacy_policy", "change_password", "new_message_page")

    val isTopLevelDestination = currentRoute in topLevelRoutes
    val isMessagePage = currentRoute?.startsWith("messages/") == true || currentRoute?.startsWith("sos_messages/") == true

    // --- BLE PERMISSION LAUNCHER ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true

        hasBlePermissions = granted
        if (granted) {
            Log.d("AppMainPage", "BLE Permissions Granted")
            Toast.makeText(context, "Bluetooth permissions granted. Off-grid mode is available.", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("AppMainPage", "BLE Permissions Denied")
            Toast.makeText(context, "Bluetooth permissions are required for off-grid messaging.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Initial check for permissions ---
    LaunchedEffect(Unit) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    // This LaunchedEffect for services can remain as it is
    LaunchedEffect(key1 = true) {
        // ... (Your existing code for MessageCheckService and cleanup)
        Log.d("AppMainPage", "Launching background services setup.")
        val serviceIntent = Intent(context, MessageCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    Scaffold(
        containerColor = HopFogBackground,
        topBar = {
            if (isTopLevelDestination) {
                CenterAlignedTopAppBar(
                    title = { Text("HopFog", fontWeight = FontWeight.Bold, fontSize = 32.sp) },
                    actions = {
                        // --- SIMPLIFIED: One button to show status and request permissions if needed ---
                        IconButton(onClick = { showStatusPopup = true }) {
                            Icon(
                                imageVector = if (hasBlePermissions) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                contentDescription = "Connection Status",
                                tint = if (hasBlePermissions) HopFogGreen else HopFogRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = HopFogBackground,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            } else if (currentRoute in subLevelRoutes) {
                // ... (This part remains the same)
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
            } else if (isMessagePage) {
                // ... (This part remains the same)
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
            // --- The NavHost and its composables remain largely the same ---
            // They don't need to know about the BLE connection state.
            NavHost(
                navController = innerNavController,
                startDestination = "home_content",
            ) {
                composable("home_content") {
                    HomePageContent(
                        isOnline = hasBlePermissions, // The "isOnline" prop now just means "can we go off-grid?"

                        onSendSosClick = {
                            // Check if the user has already agreed to the SOS terms
                            if (SessionManager.hasAgreedToSos(context)) {
                                // If they have, find the chat and navigate directly
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sosResponse = NetworkManager.findOrCreateSosChat(context)
                                    if (sosResponse != null) {
                                        innerNavController.navigate("sos_messages/${sosResponse.conversationId}/${sosResponse.contactName}")
                                    } else {
                                        Toast.makeText(context, "Could not create SOS chat.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                // If they have NOT, navigate to the agreement page first
                                innerNavController.navigate("sos_agreement")
                            }
                        },
                        onNewMessageClick = { innerNavController.navigate("new_message_page") }
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
                        }
                    )
                }

                composable("chats_list") {
                    LaunchedEffect(Unit) {
                        chatViewModel.setContext(context)
                    }
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

                    // THE FIX: Call the new MessagePage with all required parameters.
                    // The `LaunchedEffect` that prepares the chat is now inside MessagePage itself.
                    MessagePage(
                        chatViewModel = chatViewModel,
                        conversationId = conversationId,
                        contactName = contactName
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
                    // This is now the correct call for the fixed SosMessagePage
                    SosMessagePage(
                        chatViewModel = chatViewModel,
                        conversationId = conversationId,
                        contactName = contactName
                    )
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

            // The ConnectionStatusPopup is updated to reflect the permission status
            ConnectionStatusPopup(
                isVisible = showStatusPopup,
                hasPermissions = hasBlePermissions,
                onDismiss = { showStatusPopup = false }
            )
        }
    }
}

// --- MODIFIED ConnectionStatusPopup ---
@Composable
fun ConnectionStatusPopup(isVisible: Boolean, hasPermissions: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.padding(top = 40.dp, start = 80.dp, end = 80.dp).fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
            border = BorderStroke(2.dp, if (hasPermissions) HopFogGreen else HopFogRed)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Off-Grid Status", fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Status", tint = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Status: ${if (hasPermissions) "Ready" else "Permissions Needed"}", color = Color.DarkGray)
                Text("• Mode: Transactional (On-Demand)", color = Color.DarkGray)
            }
        }
    }
}


@Composable
private fun AppBottomNavigation(navController: NavController) {
    // ... (This part remains the same)
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