package com.gru.nagme.ui

import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.data.SettingsPrefs
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.domain.NagScheduling
import com.gru.nagme.overlay.CharacterView
import com.gru.nagme.service.LineBank
import com.gru.nagme.service.OverlayService

private const val CELEBRATE_INDEX = 6

/**
 * Layar Karakter: pratinjau hidup (ukuran asli seperti di layar), galeri 3 karakter,
 * ukuran, ekspresi per level, dan suara. Semua perubahan langsung tersimpan.
 */
@Composable
fun CharacterSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s: SettingsPrefs = (context.applicationContext as NagMeApp).container.settings

    var style by remember { mutableIntStateOf(s.characterStyle) }
    var sizeDp by remember { mutableFloatStateOf(s.sizeDp.toFloat()) }
    var volume by remember { mutableFloatStateOf(s.volume.toFloat()) }
    var alarmStream by remember { mutableStateOf(s.useAlarmStream) }
    var pack by remember { mutableStateOf(s.voicePack) }
    var previewIndex by remember { mutableIntStateOf(0) }

    val levels = listOf(
        NagLevel.CALM, NagLevel.WORRIED, NagLevel.STRESSED,
        NagLevel.PANIC, NagLevel.CRITICAL, NagLevel.OVERDUE
    )
    val labels = listOf(
        R.string.char_test_l1, R.string.char_test_l2, R.string.char_test_l3,
        R.string.char_test_l4, R.string.char_test_l5, R.string.char_test_late,
        R.string.char_celebrate
    )
    val celebrateLine = remember { LineBank.celebrate() }
    val lines = listOf(
        stringResource(R.string.notif_l1, "2h"),
        stringResource(R.string.notif_l2, "20h"),
        stringResource(R.string.notif_l3, "10h"),
        stringResource(R.string.notif_l4, "5h"),
        stringResource(R.string.notif_l5, "2h"),
        stringResource(R.string.notif_l6),
        celebrateLine
    )
    val styleNames = listOf(R.string.char_blobby, R.string.char_cat, R.string.char_bunny)
    val sample = stringResource(R.string.char_test_task)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                }
                Text(
                    stringResource(R.string.char_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // --- Pratinjau (ukuran sama seperti overlay asli) ---
            SectionCard(stringResource(R.string.char_preview)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { CharacterView(it) },
                        update = {
                            it.style = style
                            it.celebrate = previewIndex == CELEBRATE_INDEX
                            it.level = levels[previewIndex.coerceAtMost(levels.lastIndex)]
                            it.line = lines[previewIndex]
                        },
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .height((CharacterView.BUBBLE_DP + sizeDp).dp)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    labels.forEachIndexed { i, label ->
                        FilterChip(
                            selected = previewIndex == i,
                            onClick = { previewIndex = i },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
                Text(stringResource(R.string.set_size, sizeDp.toInt()), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = sizeDp,
                    onValueChange = { sizeDp = it },
                    onValueChangeFinished = { s.sizeDp = sizeDp.toInt() },
                    valueRange = 120f..280f
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, R.string.toast_overlay_missing, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, R.string.toast_char_coming, Toast.LENGTH_SHORT).show()
                            val idx = previewIndex
                            Handler(Looper.getMainLooper()).postDelayed({
                                OverlayService.start(
                                    context.applicationContext,
                                    levels[idx.coerceAtMost(levels.lastIndex)],
                                    OverlayService.TEST_TASK_ID, sample, lines[idx],
                                    NagScheduling.thresholdMs(levels[idx.coerceAtMost(levels.lastIndex)]),
                                    celebrate = idx == CELEBRATE_INDEX
                                )
                            }, 3_000L)
                        }
                    }
                ) { Text(stringResource(R.string.btn_test_on_screen)) }
            }

            // --- Galeri karakter ---
            SectionCard(stringResource(R.string.char_pick)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    styleNames.forEachIndexed { i, name ->
                        val selected = style == i
                        Card(
                            onClick = { style = i; s.characterStyle = i },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AndroidView(
                                    factory = {
                                        CharacterView(it).apply {
                                            compact = true
                                            animated = false
                                            level = NagLevel.CALM
                                        }
                                    },
                                    update = { it.style = i },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                                Text(
                                    stringResource(name),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // --- Suara ---
            SectionCard(stringResource(R.string.char_voice)) {
                Text(stringResource(R.string.set_volume, volume.toInt()), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    onValueChangeFinished = { s.volume = volume.toInt() },
                    valueRange = 0f..100f
                )
                SwitchRow(stringResource(R.string.set_alarm_stream), alarmStream) {
                    alarmStream = it; s.useAlarmStream = it
                }
                Text(stringResource(R.string.set_pack), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pack == SettingsPrefs.PACK_CALM,
                        onClick = { pack = SettingsPrefs.PACK_CALM; s.voicePack = pack },
                        label = { Text(stringResource(R.string.pack_calm)) }
                    )
                    FilterChip(
                        selected = pack == SettingsPrefs.PACK_DRAMATIC,
                        onClick = { pack = SettingsPrefs.PACK_DRAMATIC; s.voicePack = pack },
                        label = { Text(stringResource(R.string.pack_dramatic)) }
                    )
                }
            }
            Box(Modifier.height(24.dp))
        }
    }
}
