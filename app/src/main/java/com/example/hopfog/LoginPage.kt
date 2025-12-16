package com.example.hopfog

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hopfog.ui.theme.HopFogBackground
import com.example.hopfog.ui.theme.HopFogBlue
import com.example.hopfog.ui.theme.HopFogTheme

@Composable
fun LoginPage(
    onLoginClicked: () -> Unit,
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

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
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = HopFogBackground,
                    unfocusedContainerColor = HopFogBackground,
                    focusedIndicatorColor = HopFogBlue,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = HopFogBlue,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedLeadingIconColor = HopFogBlue,
                    unfocusedLeadingIconColor = Color.Gray,
                )
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
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = HopFogBackground,
                    unfocusedContainerColor = HopFogBackground,
                    focusedIndicatorColor = HopFogBlue,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = HopFogBlue,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedLeadingIconColor = HopFogBlue,
                    unfocusedLeadingIconColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                TextButton(onClick = onForgotPasswordClicked) {
                    Text("Forgot Password?", color = HopFogBlue)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLoginClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HopFogBlue)
            ) {
                Text("Sign In", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))

            // "Don't have account? Sign Up" text
            val annotatedText = buildAnnotatedString {
                append("Don't have account? ")
                pushStringAnnotation(tag = "SignUp", annotation = "SignUp")
                withStyle(style = SpanStyle(color = HopFogBlue, fontWeight = FontWeight.Bold)) {
                    append("Sign Up")
                }
                pop()
            }

            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "SignUp", start = offset, end = offset)
                        .firstOrNull()?.let {
                            onSignUpClicked()
                        }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    HopFogTheme {
        LoginPage({}, {}, {})
    }
}