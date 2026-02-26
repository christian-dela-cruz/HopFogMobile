package com.example.hopfog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import android.widget.Toast
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext //
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun LoginPage(
    userViewModel: UserViewModel = viewModel(),
    onLoginClicked: () -> Unit,
    onSignUpClicked: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("HopFog_RememberMe", Context.MODE_PRIVATE)

    var email by remember { mutableStateOf(prefs.getString("saved_username", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(prefs.getBoolean("remember_me", false)) }

    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = HopFogBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HopFog",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 40.dp)

            )
            Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
            Text(
                text = "Sign In",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Email or Username field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email or Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp), // Added rounded corners
                colors = authTextFieldColors() // Using the new helper for colors
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp), // Added rounded corners
                colors = authTextFieldColors() // Using the new helper for colors
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = HopFogBlue,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.Black
                        )
                    )
                    Text("Remember Me", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please enter username and password.", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch {
                            val resultJson = NetworkManager.loginUser(context, email, password)
                            if (resultJson != null) {
                                val editor = prefs.edit()
                                if (rememberMe) {
                                    editor.putString("saved_username", email)
                                    editor.putString("saved_password", password)
                                    editor.putBoolean("remember_me", true)
                                } else {
                                    editor.clear()
                                }
                                editor.apply()
                                userViewModel.onLoginSuccess(resultJson)
                                onLoginClicked()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HopFogBlue)
            ) {
                Text("Sign In", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Don't have an account? Ask your community admin to create one for you.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    HopFogTheme {
        LoginPage(
            onLoginClicked = {},
            onSignUpClicked = {})
    }
}