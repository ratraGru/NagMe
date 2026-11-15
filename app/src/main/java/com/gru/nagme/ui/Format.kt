package com.gru.nagme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.gru.nagme.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Membuat formatter itu mahal; simpan satu per Locale supaya tidak dibuat ulang untuk tiap kartu.
@Volatile private var deadlineFormatterCache: Pair<Locale, DateTimeFormatter>? = null

private fun deadlineFormatter(): DateTimeFormatter {
    val locale = Locale.getDefault()
    deadlineFormatterCache?.let { if (it.first == locale) return it.second }
    val f = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
    deadlineFormatterCache = locale to f
    return f
}

fun formatDeadline(epochMs: Long): String =
    deadlineFormatter().format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

/**
 * "Sekarang" yang dibulatkan ke menit dan diperbarui tiap 30 detik. Dengan begitu kartu tidak
 * dianggap berubah (dan digambar ulang) di setiap recompose, hanya saat menitnya berganti.
 */
@Composable
fun rememberNow(clock: () -> Long): Long {
    var now by remember { mutableLongStateOf(clock() / 60_000L * 60_000L) }
    LaunchedEffect(Unit) {
        while (true) {
            now = clock() / 60_000L * 60_000L
            delay(30_000L)
        }
    }
    return now
}

fun formatDate(date: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(date)

fun formatTime(time: LocalTime): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(time)

fun isSameDay(epochMs: Long, otherEpochMs: Long): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate() ==
        Instant.ofEpochMilli(otherEpochMs).atZone(zone).toLocalDate()
}

/** Teks sisa waktu. Sengaja netral (tanpa kata "telat"/"gagal") — prinsip P3. */
@Composable
fun remainingText(deadlineEpoch: Long, now: Long): String {
    val ms = deadlineEpoch - now
    if (ms <= 0L) return stringResource(R.string.remaining_passed)
    val totalMinutes = ms / 60_000L
    val days = (totalMinutes / 1_440L).toInt()
    val hours = ((totalMinutes % 1_440L) / 60L).toInt()
    val minutes = (totalMinutes % 60L).toInt()
    val value = when {
        days > 0 -> stringResource(R.string.remaining_dh, days, hours)
        hours > 0 -> stringResource(R.string.remaining_hm, hours, minutes)
        else -> stringResource(R.string.remaining_m, minutes)
    }
    return stringResource(R.string.time_left, value)
}
