package com.gru.nagme.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val LAST_PAGE = 3

/**
 * S1 — Onboarding 4 halaman (Bagian 5.8): sambutan, lalu 1 izin per halaman.
 * Semua halaman bisa dilewati; izin yang ditolak cukup ditandai banner di Beranda, tanpa spam.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val status = rememberPermissionStatus()
    var page by remember { mutableIntStateOf(0) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* status diperbarui otomatis saat kembali ke layar ini */ }

    val title: String
    val body: String
    var granted: Boolean? = null
    var enable: (() -> Unit)? = null
    when (page) {
        0 -> {
            title = stringResource(com.gru.nagme.R.string.ob_welcome_title)
            body = stringResource(com.gru.nagme.R.string.ob_welcome_body)
        }
        1 -> {
            title = stringResource(com.gru.nagme.R.string.perm_overlay_title)
            body = stringResource(com.gru.nagme.R.string.perm_overlay_desc)
            granted = status.overlay
            enable = { PermissionChecker.safeStart(context, PermissionChecker.overlayIntent(context)) }
        }
        2 -> {
            title = stringResource(com.gru.nagme.R.string.perm_alarm_title)
            body = stringResource(com.gru.nagme.R.string.perm_alarm_desc)
            granted = status.exactAlarm
            enable = { PermissionChecker.safeStart(context, PermissionChecker.exactAlarmIntent(context)) }
        }
        else -> {
            title = stringResource(com.gru.nagme.R.string.perm_notif_title)
            body = stringResource(com.gru.nagme.R.string.perm_notif_desc)
            granted = status.notifications
            enable = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionChecker.safeStart(context, PermissionChecker.notificationSettingsIntent(context))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(com.gru.nagme.R.string.ob_skip)) }
        }
        Spacer(Modifier.weight(1f))
        BobbingFace(160)
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (granted != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(
                    if (granted) com.gru.nagme.R.string.perm_granted else com.gru.nagme.R.string.perm_missing
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(com.gru.nagme.R.string.ob_step, page + 1, LAST_PAGE + 1),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        if (enable != null && granted == false) {
            Button(onClick = { enable() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(com.gru.nagme.R.string.btn_enable))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { if (page == LAST_PAGE) onFinish() else page++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (page == LAST_PAGE) com.gru.nagme.R.string.ob_done else com.gru.nagme.R.string.ob_next))
            }
        } else {
            Button(
                onClick = { if (page == LAST_PAGE) onFinish() else page++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (page == LAST_PAGE) com.gru.nagme.R.string.ob_done else com.gru.nagme.R.string.ob_next))
            }
        }
    }
}
