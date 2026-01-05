package com.example.hopfog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HopFogTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "landing") {
                    composable("landing") {
                        LandingPage(onGetStartedClicked = { navController.navigate("login") })
                    }
                    composable("login") {
                        LoginPage(
                            onLoginClicked = {
                                // Navigate to the main app container
                                navController.navigate("app_main") {
                                    popUpTo("landing") { inclusive = true }
                                }
                            },
                            onSignUpClicked = { navController.navigate("register") },
                            onForgotPasswordClicked = { /* TODO */ }
                        )
                    }
                    composable("register") {
                        RegisterPage(
                            onSignUpClicked = {
                                // Also navigate to the main app container
                                navController.navigate("app_main") {
                                    popUpTo("landing") { inclusive = true }
                                }
                            },
                            onBackClicked = { navController.popBackStack() }
                        )
                    }
                    // This is the new route for the entire logged-in experience
                    composable("app_main") {
                        AppMainPage()
                    }
                }
            }
        }
    }
}

// LandingPage remains the same
@Composable
fun LandingPage(onGetStartedClicked: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = HopFogBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.hopfog_logo),
                    contentDescription = "HopFog Logo",
                    modifier = Modifier.size(160.dp).offset(x = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "HopFog",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(2f))
            Button(
                onClick = onGetStartedClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HopFogBlue)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}