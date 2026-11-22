package com.example.drift

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural real-time WebAudio-style synthesizer using Android AudioTrack.
 * Generates sawtooth engine harmonics + bandpass-like tire screech when drifting.
 */
class DriftAudioEngine {
    @Volatile private var running = false
    @Volatile private var targetSpeed = 0f
    @Volatile private var targetDrifting = false
    @Volatile private var targetBoosting = false
    @Volatile private var audioMuted = false

    private var workerThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    fun setMuted(muted: Boolean) {
        audioMuted = muted
    }

    fun start() {
        if (running) return
        running = true
        workerThread = thread(start = true, name = "DriftAudioThread") {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(1024)

            val track = try {
                AudioTrack.Builder()
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
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Throwable) {
                running = false
                return@thread
            }

            audioTrack = track
            try {
                track.play()
            } catch (_: Throwable) {
                running = false
                track.release()
                return@thread
            }

            val chunkSamples = 512
            val buffer = ShortArray(chunkSamples)
            var phase = 0.0
            var subPhase = 0.0
            var smoothedFreq = 52.0
            var smoothedScreech = 0.0
            var bpState1 = 0.0
            var bpState2 = 0.0

            while (running) {
                if (audioMuted) {
                    buffer.fill(0)
                    track.write(buffer, 0, chunkSamples)
                    continue
                }

                val absSpd = abs(targetSpeed).coerceAtMost(220f)
                val boostAdd = if (targetBoosting) 42.0 else 0.0
                val desiredFreq = 46.0 + absSpd * 0.95 + boostAdd
                val desiredScreech = if (targetDrifting && absSpd > 28f) 1.0 else 0.0
                val engineGain = if (absSpd < 2f) 0.035 else if (targetBoosting) 0.14 else 0.09

                for (i in 0 until chunkSamples) {
                    smoothedFreq += (desiredFreq - smoothedFreq) * 0.0025
                    smoothedScreech += (desiredScreech - smoothedScreech) * 0.004

                    phase += smoothedFreq / sampleRate
                    if (phase >= 1.0) phase -= 1.0

                    subPhase += (smoothedFreq * 2.0) / sampleRate
                    if (subPhase >= 1.0) subPhase -= 1.0

                    // Warm low-passed sawtooth + second harmonic
                    val saw = (phase * 2.0 - 1.0) * 0.65 + sin(subPhase * 2.0 * PI) * 0.35
                    val engineSample = saw * engineGain

                    // Simple resonant noise for tire screech
                    val white = Random.nextDouble() * 2.0 - 1.0
                    bpState1 += 0.28 * (white - bpState1)
                    bpState2 += 0.14 * (bpState1 - bpState2)
                    val screechSample = (bpState1 - bpState2) * smoothedScreech * 0.22

                    val mixed = (engineSample + screechSample).coerceIn(-0.95, 0.95)
                    buffer[i] = (mixed * Short.MAX_VALUE * 0.45).toInt().toShort()
                }

                try {
                    track.write(buffer, 0, chunkSamples)
                } catch (_: Throwable) {
                    break
                }
            }

            try {
                track.pause()
                track.flush()
                track.release()
            } catch (_: Throwable) {
                // ignore cleanup errors
            }
            audioTrack = null
        }
    }

    fun update(speedKmh: Float, isDrifting: Boolean, isBoosting: Boolean) {
        targetSpeed = speedKmh
        targetDrifting = isDrifting
        targetBoosting = isBoosting
    }

    fun stop() {
        running = false
        workerThread?.interrupt()
        workerThread = null
    }
}
