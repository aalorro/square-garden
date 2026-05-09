package com.squaregarden.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Generates game sound effects procedurally using sine-wave synthesis,
 * and loads sampled audio snippets from raw resources.
 */
object SoundGenerator {

    private const val SAMPLE_RATE = 22050

    /** Load a raw PCM resource (16-bit signed LE mono @ 22050Hz) into a ShortArray. */
    fun loadRawResource(context: Context, resId: Int): ShortArray {
        val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = ShortArray(bytes.size / 2)
        buffer.asShortBuffer().get(shorts)
        return shorts
    }

    private fun generatePcm(
        durationMs: Int,
        builder: (sampleIndex: Int, totalSamples: Int) -> Float
    ): ShortArray {
        val totalSamples = SAMPLE_RATE * durationMs / 1000
        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val sample = builder(i, totalSamples).coerceIn(-1f, 1f)
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }

    private fun sine(freq: Float, sampleIndex: Int): Float {
        return sin(2.0 * PI * freq * sampleIndex / SAMPLE_RATE).toFloat()
    }

    private fun envelope(sampleIndex: Int, totalSamples: Int, attackMs: Int = 10, releaseMs: Int = 50): Float {
        val attackSamples = SAMPLE_RATE * attackMs / 1000
        val releaseSamples = SAMPLE_RATE * releaseMs / 1000
        val t = sampleIndex.toFloat()
        return when {
            sampleIndex < attackSamples -> t / attackSamples
            sampleIndex > totalSamples - releaseSamples -> (totalSamples - t) / releaseSamples
            else -> 1f
        }
    }

    /** Quick soft tap blip */
    fun generateTap(): ShortArray = generatePcm(80) { i, total ->
        val env = envelope(i, total, 5, 40)
        env * 0.5f * sine(880f, i)
    }

    /** Smooth slide whoosh */
    fun generateSwap(): ShortArray = generatePcm(200) { i, total ->
        val env = envelope(i, total, 15, 80)
        val progress = i.toFloat() / total
        val freq = 300f + 400f * progress
        env * 0.4f * (sine(freq, i) * 0.6f + sine(freq * 1.5f, i) * 0.4f)
    }

    /** Satisfying match ding */
    fun generateMatch(): ShortArray = generatePcm(350) { i, total ->
        val env = envelope(i, total, 10, 150)
        env * 0.6f * (sine(660f, i) * 0.5f + sine(990f, i) * 0.3f + sine(1320f, i) * 0.2f)
    }

    /** 1-star: gentle 5-note ascending melody (~5 seconds) */
    fun generateWin1Star(): ShortArray = generatePcm(5000) { i, total ->
        val env = envelope(i, total, 40, 1200)
        val progress = i.toFloat() / total
        val freq = when {
            progress < 0.18f -> 262f   // C4
            progress < 0.34f -> 330f   // E4
            progress < 0.50f -> 392f   // G4
            progress < 0.70f -> 523f   // C5
            else -> 659f               // E5
        }
        val vibrato = 1f + 0.003f * sine(4.5f, i)
        val shimmer = if (progress > 0.65f) 0.08f * sine(2640f, i) * (1f - progress) * 2f else 0f
        env * 0.5f * (
            sine(freq * vibrato, i) * 0.5f +
            sine(freq * 2f, i) * 0.25f +
            sine(freq * 3f, i) * 0.1f +
            sine(freq * 0.5f, i) * 0.15f +
            shimmer
        )
    }

    /** 2-star: rich 6-note arpeggio with shimmer (~5.5 seconds) */
    fun generateWin2Star(): ShortArray = generatePcm(5500) { i, total ->
        val env = envelope(i, total, 40, 1500)
        val progress = i.toFloat() / total
        val freq = when {
            progress < 0.14f -> 440f   // A4
            progress < 0.28f -> 554f   // C#5
            progress < 0.42f -> 659f   // E5
            progress < 0.58f -> 880f   // A5
            progress < 0.75f -> 659f   // E5 (descend)
            else -> 880f               // A5 (resolve)
        }
        val vibrato = 1f + 0.004f * sine(5f, i)
        val shimmer = 0.06f * sine(2200f, i) * sin(progress * 10.0).toFloat().coerceIn(0f, 1f)
        val sparkle = if (progress > 0.5f) 0.1f * sine(3520f, i) * (1f - progress) * 1.5f else 0f
        env * 0.55f * (
            sine(freq * vibrato, i) * 0.4f +
            sine(freq * 2f, i) * 0.2f +
            sine(freq * 3f, i) * 0.1f +
            sine(freq * 0.5f, i) * 0.15f +
            shimmer + sparkle
        )
    }

    /** 3-star: grand 7-note fanfare with full harmonics (~6 seconds) */
    fun generateWin3Star(): ShortArray = generatePcm(6000) { i, total ->
        val env = envelope(i, total, 50, 2000)
        val progress = i.toFloat() / total
        val freq = when {
            progress < 0.10f -> 440f    // A4
            progress < 0.20f -> 554f    // C#5
            progress < 0.30f -> 659f    // E5
            progress < 0.45f -> 880f    // A5
            progress < 0.58f -> 1108f   // C#6
            progress < 0.72f -> 1318f   // E6
            else -> 880f                // settle A5
        }
        val vibrato = 1f + 0.005f * sine(5f, i)
        val shimmer = 0.1f * sine(2640f, i) * sin(progress * 8.0).toFloat().coerceIn(0f, 1f)
        val sparkle = if (progress > 0.4f) 0.12f * sine(3520f, i) * sin(progress * 14.0).toFloat().coerceIn(0f, 1f) else 0f
        val chordSustain = if (progress > 0.72f) 0.08f * sine(554f, i) + 0.06f * sine(659f, i) else 0f
        env * 0.6f * (
            sine(freq * vibrato, i) * 0.3f +
            sine(freq * 2f, i) * 0.15f +
            sine(freq * 3f, i) * 0.1f +
            sine(freq * 0.5f, i) * 0.15f +
            shimmer + sparkle + chordSustain
        )
    }

    /** Perfect game: epic ascending fanfare with chord layers (~7 seconds) */
    fun generatePerfectGame(): ShortArray = generatePcm(7000) { i, total ->
        val env = envelope(i, total, 60, 2500)
        val progress = i.toFloat() / total
        val freq = when {
            progress < 0.08f -> 440f    // A4
            progress < 0.16f -> 554f    // C#5
            progress < 0.24f -> 659f    // E5
            progress < 0.34f -> 880f    // A5
            progress < 0.44f -> 1108f   // C#6
            progress < 0.54f -> 1318f   // E6
            progress < 0.65f -> 1760f   // A6
            else -> 880f                // triumphant A5 sustain
        }
        val vibrato = 1f + 0.006f * sine(4.5f, i)
        val shimmer = 0.12f * sine(2640f, i) * sin(progress * 6.0).toFloat().coerceIn(0f, 1f)
        val sparkle = if (progress > 0.3f) 0.1f * sine(4186f, i) * sin(progress * 16.0).toFloat().coerceIn(0f, 1f) else 0f
        val sparkle2 = if (progress > 0.5f) 0.06f * sine(5274f, i) * sin(progress * 20.0).toFloat().coerceIn(0f, 1f) else 0f
        val chord = if (progress > 0.65f) {
            0.1f * sine(554f, i) + 0.08f * sine(659f, i) + 0.06f * sine(440f, i)
        } else 0f
        env * 0.55f * (
            sine(freq * vibrato, i) * 0.25f +
            sine(freq * 2f, i) * 0.12f +
            sine(freq * 3f, i) * 0.08f +
            sine(freq * 0.5f, i) * 0.15f +
            shimmer + sparkle + sparkle2 + chord
        )
    }

    /** World unlock: mysterious sweep into triumphant chord (~4 seconds) */
    fun generateWorldUnlock(): ShortArray = generatePcm(4000) { i, total ->
        val env = envelope(i, total, 50, 1000)
        val progress = i.toFloat() / total
        val freq = when {
            progress < 0.25f -> 220f + 220f * progress * 4f  // sweep 220→440
            progress < 0.40f -> 440f     // A4
            progress < 0.55f -> 554f     // C#5
            progress < 0.70f -> 659f     // E5
            else -> 880f                 // A5 resolve
        }
        val vibrato = 1f + 0.004f * sine(5f, i)
        val shimmer = if (progress > 0.25f) 0.1f * sine(2200f, i) * (1f - progress) else 0f
        val sparkle = if (progress > 0.55f) 0.12f * sine(3520f, i) * sin(progress * 12.0).toFloat().coerceIn(0f, 1f) else 0f
        val chord = if (progress > 0.7f) 0.08f * sine(554f, i) + 0.06f * sine(659f, i) else 0f
        env * 0.55f * (
            sine(freq * vibrato, i) * 0.35f +
            sine(freq * 2f, i) * 0.15f +
            sine(freq * 0.5f, i) * 0.2f +
            shimmer + sparkle + chord
        )
    }

    /** Rising sparkle chime for each star landing on the counter */
    fun generateStarCollect(): ShortArray = generatePcm(150) { i, total ->
        val env = envelope(i, total, 5, 80)
        val progress = i.toFloat() / total
        val freq = 1200f + 600f * progress // rising sparkle
        env * 0.5f * (sine(freq, i) * 0.5f + sine(freq * 2f, i) * 0.3f + sine(freq * 3f, i) * 0.2f)
    }

    /** Celebratory fanfare for life restored (~5 seconds) */
    fun generateLifeRestored(): ShortArray = generatePcm(5000) { i, total ->
        val env = envelope(i, total, 50, 1500)
        val progress = i.toFloat() / total
        // Ascending fanfare in 5 stages
        val freq = when {
            progress < 0.12f -> 440f   // A4
            progress < 0.22f -> 554f   // C#5
            progress < 0.32f -> 659f   // E5
            progress < 0.45f -> 880f   // A5
            progress < 0.60f -> 1108f  // C#6
            else -> 880f               // settle on A5
        }
        val vibrato = 1f + 0.005f * sine(4f, i)
        val shimmer = if (progress > 0.4f) 0.12f * sine(2640f, i) * (1f - progress) else 0f
        val sparkle = if (progress > 0.6f) 0.08f * sine(3520f, i) * sin(progress * 12.0).toFloat() else 0f
        env * 0.55f * (
            sine(freq * vibrato, i) * 0.35f +
            sine(freq * 2f, i) * 0.2f +
            sine(freq * 3f, i) * 0.1f +
            sine(freq * 0.5f, i) * 0.2f +
            shimmer + sparkle
        )
    }

    /** Quick rising scramble whoosh for board shuffle */
    fun generateShuffle(): ShortArray = generatePcm(300) { i, total ->
        val env = envelope(i, total, 10, 100)
        val progress = i.toFloat() / total
        val freq = 200f + 800f * progress
        val noise = (Math.random().toFloat() - 0.5f) * 0.2f * (1f - progress)
        env * 0.5f * (sine(freq, i) * 0.4f + sine(freq * 1.5f, i) * 0.3f + noise)
    }

    /** Sad descending tone for losing */
    fun generateLose(): ShortArray = generatePcm(700) { i, total ->
        val env = envelope(i, total, 20, 300)
        val progress = i.toFloat() / total
        val freq = 440f - 120f * progress
        env * 0.45f * (sine(freq, i) * 0.6f + sine(freq * 0.5f, i) * 0.4f)
    }

    suspend fun playPcm(pcm: ShortArray, volume: Float = 1f) = withContext(Dispatchers.IO) {
        val bufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufSize, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.setVolume(volume)
        track.play()

        // Wait for playback to finish, then release
        val durationMs = (pcm.size.toLong() * 1000) / SAMPLE_RATE
        kotlinx.coroutines.delay(durationMs + 50)
        track.stop()
        track.release()
    }
}
