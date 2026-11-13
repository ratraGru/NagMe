package com.gru.nagme.service

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.gru.nagme.data.SettingsPrefs
import com.gru.nagme.domain.NagLevel
import java.util.Locale

/**
 * Suara karakter lewat Android TextToSpeech (Bagian 5.5). Dipakai dari main thread.
 * Cadangan: TTS Indonesia tidak ada -> bahasa default; TTS gagal total -> nada notifikasi bawaan.
 */
class VoiceManager(context: Context, private val settings: SettingsPrefs) {
    private class Request(val text: String, val level: NagLevel, val onDone: () -> Unit)

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var failed = false
    private var pending: Request? = null
    private var current: (() -> Unit)? = null
    private var counter = 0

    init {
        try {
            tts = TextToSpeech(appContext) { status -> handler.post { onInit(status) } }
        } catch (t: Throwable) {
            Log.e(TAG, "TTS tidak bisa dibuat", t)
            failed = true
        }
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            failed = true
        } else {
            var r = engine.setLanguage(Locale("id", "ID"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                r = engine.setLanguage(Locale.getDefault())
            }
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) failed = true
            else {
                ready = true
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { handler.post { finish() } }
                    @Deprecated("Deprecated in Java")
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) { handler.post { fallbackSound() } }
                })
            }
        }
        val p = pending
        pending = null
        if (p != null) run(p)
    }

    /** Ucapkan [text]; [onDone] dipanggil (main thread) setelah selesai / gagal / dimatikan. */
    fun speak(text: String, level: NagLevel, onDone: () -> Unit) {
        if (settings.volume == 0) { onDone(); return }
        val req = Request(text, level, onDone)
        when {
            failed -> { current = onDone; fallbackSound() }
            !ready -> pending = req
            else -> run(req)
        }
    }

    private fun run(req: Request) {
        val engine = tts
        if (failed || engine == null || !ready) {
            current = req.onDone
            fallbackSound()
            return
        }
        current = req.onDone
        try {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (settings.useAlarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val (pitch, rate) = pitchAndRate(req.level)
            engine.setPitch(pitch)
            engine.setSpeechRate(rate)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeFor(req.level))
            }
            val r = engine.speak(req.text, TextToSpeech.QUEUE_FLUSH, params, "nag-${counter++}")
            if (r != TextToSpeech.SUCCESS) fallbackSound()
        } catch (t: Throwable) {
            Log.e(TAG, "TTS gagal bicara", t)
            fallbackSound()
        }
    }

    private fun finish() {
        val cb = current
        current = null
        cb?.invoke()
    }

    private fun fallbackSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(appContext, uri)?.play()
        } catch (t: Throwable) {
            Log.w(TAG, "Nada cadangan gagal", t)
        }
        handler.postDelayed({ finish() }, 1_500L)
    }

    private fun volumeFor(level: NagLevel): Float {
        val factor = when (level) {
            NagLevel.DORMANT, NagLevel.CALM -> 0.6f
            NagLevel.WORRIED -> 0.7f
            NagLevel.STRESSED -> 0.8f
            NagLevel.PANIC -> 0.9f
            NagLevel.CRITICAL -> 1.0f   // L5 paling kencang (Bagian 5.3)
            NagLevel.OVERDUE -> 0.6f
        }
        return (settings.volume / 100f * factor).coerceIn(0f, 1f)
    }

    /** Paket "Dramatis" menaikkan pitch & tempo seiring level; "Santai" nyaris datar. */
    private fun pitchAndRate(level: NagLevel): Pair<Float, Float> {
        if (settings.voicePack != SettingsPrefs.PACK_DRAMATIC) {
            return if (level == NagLevel.OVERDUE) 0.9f to 0.9f else 1.0f to 1.0f
        }
        return when (level) {
            NagLevel.DORMANT, NagLevel.CALM -> 1.0f to 0.95f
            NagLevel.WORRIED -> 1.05f to 1.0f
            NagLevel.STRESSED -> 1.1f to 1.1f
            NagLevel.PANIC -> 1.2f to 1.2f
            NagLevel.CRITICAL -> 1.3f to 1.3f
            NagLevel.OVERDUE -> 0.8f to 0.85f
        }
    }

    fun stop() {
        pending = null
        try { tts?.stop() } catch (_: Throwable) {}
    }

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        current = null
        pending = null
        try { tts?.stop(); tts?.shutdown() } catch (_: Throwable) {}
        tts = null
    }

    private companion object {
        const val TAG = "VoiceManager"
    }
}
