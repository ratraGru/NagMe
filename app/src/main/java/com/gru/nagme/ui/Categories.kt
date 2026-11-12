package com.gru.nagme.ui

import androidx.compose.ui.graphics.Color
import com.gru.nagme.R

/** 6 warna preset kategori: biru, hijau, kuning, oranye, ungu, pink (Bagian 10.3). */
object Categories {
    val colors = listOf(
        Color(0xFF6FA8DC),
        Color(0xFF7CC9A0),
        Color(0xFFF2CC66),
        Color(0xFFF4A261),
        Color(0xFFB39DDB),
        Color(0xFFF2A6C0)
    )

    val labels = listOf(
        R.string.cat_class,
        R.string.cat_club,
        R.string.cat_personal,
        R.string.cat_project,
        R.string.cat_exam,
        R.string.cat_other
    )

    val indices: List<Int> get() = colors.indices.toList()
}
