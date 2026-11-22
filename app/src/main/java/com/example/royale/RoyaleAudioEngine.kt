package com.example.royale

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight procedural sound effects & battle ambience synthesizer for Arena Royale.
 * Respects user settings (musicEnabled, sfxEnabled, volume).
 */
class RoyaleAudioEngine {
    @Volatile var sfxEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = true
    @Volatile var masterVolume: Float = 0.8f

    enum class SfxType {
        CLICK,
        DEPLOY,
        HIT,
        SPELL,
        TOWER_DOWN,
        VICTORY,
        DEFEAT,
        CHEST_OPEN
    }

    fun playSfx(type: SfxType) {
        if (!sfxEnabled || masterVolume <= 0.02f) return
        val vol = masterVolume.coerceIn(0f, 1f)
        thread(start = true, name = "RoyaleSfx") {
            val sampleRate = 22050
            val durationSec = when (type) {
                SfxType.CLICK -> 0.055f
                SfxType.DEPLOY -> 0.14f
                SfxType.HIT -> 0.08f
                SfxType.SPELL -> 0.22f
                SfxType.TOWER_DOWN -> 0.35f
                SfxType.VICTORY, SfxType.DEFEAT, SfxType.CHEST_OPEN -> 0.45f
            }
            val totalSamples = (sampleRate * durationSec).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toFloat() / sampleRate
                val env = exp(-t * when (type) {
                    SfxType.CLICK -> 38f
                    SfxType.HIT -> 24f
                    SfxType.VICTORY, SfxType.CHEST_OPEN -> 4.5f
                    else -> 8f
                })
                val wave = when (type) {
                    SfxType.CLICK -> sin(2.0 * PI * 760.0 * t)
                    SfxType.DEPLOY -> sin(2.0 * PI * (240.0 + 380.0 * t) * t)
                    SfxType.HIT -> sin(2.0 * PI * (180.0 - 90.0 * t) * t)
                    SfxType.SPELL -> sin(2.0 * PI * (520.0 - 200.0 * t) * t) * 0.7 + sin(2.0 * PI * 880.0 * t) * 0.3
                    SfxType.TOWER_DOWN -> sin(2.0 * PI * (120.0 - 60.0 * t) * t)
                    SfxType.VICTORY -> {
                        val note = when {
                            t < 0.14f -> 523.25
                            t < 0.28f -> 659.25
                            else -> 783.99
                        }
                        sin(2.0 * PI * note * t)
                    }
                    SfxType.DEFEAT -> {
                        val note = if (t < 0.22f) 349.23 else 261.63
                        sin(2.0 * PI * note * t)
                    }
                    SfxType.CHEST_OPEN -> {
                        val note = if (t < 0.2f) 440.0 else 880.0
                        sin(2.0 * PI * note * t)
                    }
                }
                pcm[i] = (wave * env * vol * 0.35 * Short.MAX_VALUE).toInt().toShort()
            }

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((durationSec * 1000).toLong() + 30L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore if AudioTrack unavailable in test/headless environment
            }
        }
    }
}
