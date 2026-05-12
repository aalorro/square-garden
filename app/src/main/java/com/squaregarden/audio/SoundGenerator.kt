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

    // ---- Brass synthesis helpers ----

    /** Brass instrument timbre: fundamental + harmonics for warm brassy tone */
    private fun brass(freq: Float, i: Int, brightness: Float = 0.7f): Float {
        val b = brightness.coerceIn(0f, 1f)
        return (sine(freq, i) +
                sine(freq * 2f, i) * 0.65f +
                sine(freq * 3f, i) * 0.42f * b +
                sine(freq * 4f, i) * 0.24f * b +
                sine(freq * 5f, i) * 0.12f * b * b +
                sine(freq * 6f, i) * 0.06f * b * b) / 2.49f
    }

    /** Three detuned brass voices for full section feel */
    private fun brassChoir(freq: Float, i: Int, brightness: Float = 0.7f): Float {
        return (brass(freq * 0.996f, i, brightness) +
                brass(freq, i, brightness) +
                brass(freq * 1.004f, i, brightness)) / 3f
    }

    /** Smooth envelope for individual notes within a larger piece */
    private fun noteGate(progress: Float, start: Float, end: Float): Float {
        if (progress < start || progress >= end) return 0f
        val pos = (progress - start) / (end - start)
        return when {
            pos < 0.1f -> pos / 0.1f
            pos > 0.8f -> (1f - pos) / 0.2f
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    /** Quick soft tap blip */
    fun generateTap(): ShortArray = generatePcm(80) { i, total ->
        val env = envelope(i, total, 5, 40)
        env * 0.5f * sine(880f, i)
    }

    /** Simple beep for solution replay goal completion */
    fun generateBeep(): ShortArray = generatePcm(120) { i, total ->
        val env = envelope(i, total, 5, 60)
        env * 0.5f * sine(1047f, i) // C6 — clean, simple
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

    /** 1-star: warm brass fanfare with ascending Bb major arpeggio (~6.5 seconds) */
    fun generateWin1Star(): ShortArray = generatePcm(6500) { i, total ->
        val master = envelope(i, total, 60, 1800)
        val p = i.toFloat() / total
        val vib = 1f + 0.003f * sine(5f, i)

        // Melody: ascending Bb major arpeggio, single brass building to choir
        val melody = (
            noteGate(p, 0.00f, 0.16f) * brass(349f * vib, i, 0.6f) +     // F4 pickup
            noteGate(p, 0.13f, 0.29f) * brass(466f * vib, i, 0.7f) +     // Bb4
            noteGate(p, 0.26f, 0.43f) * brass(587f * vib, i, 0.75f) +    // D5
            noteGate(p, 0.40f, 0.58f) * brass(698f * vib, i, 0.8f) +     // F5
            noteGate(p, 0.55f, 1.00f) * brassChoir(932f * vib, i, 0.85f) // Bb5 sustained
        ) * 0.40f

        // Bass: Bb3 pedal
        val bass = noteGate(p, 0.13f, 1.00f) * brass(233f, i, 0.4f) * 0.20f

        // Chord fill on sustain: D5 + F5
        val harmony = if (p > 0.58f) {
            val h = ((p - 0.58f) / 0.08f).coerceIn(0f, 1f) * ((1f - p) / 0.3f).coerceIn(0f, 1f)
            h * (brass(587f * vib, i, 0.5f) * 0.10f + brass(698f * vib, i, 0.5f) * 0.08f)
        } else 0f

        // Soft cymbal shimmer on sustain
        val cymbal = if (p > 0.55f) {
            val c = ((p - 0.55f) / 0.06f).coerceIn(0f, 1f) * ((1f - p) / 0.35f).coerceIn(0f, 1f)
            (Math.random().toFloat() - 0.5f) * 0.04f * c +
            sine(6500f, i) * 0.02f * c * sine(0.5f, i)
        } else 0f

        master * (melody + bass + harmony + cymbal)
    }

    /** 2-star: triumphant brass section fanfare with timpani (~7.5 seconds) */
    fun generateWin2Star(): ShortArray = generatePcm(7500) { i, total ->
        val master = envelope(i, total, 60, 2000)
        val p = i.toFloat() / total
        val vib = 1f + 0.004f * sine(5.5f, i)

        // Melody: bold ascending Bb major run with brass choir
        val melody = (
            noteGate(p, 0.00f, 0.10f) * brassChoir(349f * vib, i, 0.65f) +   // F4 pickup
            noteGate(p, 0.08f, 0.19f) * brassChoir(466f * vib, i, 0.7f) +    // Bb4
            noteGate(p, 0.17f, 0.28f) * brassChoir(587f * vib, i, 0.75f) +   // D5
            noteGate(p, 0.26f, 0.38f) * brassChoir(698f * vib, i, 0.8f) +    // F5
            noteGate(p, 0.35f, 0.50f) * brassChoir(932f * vib, i, 0.85f) +   // Bb5
            noteGate(p, 0.47f, 0.60f) * brassChoir(1175f * vib, i, 0.9f) +   // D6
            noteGate(p, 0.57f, 1.00f) * brassChoir(932f * vib, i, 0.9f)      // Bb5 resolve
        ) * 0.35f

        // Counter-melody harmony (thirds)
        val harmony = (
            noteGate(p, 0.35f, 0.50f) * brass(698f * vib, i, 0.6f) +         // F5 under Bb5
            noteGate(p, 0.47f, 0.60f) * brass(932f * vib, i, 0.6f) +         // Bb5 under D6
            noteGate(p, 0.57f, 1.00f) * brassChoir(698f * vib, i, 0.65f)     // F5 under Bb5
        ) * 0.18f

        // Bass voice: Bb3 then F3
        val bass = (
            noteGate(p, 0.08f, 0.45f) * brass(233f, i, 0.4f) +
            noteGate(p, 0.45f, 1.00f) * brass(175f, i, 0.45f)
        ) * 0.18f

        // Timpani accent at climax
        val timpani = if (p in 0.47f..0.58f) {
            val t = ((0.58f - p) / 0.11f).coerceIn(0f, 1f)
            sine(87f, i) * 0.15f * t * t
        } else 0f

        // Cymbal crash at D6 entry + sustain shimmer
        val cymbal = if (p > 0.47f) {
            val attack = ((p - 0.47f) / 0.02f).coerceIn(0f, 1f)
            val decay = ((1f - p) / 0.45f).coerceIn(0f, 1f)
            val c = attack * decay
            (Math.random().toFloat() - 0.5f) * 0.06f * c +
            sine(7200f, i) * 0.015f * c + sine(5800f, i) * 0.01f * c
        } else 0f

        master * (melody + harmony + bass + timpani + cymbal)
    }

    /** 3-star: grand brass section fanfare with percussion (~8.5 seconds) */
    fun generateWin3Star(): ShortArray = generatePcm(8500) { i, total ->
        val master = envelope(i, total, 80, 2500)
        val p = i.toFloat() / total
        val vib = 1f + 0.005f * sine(5f, i)

        // Melody: full brass section grand ascending fanfare
        val melody = (
            noteGate(p, 0.00f, 0.07f) * brassChoir(233f * vib, i, 0.6f) +    // Bb3 intro
            noteGate(p, 0.06f, 0.13f) * brassChoir(349f * vib, i, 0.65f) +   // F4
            noteGate(p, 0.12f, 0.20f) * brassChoir(466f * vib, i, 0.7f) +    // Bb4
            noteGate(p, 0.18f, 0.28f) * brassChoir(587f * vib, i, 0.75f) +   // D5
            noteGate(p, 0.26f, 0.36f) * brassChoir(698f * vib, i, 0.8f) +    // F5
            noteGate(p, 0.34f, 0.46f) * brassChoir(932f * vib, i, 0.85f) +   // Bb5
            noteGate(p, 0.44f, 0.55f) * brassChoir(1175f * vib, i, 0.9f) +   // D6
            noteGate(p, 0.53f, 0.64f) * brassChoir(1397f * vib, i, 0.95f) +  // F6 peak!
            noteGate(p, 0.62f, 1.00f) * brassChoir(932f * vib, i, 0.9f)      // Bb5 grand resolve
        ) * 0.30f

        // Rich chord harmony on resolve
        val harmony = if (p > 0.62f) {
            val h = ((p - 0.62f) / 0.06f).coerceIn(0f, 1f) * ((1f - p) / 0.3f).coerceIn(0f, 1f)
            h * (brassChoir(587f * vib, i, 0.6f) * 0.10f +
                 brassChoir(698f * vib, i, 0.6f) * 0.08f +
                 brassChoir(1175f * vib, i, 0.5f) * 0.06f)
        } else 0f

        // Counter-melody thirds on the way up
        val counter = (
            noteGate(p, 0.26f, 0.36f) * brass(587f * vib, i, 0.6f) +
            noteGate(p, 0.34f, 0.46f) * brass(698f * vib, i, 0.6f) +
            noteGate(p, 0.44f, 0.55f) * brass(932f * vib, i, 0.65f)
        ) * 0.12f

        // Bass: Bb2 pedal
        val bass = noteGate(p, 0.06f, 1.00f) * brass(117f, i, 0.4f) * 0.18f

        // Timpani hits on accent notes
        val timpani = (
            if (p in 0.34f..0.42f) sine(117f, i) * 0.10f * ((0.42f - p) / 0.08f).coerceIn(0f, 1f) else 0f
        ) + (
            if (p in 0.53f..0.62f) sine(87f, i) * 0.14f * ((0.62f - p) / 0.09f).coerceIn(0f, 1f) else 0f
        )

        // Snare roll building to F6 climax
        val snareRoll = if (p in 0.48f..0.53f) {
            val intensity = ((p - 0.48f) / 0.05f).coerceIn(0f, 1f)
            val roll = sin(p.toDouble() * 1200.0).toFloat().let { if (it > 0f) 1f else 0f }
            (Math.random().toFloat() - 0.5f) * 0.10f * intensity * roll
        } else 0f

        // Grand cymbal crash at F6 peak + sustain shimmer
        val cymbal = if (p > 0.53f) {
            val attack = if (p < 0.55f) ((p - 0.53f) / 0.02f) else 1f
            val decay = ((1f - p) / 0.4f).coerceIn(0f, 1f)
            val c = attack.coerceIn(0f, 1f) * decay
            (Math.random().toFloat() - 0.5f) * 0.07f * c +
            sine(6800f, i) * 0.02f * c + sine(8200f, i) * 0.01f * c
        } else 0f

        master * (melody + harmony + counter + bass + timpani + snareRoll + cymbal)
    }

    /** Perfect game: epic two-part brass celebration with key change (~11 seconds) */
    fun generatePerfectGame(): ShortArray = generatePcm(11000) { i, total ->
        val master = envelope(i, total, 100, 3000)
        val p = i.toFloat() / total
        val vib = 1f + 0.005f * sine(4.5f, i)

        // === PART 1 (0.0-0.45): Building fanfare in Bb major ===
        val part1Melody = (
            noteGate(p, 0.00f, 0.06f) * brassChoir(233f * vib, i, 0.65f) +   // Bb3
            noteGate(p, 0.05f, 0.10f) * brassChoir(349f * vib, i, 0.7f) +    // F4
            noteGate(p, 0.09f, 0.15f) * brassChoir(466f * vib, i, 0.75f) +   // Bb4
            noteGate(p, 0.14f, 0.20f) * brassChoir(587f * vib, i, 0.8f) +    // D5
            noteGate(p, 0.19f, 0.26f) * brassChoir(698f * vib, i, 0.85f) +   // F5
            noteGate(p, 0.25f, 0.33f) * brassChoir(932f * vib, i, 0.9f) +    // Bb5
            noteGate(p, 0.32f, 0.45f) * brassChoir(1175f * vib, i, 0.9f)     // D6 building...
        ) * 0.28f

        val part1Harmony = (
            noteGate(p, 0.19f, 0.26f) * brass(466f * vib, i, 0.6f) +
            noteGate(p, 0.25f, 0.33f) * brass(698f * vib, i, 0.6f) +
            noteGate(p, 0.32f, 0.45f) * brass(932f * vib, i, 0.6f)
        ) * 0.12f

        // Dramatic timpani roll + snare transition (0.43-0.47)
        val transitionRoll = if (p in 0.43f..0.47f) {
            val intensity = ((p - 0.43f) / 0.04f).coerceIn(0f, 1f)
            val roll = sin(p.toDouble() * 1500.0).toFloat().let { if (it > 0f) 1f else 0f }
            sine(110f, i) * 0.12f * intensity +
            (Math.random().toFloat() - 0.5f) * 0.10f * intensity * roll
        } else 0f

        // === PART 2 (0.47-1.0): Grand celebration in C major (key change!) ===
        val part2Melody = (
            noteGate(p, 0.47f, 0.53f) * brassChoir(523f * vib, i, 0.85f) +   // C5
            noteGate(p, 0.52f, 0.58f) * brassChoir(659f * vib, i, 0.9f) +    // E5
            noteGate(p, 0.57f, 0.63f) * brassChoir(784f * vib, i, 0.9f) +    // G5
            noteGate(p, 0.62f, 0.70f) * brassChoir(1047f * vib, i, 0.95f) +  // C6
            noteGate(p, 0.68f, 0.76f) * brassChoir(1319f * vib, i, 1.0f) +   // E6
            noteGate(p, 0.74f, 1.00f) * brassChoir(1047f * vib, i, 0.95f)    // C6 grand sustain
        ) * 0.28f

        // Grand chord on final sustain: full C major voiced wide
        val grandChord = if (p > 0.76f) {
            val g = ((p - 0.76f) / 0.06f).coerceIn(0f, 1f) * ((1f - p) / 0.2f).coerceIn(0f, 1f)
            g * (brassChoir(523f * vib, i, 0.7f) * 0.08f +
                 brassChoir(659f * vib, i, 0.7f) * 0.07f +
                 brassChoir(784f * vib, i, 0.65f) * 0.06f +
                 brassChoir(1319f * vib, i, 0.6f) * 0.05f)
        } else 0f

        // Deep bass
        val bass = (
            noteGate(p, 0.00f, 0.45f) * brass(117f, i, 0.4f) * 0.15f +
            noteGate(p, 0.47f, 1.00f) * brass(131f, i, 0.45f) * 0.18f
        )

        // Cymbal crash at part 2 entry
        val cymbal1 = if (p in 0.47f..0.70f) {
            val c = ((p - 0.47f) / 0.02f).coerceIn(0f, 1f) * ((0.70f - p) / 0.20f).coerceIn(0f, 1f)
            (Math.random().toFloat() - 0.5f) * 0.07f * c +
            sine(7500f, i) * 0.015f * c
        } else 0f

        // Second cymbal crash at grand chord
        val cymbal2 = if (p > 0.74f) {
            val c = ((p - 0.74f) / 0.02f).coerceIn(0f, 1f) * ((1f - p) / 0.22f).coerceIn(0f, 1f)
            (Math.random().toFloat() - 0.5f) * 0.08f * c +
            sine(6200f, i) * 0.02f * c + sine(8800f, i) * 0.01f * c
        } else 0f

        // Timpani accents in part 2
        val timpani = (
            if (p in 0.47f..0.54f) sine(131f, i) * 0.12f * ((0.54f - p) / 0.07f).coerceIn(0f, 1f) else 0f
        ) + (
            if (p in 0.62f..0.68f) sine(98f, i) * 0.10f * ((0.68f - p) / 0.06f).coerceIn(0f, 1f) else 0f
        )

        // Shimmering high overtones on final sustain
        val shimmer = if (p > 0.78f) {
            val s = ((p - 0.78f) / 0.05f).coerceIn(0f, 1f) * ((1f - p) / 0.18f).coerceIn(0f, 1f)
            sine(4186f, i) * 0.03f * s * sine(2f, i) +
            sine(5274f, i) * 0.02f * s * sine(3f, i)
        } else 0f

        master * (part1Melody + part1Harmony + transitionRoll + part2Melody + grandChord +
                  bass + cymbal1 + cymbal2 + timpani + shimmer)
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

    /** Shuffle: whooshing card-scatter with cascading tones */
    fun generateShuffle(): ShortArray = generatePcm(500) { i, total ->
        val env = envelope(i, total, 15, 150)
        val progress = i.toFloat() / total
        // Two sweeps: one up, one down — creates a scrambling feel
        val freqUp = 200f + 1000f * progress
        val freqDown = 900f - 500f * progress
        val noise = (Math.random().toFloat() - 0.5f) * 0.15f * (1f - progress)
        env * 0.5f * (
            sine(freqUp, i) * 0.3f +
            sine(freqDown, i) * 0.2f +
            sine(freqUp * 1.5f, i) * 0.15f +
            noise
        )
    }

    /** Passthrough: ethereal phase-shift whoosh — tile passing through barriers */
    fun generatePassthrough(): ShortArray = generatePcm(400) { i, total ->
        val env = envelope(i, total, 15, 120)
        val progress = i.toFloat() / total
        // Phaser-style modulated sweep — sounds like passing through a wall
        val baseFreq = 350f + 300f * sin(progress * PI.toFloat())
        val phaser = sine(baseFreq, i) * 0.4f + sine(baseFreq * 1.01f, i) * 0.4f // phase beating
        val shimmer = 0.2f * sine(2400f * progress, i) * (1f - progress)
        val whoosh = (Math.random().toFloat() - 0.5f) * 0.08f * sin(progress * PI.toFloat())
        env * 0.55f * (phaser + shimmer + whoosh)
    }

    /** Unfreeze: crystalline ice-crack with bright chime */
    fun generateUnfreeze(): ShortArray = generatePcm(450) { i, total ->
        val env = envelope(i, total, 5, 180)
        val progress = i.toFloat() / total
        // Sharp crack at start, then bright rising chime
        val crack = if (progress < 0.15f) {
            val crackEnv = (1f - progress / 0.15f)
            (Math.random().toFloat() - 0.5f) * 0.6f * crackEnv
        } else 0f
        val chimeFreq = 1200f + 800f * (progress - 0.15f).coerceIn(0f, 1f)
        val chime = if (progress > 0.1f) {
            val chimeEnv = if (progress < 0.25f) (progress - 0.1f) / 0.15f else 1f
            chimeEnv * (sine(chimeFreq, i) * 0.4f + sine(chimeFreq * 2f, i) * 0.2f + sine(chimeFreq * 3f, i) * 0.1f)
        } else 0f
        // Icy sparkle overtones
        val sparkle = if (progress > 0.2f) 0.12f * sine(3800f, i) * sin(progress * 18.0).toFloat().coerceIn(0f, 1f) else 0f
        env * 0.6f * (crack + chime + sparkle)
    }

    /** Common scramble envelope: 10ms attack, fade out last 500ms of 4s */
    private fun scrambleEnv(i: Int, total: Int): Float {
        val progress = i.toFloat() / total
        return when {
            i < SAMPLE_RATE * 10 / 1000 -> i.toFloat() / (SAMPLE_RATE * 10 / 1000)
            progress > 0.875f -> (1f - progress) / 0.125f
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    /** Scramble 1: Low hum with digital glitch bursts */
    fun generateScramble1(): ShortArray = generatePcm(4000) { i, total ->
        val progress = i.toFloat() / total
        val env = scrambleEnv(i, total)
        val baseFreq = 80f + 40f * progress
        val wobble = 1f + 0.08f * sine(3f + 2f * progress, i)
        val hum = sine(baseFreq * wobble, i) * 0.35f + sine(baseFreq * 2f * wobble, i) * 0.15f
        val glitchCycle = sin(progress * 47.0).toFloat()
        val glitch = if (glitchCycle > 0.7f) {
            (Math.random().toFloat() - 0.5f) * 0.4f * (glitchCycle - 0.7f) / 0.3f
        } else 0f
        val buzzFreq = 200f + 300f * progress
        val buzz = sine(buzzFreq, i) * 0.12f * progress
        val artifact = sine(1800f + 600f * sin(progress * 23.0).toFloat(), i) * 0.06f *
            sin(progress * 31.0).toFloat().coerceIn(0f, 1f)
        env * 0.7f * (hum + glitch + buzz + artifact)
    }

    /** Scramble 2: Stuttering dial-up modem — rapid bit-crush pulses */
    fun generateScramble2(): ShortArray = generatePcm(4000) { i, total ->
        val progress = i.toFloat() / total
        val env = scrambleEnv(i, total)
        // Carrier tone sweeping up like a modem handshake
        val carrier = sine(600f + 1400f * progress, i) * 0.3f
        // Stutter: square-wave gate at increasing rate
        val gateRate = 8f + 20f * progress
        val gate = if (sine(gateRate, i) > 0f) 1f else 0.15f
        // Bit-crush noise bursts every ~400ms
        val burstPhase = (progress * 10f) % 1f
        val burst = if (burstPhase < 0.2f) {
            (Math.random().toFloat() - 0.5f) * 0.35f * (1f - burstPhase / 0.2f)
        } else 0f
        // Undertone hum
        val hum = sine(60f, i) * 0.15f
        env * 0.65f * (carrier * gate + burst + hum)
    }

    /** Scramble 3: Warped tape / vinyl wobble — pitch-bending drone */
    fun generateScramble3(): ShortArray = generatePcm(4000) { i, total ->
        val progress = i.toFloat() / total
        val env = scrambleEnv(i, total)
        // Wobbly drone that sounds like a warped record
        val warpSpeed = 2f + 4f * progress
        val warpDepth = 0.25f + 0.15f * sine(0.5f, i)
        val baseFreq = 150f + 50f * progress
        val warpedFreq = baseFreq * (1f + warpDepth * sine(warpSpeed, i))
        val drone = sine(warpedFreq, i) * 0.3f + sine(warpedFreq * 1.5f, i) * 0.15f
        // Crackle — sparse random pops like vinyl
        val crackle = if (Math.random() < 0.003 + 0.007 * progress) {
            (Math.random().toFloat() - 0.5f) * 0.5f
        } else 0f
        // Sub-bass rumble
        val sub = sine(40f + 10f * sine(0.3f, i), i) * 0.2f
        // Ghostly high overtone fading in
        val ghost = sine(1200f * (1f + 0.1f * sine(1.5f, i)), i) * 0.05f * progress
        env * 0.7f * (drone + crackle + sub + ghost)
    }

    /** Scramble 4: Electric buzz saw — harsh sawtooth with resonant sweep */
    fun generateScramble4(): ShortArray = generatePcm(4000) { i, total ->
        val progress = i.toFloat() / total
        val env = scrambleEnv(i, total)
        // Sawtooth wave (buzzy, electric)
        val sawFreq = 100f + 60f * progress
        val sawPhase = (i.toFloat() * sawFreq / SAMPLE_RATE) % 1f
        val saw = (sawPhase * 2f - 1f) * 0.25f
        // Resonant filter sweep — sine modulated amplitude at sweeping freq
        val resFreq = 400f + 800f * (0.5f + 0.5f * sine(1.2f, i))
        val resonance = sine(resFreq, i) * 0.2f * (0.5f + 0.5f * sine(0.8f, i))
        // Electrical spark pops — irregular bursts
        val sparkTrigger = sin(progress * 53.0 + 7.0).toFloat()
        val spark = if (sparkTrigger > 0.85f) {
            (Math.random().toFloat() - 0.5f) * 0.45f * (sparkTrigger - 0.85f) / 0.15f
        } else 0f
        // Power-line 60Hz undertone
        val powerLine = sine(60f, i) * 0.1f + sine(120f, i) * 0.05f
        env * 0.65f * (saw + resonance + spark + powerLine)
    }

    /** Scramble 5: Data corruption — rapid digital chirps with static */
    fun generateScramble5(): ShortArray = generatePcm(4000) { i, total ->
        val progress = i.toFloat() / total
        val env = scrambleEnv(i, total)
        // Rapid chirps — short sine bursts at semi-random frequencies
        val chirpRate = 12f + 8f * progress
        val chirpPhase = (progress * chirpRate) % 1f
        val chirpFreq = 800f + 1200f * sin(progress * 37.0).toFloat().coerceIn(0f, 1f)
        val chirp = if (chirpPhase < 0.4f) {
            sine(chirpFreq, i) * 0.3f * (1f - chirpPhase / 0.4f)
        } else 0f
        // Static noise bed, thickening over time
        val staticNoise = (Math.random().toFloat() - 0.5f) * (0.08f + 0.12f * progress)
        // Low digital rumble
        val rumble = sine(70f + 30f * sine(0.7f, i), i) * 0.2f
        // Descending data-dump tone
        val dataTone = sine(2000f - 1200f * progress, i) * 0.06f *
            sin(progress * 19.0).toFloat().coerceIn(0f, 1f)
        env * 0.7f * (chirp + staticNoise + rumble + dataTone)
    }

    // ---- Procedural goal-completion celebration FX ----

    /** Goal celebration 1: Bright chime cascade — descending xylophone run */
    fun generateGoalCelebration1(): ShortArray = generatePcm(900) { i, total ->
        val p = i.toFloat() / total
        val env = envelope(i, total, 5, 300)
        // 5 descending bell tones staggered across the duration
        val note1 = noteGate(p, 0.00f, 0.35f) * sine(1760f, i) * 0.35f  // A6
        val note2 = noteGate(p, 0.12f, 0.45f) * sine(1568f, i) * 0.30f  // G6
        val note3 = noteGate(p, 0.24f, 0.58f) * sine(1319f, i) * 0.28f  // E6
        val note4 = noteGate(p, 0.38f, 0.72f) * sine(1175f, i) * 0.25f  // D6
        val note5 = noteGate(p, 0.52f, 1.00f) * sine(1047f, i) * 0.30f  // C6 landing
        // Bell-like overtones on each
        val overtones = (
            noteGate(p, 0.00f, 0.35f) * sine(1760f * 2.76f, i) * 0.08f +
            noteGate(p, 0.24f, 0.58f) * sine(1319f * 2.76f, i) * 0.06f +
            noteGate(p, 0.52f, 1.00f) * sine(1047f * 2.76f, i) * 0.07f
        )
        env * (note1 + note2 + note3 + note4 + note5 + overtones)
    }

    /** Goal celebration 2: Triumphant horn stab — quick brass chord punch */
    fun generateGoalCelebration2(): ShortArray = generatePcm(700) { i, total ->
        val p = i.toFloat() / total
        val env = envelope(i, total, 8, 250)
        val vib = 1f + 0.003f * sine(6f, i)
        // Sharp brass chord: C major triad hit
        val root = brassChoir(523f * vib, i, 0.8f) * 0.30f   // C5
        val third = brass(659f * vib, i, 0.7f) * 0.20f        // E5
        val fifth = brass(784f * vib, i, 0.7f) * 0.18f        // G5
        val octave = brass(1047f * vib, i, 0.6f) * 0.12f      // C6
        // Accent punch: quick attack bump
        val punch = if (p < 0.08f) (1f + 0.4f * (1f - p / 0.08f)) else 1f
        env * punch * (root + third + fifth + octave)
    }

    /** Goal celebration 3: Sparkle arpeggio — rising crystalline run with shimmer */
    fun generateGoalCelebration3(): ShortArray = generatePcm(1100) { i, total ->
        val p = i.toFloat() / total
        val env = envelope(i, total, 5, 400)
        // Rising pentatonic arpeggio with glassy timbre
        val note1 = noteGate(p, 0.00f, 0.28f) * sine(880f, i)   // A5
        val note2 = noteGate(p, 0.10f, 0.38f) * sine(1047f, i)  // C6
        val note3 = noteGate(p, 0.22f, 0.52f) * sine(1175f, i)  // D6
        val note4 = noteGate(p, 0.34f, 0.65f) * sine(1319f, i)  // E6
        val note5 = noteGate(p, 0.48f, 1.00f) * sine(1760f, i)  // A6 high landing
        val melody = (note1 + note2 + note3 + note4 + note5) * 0.22f
        // Crystalline harmonics (inharmonic bell partials)
        val crystal = (
            noteGate(p, 0.00f, 0.28f) * sine(880f * 3.2f, i) * 0.06f +
            noteGate(p, 0.22f, 0.52f) * sine(1175f * 2.8f, i) * 0.05f +
            noteGate(p, 0.48f, 1.00f) * sine(1760f * 2.4f, i) * 0.07f
        )
        // Twinkling shimmer on the sustained high note
        val shimmer = if (p > 0.55f) {
            val s = ((p - 0.55f) / 0.1f).coerceIn(0f, 1f) * ((1f - p) / 0.35f).coerceIn(0f, 1f)
            sine(4400f, i) * 0.04f * s * sine(8f, i) +
            sine(5280f, i) * 0.03f * s * sine(11f, i)
        } else 0f
        env * (melody + crystal + shimmer)
    }

    /** Goal celebration 4: Tubular bell hit — warm resonant bell with low sustain */
    fun generateGoalCelebration4(): ShortArray = generatePcm(1200) { i, total ->
        val p = i.toFloat() / total
        // Bell: sharp attack, long exponential decay
        val decay = if (p < 0.01f) p / 0.01f else (1f - p).pow(0.6f)
        val env = decay * envelope(i, total, 2, 100)
        // Tubular bell partials (slightly inharmonic for realistic bell)
        val fundamental = sine(698f, i) * 0.30f              // F5
        val partial2 = sine(698f * 2.0f, i) * 0.22f
        val partial3 = sine(698f * 2.98f, i) * 0.14f         // slightly detuned
        val partial4 = sine(698f * 4.07f, i) * 0.08f         // inharmonic
        val partial5 = sine(698f * 5.2f, i) * 0.05f
        // Warm sub-octave hum that sustains
        val sub = sine(349f, i) * 0.12f * (1f - p * 0.5f).coerceIn(0f, 1f)
        // Gentle beating between close partials for warmth
        val beat = sine(698f * 1.003f, i) * 0.06f * (1f - p).coerceIn(0f, 1f)
        env * (fundamental + partial2 + partial3 + partial4 + partial5 + sub + beat)
    }

    /** Sad trombone: descending "wah wah wah wahhh" */
    fun generateLose(): ShortArray = generatePcm(2500) { i, total ->
        val master = envelope(i, total, 30, 600)
        val p = i.toFloat() / total
        val vib = 1f + 0.008f * sine(4f, i)

        // Four descending notes: Bb4 → A4 → Ab4 → G4 sliding down to E4
        val freq = when {
            p < 0.18f -> 466f                                     // Bb4
            p < 0.36f -> 440f                                     // A4
            p < 0.54f -> 415f                                     // Ab4
            else -> 392f - (p - 0.54f) / 0.46f * 62f             // G4 → E4 slide
        }

        // Per-note envelope for separated articulation
        val noteEnv = when {
            p < 0.18f -> {
                val n = p / 0.18f
                if (n < 0.08f) n / 0.08f else if (n > 0.85f) (1f - n) / 0.15f else 1f
            }
            p < 0.36f -> {
                val n = (p - 0.18f) / 0.18f
                if (n < 0.08f) n / 0.08f else if (n > 0.85f) (1f - n) / 0.15f else 1f
            }
            p < 0.54f -> {
                val n = (p - 0.36f) / 0.18f
                if (n < 0.08f) n / 0.08f else if (n > 0.85f) (1f - n) / 0.15f else 1f
            }
            else -> {
                // Final long note with slow decay
                val n = (p - 0.54f) / 0.46f
                if (n < 0.05f) n / 0.05f else 1f - n * 0.3f
            }
        }

        // Muted brass timbre (low brightness for womp-womp quality)
        val tone = brass(freq * vib, i, 0.35f)

        master * noteEnv.coerceIn(0f, 1f) * tone * 0.55f
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
