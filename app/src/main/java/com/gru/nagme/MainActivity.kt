package com.gru.nagme

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.gru.nagme.ui.NagMeRoot
import com.gru.nagme.ui.NagMeTheme
import com.gru.nagme.widget.TaskWidgetProvider

class MainActivity : ComponentActivity() {

    /** Permintaan dari widget: null = tidak ada, -1 = tambah tugas, >= 0 = edit tugas itu. */
    private val pendingOpen = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ikon status bar & navigasi gelap, karena latar app krem (terang).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        // Setelah rotasi/recreate jangan membuka layar widget lagi.
        if (savedInstanceState == null) readWidgetIntent(intent)
        setContent {
            NagMeTheme {
                NagMeRoot(
                    pendingOpen = pendingOpen.value,
                    onConsumed = { pendingOpen.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readWidgetIntent(intent)
    }

    private fun readWidgetIntent(intent: Intent?) {
        intent ?: return
        val id = when {
            intent.getBooleanExtra(TaskWidgetProvider.EXTRA_ADD, false) -> -1L
            intent.hasExtra(TaskWidgetProvider.EXTRA_TASK_ID) ->
                intent.getLongExtra(TaskWidgetProvider.EXTRA_TASK_ID, -2L).takeIf { it >= 0L }
            else -> null
        }
        intent.removeExtra(TaskWidgetProvider.EXTRA_ADD)
        intent.removeExtra(TaskWidgetProvider.EXTRA_TASK_ID)
        if (id != null) pendingOpen.value = id
    }
}
