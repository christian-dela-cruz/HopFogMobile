package com.example.hopfog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogGreen
import com.example.hopfog.ui.theme.HopFogRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainPage() {
    val innerNavController = rememberNavController()
    var isOnline by remember { mutableStateOf(true) }


    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    val topLevelRoutes = setOf("home_content", "chats_content", "settings_content")
    val isTopLevelDestination = currentRoute in topLevelRoutes

    Scaffold(
        containerColor = HopFogBackground,
        topBar = {
            // Show the main top bar for top-level screens
            if (isTopLevelDestination) {
                CenterAlignedTopAppBar(
                    title = { Text("HopFog", fontWeight = FontWeight.Bold, fontSize = 32.sp) },
                    actions = {
                        IconButton(onClick = { isOnline = !isOnline }) {
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
            } else { // Show a simple top bar with a Back button for sub-pages
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { innerNavController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HopFogBackground)
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                AppBottomNavigation(navController = innerNavController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = "home_content",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home_content") { HomePageContent() }
            composable("chats_content") { PlaceholderPage(pageName = "Chats") }
            composable("settings_content") { SettingsPage(navController = innerNavController) }


            composable("help") { HelpPage() }
            composable("terms_of_service") { TermsOfServicePage() }
            composable("privacy_policy") { PrivacyPolicyPage() }
        }
    }
}

@Composable
private fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = Color.Black) {
        // Home Item
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == "home_content" } == true,
            onClick = {
                navController.navigate("home_content") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        // Chats Item
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == "chats_content" } == true,
            onClick = {
                navController.navigate("chats_content") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
            label = { Text("Chats") }
        )
        // Settings Item
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == "settings_content" } == true,
            onClick = {
                navController.navigate("settings_content") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
fun PlaceholderPage(pageName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$pageName Page", fontSize = 32.sp, color = Color.White)
    }
}