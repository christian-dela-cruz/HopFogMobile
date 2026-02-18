package com.example.hopfog

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTheme
// --- THESE ARE THE FIXES FOR THE UNRESOLVED REFERENCE ---
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LoginPage(
    userViewModel: UserViewModel = viewModel(),
    onLoginClicked: () -> Unit,
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit
) {
    val context = LocalContext.current
    val connectionStatus by BleManager.status.collectAsState()

    LaunchedEffect(Unit) {
        BleManager.initialize(context)
    }

    StatelessLoginPage(
        connectionStatus = connectionStatus,
        onLogin = { email, password ->
            val loginData = mapOf("action" to "login", "username" to email, "password" to password)
            val jsonString = com.google.gson.Gson().toJson(loginData)

            BleManager.performLoginTransaction(jsonString, object : BleTransactionCallback {
                override fun onTransactionSuccess(response: String) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                        SessionManager.saveSession(context, userId = 999, username = email, hasAgreedSos = false)
                        val mockUserJson = org.json.JSONObject().apply {
                            put("user", org.json.JSONObject().apply {
                                put("user_id", 999)
                                put("username", email)
                                put("email", "")
                            })
                        }
                        userViewModel.onLoginSuccess(mockUserJson)
                        onLoginClicked()
                    }
                }

                override fun onTransactionFailure(error: String) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Login Failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            })
        },
        onSignUpClicked = onSignUpClicked,
        onForgotPasswordClicked = onForgotPasswordClicked
    )
}

@Composable
private fun StatelessLoginPage(
    connectionStatus: ConnectionStatus,
    onLogin: (String, String) -> Unit,
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = connectionStatus is ConnectionStatus.Scanning || connectionStatus is ConnectionStatus.Connecting
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "Permissions granted. Please try again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Bluetooth permissions are required.", Toast.LENGTH_LONG).show()
        }
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onForgotPasswordClicked) { Text("Forgot Password?", color = HopFogBlue) }
            }
            Spacer(modifier = Modifier.height(32.dp))

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
                    onLogin(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HopFogBlue)
            ) {
                if (isLoading) {
                    val loadingText = when (connectionStatus) {
                        is ConnectionStatus.Scanning -> "Scanning..."
                        is ConnectionStatus.Connecting -> "Connecting..."
                        else -> "Loading..."
                    }
                    Text(loadingText, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        StatelessLoginPage(
            connectionStatus = ConnectionStatus.Disconnected,
            onLogin = { _, _ -> },
            onSignUpClicked = {},
            onForgotPasswordClicked = {}
        )
    }
}