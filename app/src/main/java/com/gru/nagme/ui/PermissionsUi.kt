package com.gru.nagme.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gru.nagme.R

data class PermissionStatus(
    val overlay: Boolean,
    val exactAlarm: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean
) {
    /** Tiga izin inti; baterai hanya disarankan. */
    val criticalMissing: Boolean get() = !overlay || !exactAlarm || !notifications
}

object PermissionChecker {
    fun check(context: Context): PermissionStatus {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        return PermissionStatus(
            overlay = Settings.canDrawOverlays(context),
            exactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms(),
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

    fun overlayIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context))

    fun exactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
        } else {
            appDetailsIntent(context)
        }

    fun notificationSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun batteryIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))

    fun appDetailsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    /** Buka layar pengaturan; kalau tidak tersedia di HP ini, jatuh ke detail aplikasi. */
    fun safeStart(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(appDetailsIntent(context))
            } catch (_: ActivityNotFoundException) {
                // Tidak ada yang bisa dibuka; biarkan saja (tidak crash).
            }
        }
    }

    /** True bila POST_NOTIFICATIONS (Android 13+) sudah diberikan, atau tidak diperlukan. */
    fun notificationRuntimeGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun shouldShowNotificationRationale(activity: Activity): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.POST_NOTIFICATIONS
            )
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Status izin yang diperbarui otomatis tiap app kembali ke layar depan (mis. dari Pengaturan sistem). */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PermissionChecker.check(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = PermissionChecker.check(context)
    }
    return status
}

/** Banner di Beranda bila izin inti belum lengkap (Bagian 5.8 / 6.6). Tanpa dialog, tidak mengganggu. */
@Composable
fun PermissionBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = stringResource(R.string.banner_permissions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}
