package com.innova.launcher2kd.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build

class AudioDspSuite(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isSvcEnabled: Boolean = true
    private var baseVolumeLevel: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    private var lastSpeedStep: Int = 0

    init {
        initAudioFx()
    }

    private fun initAudioFx() {
        try {
            // Attach to global audio session (session 0)
            equalizer = Equalizer(0, 0).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, 0).apply {
                enabled = true
                setStrength(600.toShort()) // 60% default
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(0).apply {
                    enabled = true
                    setTargetGain(200) // +2dB subtle loudness
                }
            }
        } catch (e: Exception) {
            // Some car ROMs restrict global AudioSession 0 without system permissions
            e.printStackTrace()
        }
    }

    // 1. Preset 1: Innova Diesel Clarity
    fun applyPresetDieselClarity() {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // in Hz
                when {
                    centerFreq in 1000..4000 -> eq.setBandLevel(i.toShort(), 400.toShort()) // +4dB vocal boost
                    centerFreq < 125 -> eq.setBandLevel(i.toShort(), 200.toShort()) // +2dB warm punch
                    else -> eq.setBandLevel(i.toShort(), 0.toShort())
                }
            }
        }
    }

    // 2. Preset 2: Deep Bass Punch
    fun applyPresetDeepBass() {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000
                when {
                    centerFreq <= 125 -> eq.setBandLevel(i.toShort(), 600.toShort()) // +6dB deep bass
                    centerFreq in 250..500 -> eq.setBandLevel(i.toShort(), (-200).toShort()) // scoop mud
                    centerFreq >= 8000 -> eq.setBandLevel(i.toShort(), 300.toShort()) // crisp highs
                    else -> eq.setBandLevel(i.toShort(), 0.toShort())
                }
            }
        }
        bassBoost?.setStrength(850.toShort())
    }

    // 3. Preset 3: Vocal
    fun applyPresetVocal() {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000
                when {
                    centerFreq in 500..3000 -> eq.setBandLevel(i.toShort(), 500.toShort())
                    centerFreq < 200 -> eq.setBandLevel(i.toShort(), (-300).toShort())
                    else -> eq.setBandLevel(i.toShort(), 100.toShort())
                }
            }
        }
    }

    // 4. Preset 4: Flat
    fun applyPresetFlat() {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                eq.setBandLevel(i.toShort(), 0.toShort())
            }
        }
        bassBoost?.setStrength(0.toShort())
    }

    // 5. Speed-Compensated Volume (SVC)
    fun onSpeedChanged(speedKmH: Int) {
        if (!isSvcEnabled) return

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val speedStep = when {
            speedKmH >= 110 -> 3
            speedKmH >= 80 -> 2
            speedKmH >= 50 -> 1
            else -> 0
        }

        if (speedStep != lastSpeedStep) {
            val delta = speedStep - lastSpeedStep
            lastSpeedStep = speedStep
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val newVol = (currentVol + delta).coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        }
    }

    fun setMasterVolume(progress: Int) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (progress * maxVol) / 15
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun toggleMute(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                0
            )
            !isMuted
        } else {
            false
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
