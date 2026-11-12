package com.gru.nagme.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet pastel (mint, krem, peach) sesuai Master Context Bagian 10.3
private val NagMeColors = lightColorScheme(
    primary = Color(0xFF2F7D6B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB5603F),
    background = Color(0xFFFFF8EC),
    onBackground = Color(0xFF2B2B2B),
    surface = Color(0xFFFFF8EC),
    onSurface = Color(0xFF2B2B2B)
)

@Composable
fun NagMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NagMeColors,
        content = content
    )
}
