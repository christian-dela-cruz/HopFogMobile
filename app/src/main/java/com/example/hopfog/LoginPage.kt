package com.example.hopfog

import android.Manifest
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

    // THE FIX: Get a coroutine scope tied to the Composable's lifecycle.
    val scope = rememberCoroutineScope()

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
            Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
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

                    // THE FIX: Use the scope to launch the coroutine.
                    scope.launch {
                        // We call the NetworkManager. It pretends to log in and saves the session.
                        val response = NetworkManager.loginUser(context, email, password)
                        isLoading = false

                        // The response is now a mock success from NetworkManager.
                        // We trust it and navigate immediately.
                        if (response != null && response.getBoolean("success")) {
                            Toast.makeText(context, "Login details saved.", Toast.LENGTH_SHORT).show()
                            userViewModel.onLoginSuccess(response)
                            onLoginClicked() // Navigate to AppMainPage
                        } else {
                            Toast.makeText(context, "Login Failed", Toast.LENGTH_LONG).show()
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
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.LightGray)) { append("Don't have account? ") }
                pushStringAnnotation(tag = "SignUp", annotation = "SignUp")
                withStyle(style = SpanStyle(color = HopFogBlue, fontWeight = FontWeight.Bold)) { append("Sign Up") }
                pop()
            }
            ClickableText(text = annotatedText, onClick = { offset -> annotatedText.getStringAnnotations(tag = "SignUp", start = offset, end = offset).firstOrNull()?.let { onSignUpClicked() } })
        }
    }
}

//@Composable
//fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
//    focusedBorderColor = HopFogBlue,
//    unfocusedBorderColor = Color.Gray,
//    focusedLabelColor = HopFogBlue,
//    unfocusedLabelColor = Color.Gray,
//    focusedTextColor = Color.White,
//    unfocusedTextColor = Color.White,
//    cursorColor = HopFogBlue,
//    focusedLeadingIconColor = HopFogBlue,
//    unfocusedLeadingIconColor = Color.Gray
//)


@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    HopFogTheme {
        LoginPage(
            onLoginClicked = {},
            onSignUpClicked = {},
            onForgotPasswordClicked = {})
    }
}