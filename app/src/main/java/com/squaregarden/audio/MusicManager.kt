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
    private var introNextPlayer: MediaPlayer? = null
    private var introChainActive = false
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

    // Huge win (perfect game) randomizes between dedicated celebratory clips.
    private val hugeWinClips = listOf(R.raw.huge_win_puzzle, R.raw.huge_win_bitcrush)

    /**
     * Start gapless looping intro music (HomeScreen). No-op if already playing or music disabled.
     * Uses two MediaPlayer instances chained via setNextMediaPlayer() for truly gapless looping —
     * MediaPlayer's built-in isLooping=true inserts a small audible gap between iterations.
     */
    fun startIntro(context: Context) {
        if (!musicEnabled) return
        if (introChainActive) return
        stopIntro()
        val first = createIntroPlayer(context) ?: return
        val second = createIntroPlayer(context) ?: run {
            try { first.release() } catch (_: Exception) {}
            return
        }
        introPlayer = first
        introNextPlayer = second
        try { first.setNextMediaPlayer(second) } catch (_: Exception) {}
        setupIntroChainCompletion(context, first)
        introChainActive = true
        try { first.start() } catch (_: Exception) {}
    }

    private fun createIntroPlayer(context: Context): MediaPlayer? {
        return try {
            MediaPlayer.create(context, R.raw.intro_music)?.apply {
                setVolume(0.5f, 0.5f)
            }
        } catch (_: Exception) { null }
    }

    private fun setupIntroChainCompletion(context: Context, player: MediaPlayer) {
        player.setOnCompletionListener { completed ->
            try { completed.release() } catch (_: Exception) {}
            if (!introChainActive) return@setOnCompletionListener
            // The 'next' player has already begun playing automatically (gapless).
            // Promote it to current, then queue a fresh next behind it.
            val newCurrent = introNextPlayer ?: return@setOnCompletionListener
            introPlayer = newCurrent
            val newNext = createIntroPlayer(context)
            introNextPlayer = newNext
            if (newNext != null) {
                try { newCurrent.setNextMediaPlayer(newNext) } catch (_: Exception) {}
            }
            setupIntroChainCompletion(context, newCurrent)
        }
    }

    fun stopIntro() {
        introChainActive = false
        try {
            introPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        try { introNextPlayer?.release() } catch (_: Exception) {}
        introPlayer = null
        introNextPlayer = null
    }

    /**
     * Start win celebration music.
     * @param perfectGame true for huge-win/perfect-game (random celebratory clip, plays to end),
     *                    false for regular win (random segment of perfect_game_music, ~8 sec with fade-out).
     * @param loop true to loop the music indefinitely until [stopWinMusic] is called (game complete).
     */
    fun startWinMusic(context: Context, perfectGame: Boolean, loop: Boolean = false) {
        if (!musicEnabled) return
        stopWinMusic()
        try {
            if (perfectGame) {
                // Huge win: random celebratory clip, plays from start to end.
                winPlayer = MediaPlayer.create(context, hugeWinClips.random())?.apply {
                    setVolume(0.7f, 0.7f)
                    isLooping = loop
                    if (!loop) setOnCompletionListener { stopWinMusic() }
                    start()
                }
            } else {
                // Regular win: pick a random segment from the existing perfect_game_music track.
                winPlayer = MediaPlayer.create(context, R.raw.perfect_game_music)?.apply {
                    setVolume(0.7f, 0.7f)
                    isLooping = loop
                    seekTo(winSegments.random())
                    start()
                }
            }
        } catch (_: Exception) {}
        if (!perfectGame && !loop) {
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
