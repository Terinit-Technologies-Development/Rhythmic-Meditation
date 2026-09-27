package com.terinit.rhythmicmeditation.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Optional local sensory cues (bells/haptics).
 *
 * Purely local: tones are synthesized on-device (no audio assets, no network),
 * and they are never part of qualification — the preferences gate them off
 * completely without affecting session evidence.
 */
object SessionCues {

    /**
     * Soft synthesized bell. Fire-and-forget on a short-lived thread; failures
     * are swallowed — a missing cue must never disturb a meditation.
     */
    fun playBell(
        frequencyHz: Double = 528.0,
        durationMs: Int = 700,
        volume: Float = 0.18f
    ) {
        val thread = Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22_050
                val samples = (sampleRate * durationMs / 1000)
                val buffer = ShortArray(samples) { index ->
                    val t = index.toDouble() / sampleRate
                    val envelope = exp(-3.2 * t)
                    val value = sin(2.0 * PI * frequencyHz * t) * envelope
                    (value * Short.MAX_VALUE * volume).toInt().toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                // Hold the thread briefly so the static buffer can drain.
                Thread.sleep(durationMs.toLong() + 120L)
            } catch (_: Throwable) {
                // Audio is optional; never crash for a cue.
            } finally {
                try {
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }
}
