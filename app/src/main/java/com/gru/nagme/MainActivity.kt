package com.gru.nagme

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gru.nagme.service.KeepAliveService
import com.gru.nagme.ui.NagMeRoot
import com.gru.nagme.ui.NagMeTheme
import com.gru.nagme.ui.ThemeController
import androidx.compose.runtime.DisposableEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeepAliveService.sync(this)
        ThemeController.load((application as NagMeApp).container.settings)
        setContent {
            val dark = ThemeController.dark
            // Ikon status bar & navigasi menyesuaikan tema.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
                onDispose { }
            }
            NagMeTheme {
                NagMeRoot()
            }
        }
    }
}
