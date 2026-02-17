package com.example.hopfog

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTheme
import kotlinx.coroutines.launch

private object RememberMeManager {
    private const val PREFS_NAME = "LoginPrefs"
    private const val PREF_USERNAME = "username"
    private const val PREF_PASSWORD = "password"
    private const val PREF_REMEMBER_ME = "remember_me"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveCredentials(context: Context, username: String, password: String, remember: Boolean) {
        val editor = getPrefs(context).edit()
        if (remember) {
            editor.putString(PREF_USERNAME, username)
            editor.putString(PREF_PASSWORD, password)
            editor.putBoolean(PREF_REMEMBER_ME, true)
        } else {
            // Clear all saved credentials if remember is false
            editor.clear()
        }
        editor.apply()
    }

    fun loadCredentials(context: Context): Triple<String, String, Boolean> {
        val prefs = getPrefs(context)
        val username = prefs.getString(PREF_USERNAME, "") ?: ""
        val password = prefs.getString(PREF_PASSWORD, "") ?: ""
        val rememberMe = prefs.getBoolean(PREF_REMEMBER_ME, false)
        return Triple(username, password, rememberMe)
    }
}


@Composable
fun LoginPage(
    userViewModel: UserViewModel = viewModel(),
    onLoginClicked: () -> Unit,
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load saved credentials when the page is first displayed
    LaunchedEffect(Unit) {
        val (savedUser, savedPass, shouldRemember) = RememberMeManager.loadCredentials(context)
        email = savedUser
        password = savedPass
        rememberMe = shouldRemember
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val permissionsGranted = permissions.values.all { it }
        if (permissionsGranted) {
            Toast.makeText(context, "Permissions granted. Please try signing in again.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Bluetooth permissions are required for off-grid login.", Toast.LENGTH_LONG).show()
        }
    }

    // This initialization needs to happen for the permission check to work
    LaunchedEffect(Unit) {
        BleManager.initialize(context)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = HopFogBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "HopFog", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 40.dp))
            HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
            Text(text = "Sign In", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email or Username") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), colors = authTextFieldColors())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), colors = authTextFieldColors())
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, colors = CheckboxDefaults.colors(checkedColor = HopFogBlue, uncheckedColor = Color.Gray, checkmarkColor = Color.Black))
                    Text("Remember Me", color = Color.White)
                }
                TextButton(onClick = onForgotPasswordClicked) { Text("Forgot Password?", color = HopFogBlue) }
            }
            Spacer(modifier = Modifier.height(32.dp))

// In LoginPage.kt

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please enter username and password.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!BleManager.hasPermissions(context)) {
                        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        permissionLauncher.launch(permissionsToRequest)
                        return@Button
                    }

                    isLoading = true
                    RememberMeManager.saveCredentials(context, email, password, rememberMe)

                    // Create the JSON string for the login command
                    val loginData = mapOf("action" to "login", "username" to email, "password" to password)
                    val jsonString = com.google.gson.Gson().toJson(loginData)

                    scope.launch {
                        // Call the function that performs the real BLE validation
                        val (success, response) = NetworkManager.validateLoginWithBle(context, jsonString)
                        isLoading = false

                        if (success) {
                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                            // Create a MOCK user object for the ViewModel
                            val mockUserJson = org.json.JSONObject().apply {
                                put("user", org.json.JSONObject().apply {
                                    put("user_id", 999) // Mock ID
                                    put("username", email)
                                    put("email", "") // Safely empty
                                })
                            }
                            userViewModel.onLoginSuccess(mockUserJson)
                            onLoginClicked() // Navigate to AppMainPage
                        } else {
                            Toast.makeText(context, "Login Failed: $response", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HopFogBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("Sign In", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    HopFogTheme {
        LoginPage(
            onLoginClicked = {},
            onSignUpClicked = {},
            onForgotPasswordClicked = {}
        )
    }
}