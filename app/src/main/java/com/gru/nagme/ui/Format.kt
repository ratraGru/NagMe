package com.gru.nagme.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gru.nagme.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatDeadline(epochMs: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

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
