package com.gru.nagme.service

import com.gru.nagme.domain.NagLevel
import kotlin.random.Random

/** Bank skrip suara — Master Context Bagian 21. {sisa} dan {judul} diisi otomatis. */
object LineBank {
    private val l1 = listOf(
        "Kamu ada tugas {judul} jangan lupa, tapi masih {sisa} sih jadi santai",
        "Ada Tugas {judul} jangan lupa, skrg masih {sisa} santai dulu aja"
    )
    private val l2 = listOf(
        "Tugas {judul} sisa {sisa} lagi, mulai kerjain sekarang!",
        "Tugas {judul} belum selesai oyy, sisa {sisa}"
    )
    private val l3 = listOf(
        "Kamu ada tugas {judul} sisa {sisa} udah mepet banget!",
        "Tugas {judul} udah mepet, selesain sekarang!"
    )
    private val l4 = listOf(
        "Woiii tugasmu yg {judul} kerjain sekarang!!",
        "Tugas Kerjain itu WOII!!"
    )
    private val l5 = listOf(
        "KERJAIN SEKARANG JUGA !!!!",
        "WOIIII KAMU ADA TUGASS LOHHH!!!"
    )
    private val l6 = listOf(
        "Telatt Kannn",
        "Cape udah lahh"
    )
    private val done = listOf(
        "Nahh gitu dongg",
        "Nahhhh Mantap"
    )

    fun pick(level: NagLevel, remainingMs: Long, title: String): String {
        val pool = when (level) {
            NagLevel.DORMANT, NagLevel.CALM -> l1
            NagLevel.WORRIED -> l2
            NagLevel.STRESSED -> l3
            NagLevel.PANIC -> l4
            NagLevel.CRITICAL -> l5
            NagLevel.OVERDUE -> l6
        }
        return fill(pool[Random.nextInt(pool.size)], remainingMs, title)
    }

    fun celebrate(): String = done[Random.nextInt(done.size)]

    /** Dialog snooze berubah sesuai hitungan — Bagian 6.4. */
    fun snoozeLine(count: Int): String = when {
        count <= 1 -> "Ya udah 10 menit deh."
        count == 2 -> "Hmm, oke 10 menit lagi"
        count == 3 -> "Ahh udah lahh"
        else -> "Terserahh udah"
    }

    private fun fill(template: String, remainingMs: Long, title: String): String =
        template.replace("{sisa}", speakable(remainingMs)).replace("{judul}", title)

    /** Sisa waktu dalam kata-kata Indonesia (dibaca TTS), mis. "2 jam 5 menit". */
    fun speakable(remainingMs: Long): String {
        val totalMin = remainingMs.coerceAtLeast(0L) / 60_000L
        val d = totalMin / 1_440L
        val h = (totalMin % 1_440L) / 60L
        val m = totalMin % 60L
        return when {
            d > 0 -> if (h > 0) "$d hari $h jam" else "$d hari"
            h > 0 -> if (m > 0) "$h jam $m menit" else "$h jam"
            else -> "${maxOf(m, 1L)} menit"
        }
    }
}
