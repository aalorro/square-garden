package com.squaregarden.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.squaregarden.R
import com.squaregarden.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Singleton that manages background music playback via MediaPlayer.
 * Handles intro (Home) music and win/perfect-game celebration music.
 * Respects the musicEnabled setting — stops all music when toggled off.
 */
object MusicManager {

    private var introPlayer: MediaPlayer? = null
    private var winPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var musicEnabled = true

    /** Call once from Application or Activity to observe music toggle. */
    fun observeSettings(context: Context, scope: CoroutineScope) {
        val settingsRepo = SettingsRepository(context)
        scope.launch {
            settingsRepo.musicEnabled.collect { enabled ->
                musicEnabled = enabled
                if (!enabled) stopAll()
            }
        }
    }

    // 4 distinct segments of the track for regular wins (start position in ms)
    private val winSegments = listOf(0, 32_000, 68_000, 105_000)
    private const val WIN_PLAY_MS = 8000L
    private const val WIN_FADE_MS = 1000L

    // Perfect game gets its own dedicated segment, loops until Next Level
    private const val PERFECT_SEGMENT_START = 120_000

    /** Start looping intro music (HomeScreen). No-op if already playing or music disabled. */
    fun startIntro(context: Context) {
        if (!musicEnabled) return
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
     * Start win celebration music from the track.
     * @param loop true for perfect-game (loops dedicated segment until stopped),
     *             false for regular win (random segment, ~8 sec with 1-sec fade-out).
     */
    fun startWinMusic(context: Context, loop: Boolean) {
        if (!musicEnabled) return
        stopWinMusic()
        try {
            winPlayer = MediaPlayer.create(context, R.raw.perfect_game_music)?.apply {
                setVolume(0.7f, 0.7f)
                if (loop) {
                    // Perfect game: play from dedicated segment, loop back on end
                    seekTo(PERFECT_SEGMENT_START)
                    setOnCompletionListener { mp ->
                        try {
                            mp.seekTo(PERFECT_SEGMENT_START)
                            mp.start()
                        } catch (_: Exception) {}
                    }
                } else {
                    // Regular win: pick a random segment
                    seekTo(winSegments.random())
                }
                start()
            }
        } catch (_: Exception) {}
        if (!loop) {
            handler.postDelayed({ fadeOutWinMusic() }, WIN_PLAY_MS - WIN_FADE_MS)
        }
    }

    private fun fadeOutWinMusic() {
        val player = winPlayer ?: return
        val steps = 20
        val intervalMs = WIN_FADE_MS / steps
        for (i in 1..steps) {
            handler.postDelayed({
                try {
                    val vol = 0.7f * (1f - i.toFloat() / steps)
                    player.setVolume(vol, vol)
                } catch (_: Exception) {}
            }, i * intervalMs)
        }
        handler.postDelayed({ stopWinMusic() }, WIN_FADE_MS + 50)
    }

    fun stopWinMusic() {
        handler.removeCallbacksAndMessages(null)
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
