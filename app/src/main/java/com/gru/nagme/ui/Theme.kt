package com.gru.nagme.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gru.nagme.data.SettingsPrefs

/** Pilihan tema pengguna (gelap / terang). Berubah langsung tanpa restart. */
object ThemeController {
    var dark by mutableStateOf(true)
        private set

    fun load(prefs: SettingsPrefs) { dark = prefs.darkTheme }

    fun set(prefs: SettingsPrefs, value: Boolean) {
        prefs.darkTheme = value
        dark = value
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1B0F4A),
    primaryContainer = Color(0xFF3B2F8F),
    onPrimaryContainer = Color(0xFFE4DCFF),
    secondary = Color(0xFFFF8FC0),
    onSecondary = Color(0xFF4A0A2C),
    secondaryContainer = Color(0xFF6B2A55),
    onSecondaryContainer = Color(0xFFFFD9EA),
    background = Color(0xFF14122B),
    onBackground = Color(0xFFEDEAFF),
    surface = Color(0xFF14122B),
    onSurface = Color(0xFFEDEAFF),
    onSurfaceVariant = Color(0xFFB5B0D9),
    outline = Color(0xFF8C87B8),
    outlineVariant = Color(0xFF3A3670),
    surfaceContainerLowest = Color(0xFF1C1940),
    surfaceContainerLow = Color(0xFF1C1940),
    surfaceContainer = Color(0xFF221F4D),
    surfaceContainerHigh = Color(0xFF2A2660),
    surfaceContainerHighest = Color(0xFF322E70)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4EE6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DCFF),
    onPrimaryContainer = Color(0xFF25126B),
    secondary = Color(0xFFD6428A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9EA),
    onSecondaryContainer = Color(0xFF52113A),
    background = Color(0xFFF6F3FF),
    onBackground = Color(0xFF1E1B3A),
    surface = Color(0xFFF6F3FF),
    onSurface = Color(0xFF1E1B3A),
    onSurfaceVariant = Color(0xFF5E5A82),
    outline = Color(0xFF8D88B5),
    outlineVariant = Color(0xFFDCD6F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF8FF),
    surfaceContainer = Color(0xFFF1EDFF),
    surfaceContainerHigh = Color(0xFFEAE5FF),
    surfaceContainerHighest = Color(0xFFE3DDFF)
)

private val NagShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp)
)

/** Gradasi ungu ke pink untuk tombol utama dan aksen. */
val AccentBrush: Brush = Brush.linearGradient(listOf(Color(0xFF8E7CFF), Color(0xFFFF6FB5)))

@Composable
fun NagMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (ThemeController.dark) DarkColors else LightColors,
        shapes = NagShapes,
        content = content
    )
}
