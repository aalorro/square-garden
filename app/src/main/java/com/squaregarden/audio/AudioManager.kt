package com.squaregarden.audio

import android.content.Context
import android.media.MediaPlayer
import com.squaregarden.R
import com.squaregarden.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioManager(private val context: Context) {

    private val settingsRepo = SettingsRepository(context)
    private var soundEnabled = true
    private var scope: CoroutineScope? = null

    // Pre-generate PCM data so playback is instant
    private val tapPcm by lazy { SoundGenerator.generateTap() }
    private val swapPcm by lazy { SoundGenerator.generateSwap() }
    private val losePcm by lazy { SoundGenerator.generateLose() }
    private val starCollectPcm by lazy { SoundGenerator.generateStarCollect() }
    private val lifeRestoredPcm by lazy { SoundGenerator.generateLifeRestored() }
    private val shufflePcm by lazy { SoundGenerator.generateShuffle() }
    private val passthroughPcm by lazy { SoundGenerator.generatePassthrough() }
    private val unfreezePcm by lazy { SoundGenerator.generateUnfreeze() }
    private val scramble1Pcm by lazy { SoundGenerator.generateScramble1() }
    private val scramble2Pcm by lazy { SoundGenerator.generateScramble2() }
    private val scramble3Pcm by lazy { SoundGenerator.generateScramble3() }
    private val scramble4Pcm by lazy { SoundGenerator.generateScramble4() }
    private val scramble5Pcm by lazy { SoundGenerator.generateScramble5() }

    // Procedural brass celebration sounds
    private val win1Pcm by lazy { SoundGenerator.generateWin1Star() }
    private val win2Pcm by lazy { SoundGenerator.generateWin2Star() }
    private val win3Pcm by lazy { SoundGenerator.generateWin3Star() }
    private val perfectGamePcm by lazy { SoundGenerator.generatePerfectGame() }
    // Sampled world unlock sound
    private val worldUnlockPcm by lazy { SoundGenerator.loadRawResource(context, R.raw.unlock_sample) }

    // Congratulatory sounds for goal completion (played at random via MediaPlayer)
    private val congratsSounds = listOf(
        R.raw.congrats_1, R.raw.congrats_2, R.raw.congrats_3,
        R.raw.congrats_4, R.raw.congrats_5
    )

    fun observeSettings(scope: CoroutineScope) {
        this.scope = scope
        scope.launch {
            settingsRepo.soundEnabled.collect { soundEnabled = it }
        }
        // Pre-generate celebration sounds on background thread so they
        // don't block the main thread on first play (brass synthesis is heavy)
        scope.launch(Dispatchers.Default) {
            win1Pcm; win2Pcm; win3Pcm; perfectGamePcm
        }
    }

    private fun play(pcm: ShortArray, volume: Float = 0.7f) {
        if (!soundEnabled) return
        scope?.launch {
            SoundGenerator.playPcm(pcm, volume)
        }
    }

    fun playTap() = play(tapPcm, 0.5f)
    fun playSwap() = play(swapPcm, 0.7f)
    fun playMatch() {
        if (!soundEnabled) return
        val resId = congratsSounds.random()
        try {
            MediaPlayer.create(context, resId)?.apply {
                setVolume(0.8f, 0.8f)
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Exception) {}
    }
    fun playWin(stars: Int = 1) {
        val pcm = when (stars) {
            3 -> win3Pcm
            2 -> win2Pcm
            else -> win1Pcm
        }
        play(pcm, 1f)
    }
    fun playLose() = play(losePcm, 0.8f)
    fun playStarCollect() = play(starCollectPcm, 0.6f)
    fun playLifeRestored() = play(lifeRestoredPcm, 1f)
    fun playShuffle() = play(shufflePcm, 0.7f)
    fun playPassthrough() = play(passthroughPcm, 0.7f)
    fun playUnfreeze() = play(unfreezePcm, 0.8f)
    fun playScramble() {
        val pcm = when ((1..5).random()) {
            1 -> scramble1Pcm
            2 -> scramble2Pcm
            3 -> scramble3Pcm
            4 -> scramble4Pcm
            else -> scramble5Pcm
        }
        play(pcm, 0.7f)
    }
    fun playPerfectGame() = play(perfectGamePcm, 1f)
    fun playWorldUnlock() = play(worldUnlockPcm, 0.9f)

    fun release() {
        MusicManager.stopWinMusic()
    }
}
