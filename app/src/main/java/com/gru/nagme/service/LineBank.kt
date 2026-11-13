package com.gru.nagme.service

import com.gru.nagme.domain.NagLevel
import kotlin.random.Random

/** Bank skrip suara — Master Context Bagian 21. {sisa} dan {judul} diisi otomatis. */
object LineBank {
    private val l1 = listOf(
        "Santai, masih ada {sisa}. Tapi jangan lupa {judul} ya.",
        "Halo, cuma mampir ngingetin aja."
    )
    private val l2 = listOf(
        "{judul} tinggal {sisa} lagi lho. Udah mulai belum?",
        "Hmm, kayaknya lu harus mulai mikir nih."
    )
    private val l3 = listOf(
        "Ayo mulai dong, tinggal {sisa}!",
        "Ini udah mepet lho, serius."
    )
    private val l4 = listOf(
        "Woi. Tinggal {sisa}. Gerak.",
        "Buka laptop sekarang! Sekarang!"
    )
    private val l5 = listOf(
        "Tinggal {sisa}! Gw panik. Lu harus panik juga!",
        "Cepet! Cepet!"
    )
    private val l6 = listOf(
        "Ya udah, telat. Terserah lu deh.",
        "Gw udah bilang apa."
    )
    private val done = listOf(
        "Yeay! Selesai! Gw bangga sama lu.",
        "Nah gitu dong. Lanjut besok ya."
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
        count <= 1 -> "Ya udah, 10 menit lagi ya."
        count == 2 -> "Sip, tapi ini beneran 10 menit ya."
        count == 3 -> "Ya udah, terserah."
        else -> "Udah capek ngingetin."
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
