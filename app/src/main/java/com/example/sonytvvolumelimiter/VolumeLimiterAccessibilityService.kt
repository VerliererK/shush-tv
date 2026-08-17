package com.example.sonytvvolumelimiter

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeLimiterAccessibilityService : AccessibilityService() {
    private lateinit var audioManager: AudioManager
    private lateinit var limiterPreferences: LimiterPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var blockVolumeUpUntilKeyUp = false

    private val volumeCheck = object : Runnable {
        override fun run() {
            enforceLimit()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        limiterPreferences = LimiterPreferences(this)

        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        handler.removeCallbacks(volumeCheck)
        handler.post(volumeCheck)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isLimiterOperational()) return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    blockVolumeUpUntilKeyUp = VolumeLimiterLogic.shouldConsumeVolumeUp(
                        currentVolume = current,
                        configuredLimit = limiterPreferences.maxVolume,
                        systemMax = systemMax,
                    )
                }

                if (blockVolumeUpUntilKeyUp) enforceLimit()

                // Let Android handle volume changes below the limit. This preserves Sony's
                // native long-press/repeat behavior.
                blockVolumeUpUntilKeyUp
            }

            KeyEvent.ACTION_UP -> {
                val wasBlocked = blockVolumeUpUntilKeyUp
                blockVolumeUpUntilKeyUp = false
                wasBlocked
            }

            else -> blockVolumeUpUntilKeyUp
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        handler.removeCallbacks(volumeCheck)
        blockVolumeUpUntilKeyUp = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(volumeCheck)
        blockVolumeUpUntilKeyUp = false
        super.onDestroy()
    }

    private fun enforceLimit() {
        if (!isLimiterOperational()) return
        val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = VolumeLimiterLogic.clampVolume(
            currentVolume = current,
            configuredLimit = limiterPreferences.maxVolume,
            systemMax = systemMax,
        )
        if (target != current) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    private fun isLimiterOperational(): Boolean =
        ::audioManager.isInitialized &&
            ::limiterPreferences.isInitialized &&
            limiterPreferences.limiterEnabled &&
            limiterPreferences.maxVolume != LimiterPreferences.UNSET_VOLUME &&
            !audioManager.isVolumeFixed

    companion object {
        private const val CHECK_INTERVAL_MS = 100L
    }
}
