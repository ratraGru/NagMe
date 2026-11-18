package com.gru.nagme.data

import android.content.Context
import com.gru.nagme.domain.NagLevel

/** Pengaturan pengguna (SharedPreferences, tanpa tabel — Bagian 8) + offset jam simulasi Mode Demo. */
class SettingsPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nagme_settings", Context.MODE_PRIVATE)

    /** Saklar utama: mati = tidak ada karakter / notifikasi pengingat sama sekali. */
    var masterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER, true)
        set(v) { prefs.edit().putBoolean(KEY_MASTER, v).apply() }

    fun isLevelEnabled(level: NagLevel): Boolean = prefs.getBoolean("lvl_${level.value}", true)
    fun setLevelEnabled(level: NagLevel, enabled: Boolean) {
        prefs.edit().putBoolean("lvl_${level.value}", enabled).apply()
    }

    /** 0..100 */
    var volume: Int
        get() = prefs.getInt(KEY_VOLUME, 80)
        set(v) { prefs.edit().putInt(KEY_VOLUME, v.coerceIn(0, 100)).apply() }

    /** Tinggi karakter, 120..280 dp. */
    var sizeDp: Int
        get() = prefs.getInt(KEY_SIZE, 180)
        set(v) { prefs.edit().putInt(KEY_SIZE, v.coerceIn(120, 280)).apply() }

    /** Suara lewat jalur alarm (bunyi walau mode senyap). */
    var useAlarmStream: Boolean
        get() = prefs.getBoolean(KEY_ALARM_STREAM, true)
        set(v) { prefs.edit().putBoolean(KEY_ALARM_STREAM, v).apply() }

    /** "calm" (Santai) atau "dramatic" (Dramatis). */
    var voicePack: String
        get() = prefs.getString(KEY_PACK, PACK_CALM) ?: PACK_CALM
        set(v) { prefs.edit().putString(KEY_PACK, v).apply() }

    /** Selisih jam simulasi (Mode Demo). 0 = waktu asli. */
    var simOffsetMs: Long
        get() = prefs.getLong(KEY_SIM_OFFSET, 0L)
        set(v) { prefs.edit().putLong(KEY_SIM_OFFSET, v).apply() }

    /** Karakter aktif: 0 Blobby, 1 Kucing, 2 Kelinci (Galeri Karakter, Bagian 5.6). */
    var characterStyle: Int
        get() = prefs.getInt(KEY_CHARACTER, 0).coerceIn(0, 2)
        set(v) { prefs.edit().putInt(KEY_CHARACTER, v.coerceIn(0, 2)).apply() }

    /** Penjaga latar (foreground service ringan) supaya pengingat tetap hidup setelah app ditutup. */
    var keepAliveEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ALIVE, true)
        set(v) { prefs.edit().putBoolean(KEY_KEEP_ALIVE, v).apply() }

    /** True setelah onboarding selesai atau dilewati. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(v) { prefs.edit().putBoolean(KEY_ONBOARDING, v).apply() }

    companion object {
        private const val KEY_KEEP_ALIVE = "keep_alive"
        private const val KEY_CHARACTER = "character_style"
        private const val KEY_ONBOARDING = "onboarding_done"

        const val PACK_CALM = "calm"
        const val PACK_DRAMATIC = "dramatic"
        private const val KEY_MASTER = "master"
        private const val KEY_VOLUME = "volume"
        private const val KEY_SIZE = "size_dp"
        private const val KEY_ALARM_STREAM = "alarm_stream"
        private const val KEY_PACK = "voice_pack"
        private const val KEY_SIM_OFFSET = "sim_offset_ms"
    }
}
