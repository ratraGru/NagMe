package com.gru.nagme.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.domain.formatPrecise
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.service.NagNotifier
import com.gru.nagme.service.OverlayService
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * S9 + S10 — Pengaturan, Izin & Diagnostik.
 * Di Hari 4 baru bagian Izin & Diagnostik; pengaturan lain menyusul di Hari 6.
 */
@Composable
fun SettingsScreen(onOpenCharacter: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as NagMeApp
    val status = rememberPermissionStatus()

    var diag by remember { mutableStateOf(readDiagnostics(context, app)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        diag = readDiagnostics(context, app)
    }

    // Android 13+: dialog izin notifikasi. Kalau sudah ditolak permanen, arahkan ke pengaturan.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            val activity = context.findActivity()
            if (activity != null && !PermissionChecker.shouldShowNotificationRationale(activity)) {
                PermissionChecker.safeStart(context, PermissionChecker.notificationSettingsIntent(context))
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        CharacterEntryCard(onClick = onOpenCharacter)
        AppearanceCard()
        Text(
            text = stringResource(R.string.perm_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.perm_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        PermissionRow(
            titleRes = R.string.perm_overlay_title,
            descRes = R.string.perm_overlay_desc,
            granted = status.overlay,
            onEnable = {
                PermissionChecker.safeStart(context, PermissionChecker.overlayIntent(context))
            }
        )
        PermissionRow(
            titleRes = R.string.perm_alarm_title,
            descRes = R.string.perm_alarm_desc,
            granted = status.exactAlarm,
            onEnable = {
                PermissionChecker.safeStart(context, PermissionChecker.exactAlarmIntent(context))
            }
        )
        PermissionRow(
            titleRes = R.string.perm_notif_title,
            descRes = R.string.perm_notif_desc,
            granted = status.notifications,
            onEnable = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !PermissionChecker.notificationRuntimeGranted(context)
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Izin sudah ada tapi notifikasi dimatikan lewat pengaturan app.
                    PermissionChecker.safeStart(
                        context, PermissionChecker.notificationSettingsIntent(context)
                    )
                }
            }
        )
        PermissionRow(
            titleRes = R.string.perm_battery_title,
            descRes = R.string.perm_battery_desc,
            granted = status.batteryUnrestricted,
            onEnable = {
                PermissionChecker.safeStart(context, PermissionChecker.batteryIntent(context))
            }
        )

        val nextLine = if (diag.nextTrigger > 0L) {
            stringResource(R.string.diag_next, formatPrecise(diag.nextTrigger))
        } else {
            stringResource(R.string.diag_none)
        }
        val lastLine = if (diag.lastAlarm > 0L) {
            stringResource(R.string.diag_last_alarm, formatPrecise(diag.lastAlarm))
        } else {
            stringResource(R.string.diag_last_never)
        }
        val resultLine = if (diag.lastDue >= 0) {
            stringResource(R.string.diag_last_result, diag.lastDue, diag.lastPosted)
        } else {
            null
        }
        val channelLine = stringResource(
            if (diag.channelOk) R.string.diag_channel_ok else R.string.diag_channel_off
        )
        val scheduleLine = if (diag.scheduleInfo.isNotBlank()) {
            stringResource(R.string.diag_schedule, diag.scheduleInfo)
        } else {
            null
        }
        val detailLine = if (diag.lastDetail.isNotBlank()) {
            stringResource(R.string.diag_detail, diag.lastDetail)
        } else {
            null
        }
        InfoCard(
            title = stringResource(R.string.diag_title),
            body = listOfNotNull(
                nextLine, scheduleLine, lastLine, resultLine, channelLine, detailLine
            ).joinToString("\n")
        )
        Button(
            onClick = {
                val sent = NagNotifier.showTest(context)
                if (!sent) {
                    Toast.makeText(context, R.string.toast_notif_off, Toast.LENGTH_LONG).show()
                }
                diag = readDiagnostics(context, app)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_test_notif))
        }
        UserSettingsCard()
        DemoModeCard()
        CharacterTestCard(context)
        InfoCard(
            title = stringResource(R.string.tips_oem_title),
            body = stringResource(R.string.tips_oem_body)
        )
    }
}

private data class Diagnostics(
    val nextTrigger: Long,
    val lastAlarm: Long,
    val lastDue: Int,
    val lastPosted: Int,
    val channelOk: Boolean,
    val lastDetail: String,
    val scheduleInfo: String
)

private fun readDiagnostics(context: Context, app: NagMeApp): Diagnostics {
    val prefs = app.container.schedulerPrefs
    return Diagnostics(
        nextTrigger = prefs.nextTriggerEpoch,
        lastAlarm = prefs.lastAlarmEpoch,
        lastDue = prefs.lastDueCount,
        lastPosted = prefs.lastPostedCount,
        channelOk = NagNotifier.isChannelEnabled(context),
        lastDetail = prefs.lastAlarmDetail,
        scheduleInfo = prefs.lastScheduleInfo
    )
}

@Composable
private fun PermissionRow(
    titleRes: Int,
    descRes: Int,
    granted: Boolean,
    onEnable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (granted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.perm_granted),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.perm_missing),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onEnable) {
                        Text(stringResource(R.string.btn_enable))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Tombol uji karakter per level (versi awal Mode Demo, Bagian 5.7). Muncul 3 detik setelah ditekan. */
@Composable
private fun CharacterTestCard(context: Context) {
    val levels = listOf(
        NagLevel.CALM to R.string.char_test_l1,
        NagLevel.WORRIED to R.string.char_test_l2,
        NagLevel.STRESSED to R.string.char_test_l3,
        NagLevel.PANIC to R.string.char_test_l4,
        NagLevel.CRITICAL to R.string.char_test_l5,
        NagLevel.OVERDUE to R.string.char_test_late
    )
    val sample = stringResource(R.string.char_test_task)
    val lines = listOf(
        stringResource(R.string.notif_l1, "2h"),
        stringResource(R.string.notif_l2, "20h"),
        stringResource(R.string.notif_l3, "10h"),
        stringResource(R.string.notif_l4, "5h"),
        stringResource(R.string.notif_l5, "2h"),
        stringResource(R.string.notif_l6)
    )
    InfoCard(
        title = stringResource(R.string.char_test_title),
        body = stringResource(R.string.char_test_desc)
    )
    levels.chunked(3).forEachIndexed { row, chunk ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            chunk.forEachIndexed { i, (level, label) ->
                val line = lines[row * 3 + i]
                Button(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, R.string.toast_overlay_missing, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, R.string.toast_char_coming, Toast.LENGTH_SHORT).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                OverlayService.start(
                                    context.applicationContext, level, OverlayService.TEST_TASK_ID, sample, line,
                                    com.gru.nagme.domain.NagScheduling.thresholdMs(level)
                                )
                            }, 3_000L)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(label))
                }
            }
        }
    }
}
