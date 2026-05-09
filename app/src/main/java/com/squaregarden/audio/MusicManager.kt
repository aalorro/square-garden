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
     * @param loop true for perfect-game (loops until stopped), false for regular win (~7 sec).
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
            stopRunnable = Runnable { stopWinMusic() }
            handler.postDelayed(stopRunnable!!, 7000)
        }
    }

    fun stopWinMusic() {
        stopRunnable?.let { handler.removeCallbacks(it) }
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
