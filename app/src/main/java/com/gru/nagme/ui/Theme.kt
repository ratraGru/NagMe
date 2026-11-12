package com.gru.nagme.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet pastel (mint, krem, peach) sesuai Master Context Bagian 10.3
private val NagMeColors = lightColorScheme(
    primary = Color(0xFF2F7D6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFEBDD),
    onPrimaryContainer = Color(0xFF083D31),
    secondary = Color(0xFFB5603F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC8),
    onSecondaryContainer = Color(0xFF4A2412),
    background = Color(0xFFFFF8EC),
    onBackground = Color(0xFF2B2B2B),
    surface = Color(0xFFFFF8EC),
    onSurface = Color(0xFF2B2B2B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF3E3),
    surfaceContainer = Color(0xFFFFEFDD),
    surfaceContainerHigh = Color(0xFFFFEAD6),
    surfaceContainerHighest = Color(0xFFFFE5CF)
)

@Composable
fun NagMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NagMeColors,
        content = content
    )
}
