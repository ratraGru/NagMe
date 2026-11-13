package com.gru.nagme.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Waktu lengkap sampai milidetik (zona waktu HP), untuk diagnosis alarm. */
fun formatPrecise(epochMs: Long): String =
    SimpleDateFormat("dd/MM HH:mm:ss.SSS", Locale.US).format(Date(epochMs))
