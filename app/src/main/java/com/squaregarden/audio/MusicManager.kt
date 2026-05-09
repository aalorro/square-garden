package com.squaregarden.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.squaregarden.R

/**
 * Singleton that manages background music playback via MediaPlayer.
 * Handles intro (Home) music and win/perfect-game celebration music.
 */
object MusicManager {

    private var introPlayer: MediaPlayer? = null
    private var winPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    /** Start looping intro music (HomeScreen). No-op if already playing. */
    fun startIntro(context: Context) {
        if (introPlayer?.isPlaying == true) return
        stopAll()
        try {
            introPlayer = MediaPlayer.create(context, R.raw.intro_music)?.apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
                start()
            }
        } catch (_: Exception) {}
    }

    fun stopIntro() {
        try {
            introPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        introPlayer = null
    }

    /**
     * Start win celebration music.
     * @param loop true for perfect-game (loops until stopped), false for regular win (~8 sec with fade-out).
     */
    fun startWinMusic(context: Context, loop: Boolean) {
        stopWinMusic()
        try {
            winPlayer = MediaPlayer.create(context, R.raw.perfect_game_music)?.apply {
                isLooping = loop
                setVolume(0.7f, 0.7f)
                start()
            }
        } catch (_: Exception) {}
        if (!loop) {
            // Start 1-second fade-out at 7 seconds, then stop at 8 seconds
            val fadeRunnable = Runnable { fadeOutWinMusic() }
            handler.postDelayed(fadeRunnable, 7000)
            stopRunnable = fadeRunnable
        }
    }

    private fun fadeOutWinMusic() {
        val player = winPlayer ?: return
        val steps = 20
        val intervalMs = 1000L / steps // 50ms per step over 1 second
        for (i in 1..steps) {
            handler.postDelayed({
                try {
                    val vol = 0.7f * (1f - i.toFloat() / steps)
                    player.setVolume(vol, vol)
                } catch (_: Exception) {}
            }, i * intervalMs)
        }
        // Stop after fade completes
        val finalStop = Runnable { stopWinMusic() }
        handler.postDelayed(finalStop, 1000L + 50)
        stopRunnable = finalStop
    }

    fun stopWinMusic() {
        handler.removeCallbacksAndMessages(null)
        stopRunnable = null
        try {
            winPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        winPlayer = null
    }

    fun stopAll() {
        stopIntro()
        stopWinMusic()
    }
}
