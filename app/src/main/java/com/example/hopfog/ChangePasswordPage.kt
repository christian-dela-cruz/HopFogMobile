package com.example.hopfog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordPage(onPasswordChanged: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text("Change Your Password", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
//        Spacer(modifier = Modifier.height(32.dp))

        PasswordTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = "Old Password",
            isVisible = oldPasswordVisible,
            onVisibilityChange = { oldPasswordVisible = !oldPasswordVisible }
        )
        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "New Password",
            isVisible = newPasswordVisible,
            onVisibilityChange = { newPasswordVisible = !newPasswordVisible }
        )
        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm New Password",
            isVisible = newPasswordVisible, // Reuse visibility for simplicity
            onVisibilityChange = { newPasswordVisible = !newPasswordVisible }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (newPassword != confirmPassword) {
                    context.toast("New passwords do not match.")
                    return@Button
                }
                if (newPassword.length < 3) {
                    context.toast("Password must be at least 3 characters.")
                    return@Button
                }

                isLoading = true
                coroutineScope.launch {
                    val success = NetworkManager.changePassword(context, oldPassword, newPassword)
                    if (success) {
                        context.toast("Password changed successfully!")
                        onPasswordChanged() // Navigate back
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Update Password")
            }
        }
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            IconButton(onClick = onVisibilityChange) {
                Icon(imageVector = image, contentDescription = "Toggle password visibility")
            }
        }
    )
}