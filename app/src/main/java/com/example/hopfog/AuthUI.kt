package com.example.hopfog

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.hopfog.ui.theme.HopFogBlue

/**
 * A shared composable function that defines the consistent color scheme
 * for the text fields on the Login and Register pages.
 */

@Composable
fun authTextFieldColors(): TextFieldColors {
    // This is the style from your RegisterPage
    return TextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
        unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
        cursorColor = HopFogBlue,
        focusedIndicatorColor = HopFogBlue,
        unfocusedIndicatorColor = Color.Transparent,
        focusedLabelColor = Color.LightGray,
        unfocusedLabelColor = Color.LightGray,
        focusedLeadingIconColor = Color.White,
        unfocusedLeadingIconColor = Color.White
    )
}