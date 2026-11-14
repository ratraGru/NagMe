package com.gru.nagme.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.data.SettingsPrefs
import com.gru.nagme.domain.NagLevel
import com.gru.nagme.domain.formatPrecise
import com.gru.nagme.service.AlarmProcessor
import kotlinx.coroutines.launch

private const val HOUR = 3_600_000L

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** D11 — pengaturan pengguna (Bagian 5.7). Semua tersimpan di SharedPreferences lewat [SettingsPrefs]. */
@Composable
fun UserSettingsCard() {
    val app = LocalContext.current.applicationContext as NagMeApp
    val s: SettingsPrefs = app.container.settings

    var master by remember { mutableStateOf(s.masterEnabled) }
    var alarmStream by remember { mutableStateOf(s.useAlarmStream) }
    var character by remember { mutableStateOf(s.characterStyle) }
    var pack by remember { mutableStateOf(s.voicePack) }
    var volume by remember { mutableFloatStateOf(s.volume.toFloat()) }
    var size by remember { mutableFloatStateOf(s.sizeDp.toFloat()) }
    val levels = listOf(
        NagLevel.CALM to R.string.char_test_l1,
        NagLevel.WORRIED to R.string.char_test_l2,
        NagLevel.STRESSED to R.string.char_test_l3,
        NagLevel.PANIC to R.string.char_test_l4,
        NagLevel.CRITICAL to R.string.char_test_l5,
        NagLevel.OVERDUE to R.string.char_test_late
    )
    val levelState = remember { levels.map { (l, _) -> mutableStateOf(s.isLevelEnabled(l)) } }

    SectionCard(stringResource(R.string.set_title)) {
        Text(stringResource(R.string.set_character), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.char_blobby, R.string.char_cat, R.string.char_bunny).forEachIndexed { i, name ->
                FilterChip(
                    selected = character == i,
                    onClick = { character = i; s.characterStyle = i },
                    label = { Text(stringResource(name)) }
                )
            }
        }
        SwitchRow(stringResource(R.string.set_master), master) { master = it; s.masterEnabled = it }
        levels.forEachIndexed { i, (level, label) ->
            SwitchRow(stringResource(label), levelState[i].value) {
                levelState[i].value = it
                s.setLevelEnabled(level, it)
            }
        }
        Text(stringResource(R.string.set_volume, volume.toInt()), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = volume, onValueChange = { volume = it },
            onValueChangeFinished = { s.volume = volume.toInt() },
            valueRange = 0f..100f
        )
        Text(stringResource(R.string.set_size, size.toInt()), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = size, onValueChange = { size = it },
            onValueChangeFinished = { s.sizeDp = size.toInt() },
            valueRange = 120f..280f
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
}

/** D15 — Mode Demo (Bagian 5.7): data dummy, majukan waktu, reset. */
@Composable
fun DemoModeCard() {
    val context = LocalContext.current
    val app = context.applicationContext as NagMeApp
    val container = app.container
    val scope = rememberCoroutineScope()
    var offset by remember { mutableLongStateOf(container.settings.simOffsetMs) }

    val titles = listOf(
        R.string.dummy_1, R.string.dummy_2, R.string.dummy_3,
        R.string.dummy_4, R.string.dummy_5, R.string.dummy_6
    ).map { stringResource(it) }
    // Satu tugas di tiap rentang level (L5, L4, L3, L2, L1, L0).
    val hoursAhead = listOf(2L, 5L, 10L, 20L, 50L, 100L)

    fun advance(ms: Long) {
        scope.launch {
            container.settings.simOffsetMs += ms
            offset = container.settings.simOffsetMs
            AlarmProcessor.runAndReschedule(app)
        }
    }

    SectionCard(stringResource(R.string.demo_title)) {
        Text(stringResource(R.string.demo_desc), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(
                R.string.demo_clock,
                if (offset == 0L) stringResource(R.string.demo_clock_real)
                else formatPrecise(container.clock.now())
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    val now = container.clock.now()
                    titles.forEachIndexed { i, t ->
                        container.taskRepository.add(t, null, now + hoursAhead[i] * HOUR, i, true)
                    }
                    Toast.makeText(context, R.string.demo_dummy_done, Toast.LENGTH_SHORT).show()
                }
            }
        ) { Text(stringResource(R.string.demo_dummy)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { advance(HOUR) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.demo_plus_1h))
            }
            OutlinedButton(onClick = { advance(6 * HOUR) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.demo_plus_6h))
            }
            OutlinedButton(onClick = { advance(24 * HOUR) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.demo_plus_1d))
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    container.settings.simOffsetMs = 0L
                    offset = 0L
                    container.taskRepository.deleteAll()
                    Toast.makeText(context, R.string.demo_reset_done, Toast.LENGTH_SHORT).show()
                }
            }
        ) { Text(stringResource(R.string.demo_reset)) }
    }
}
