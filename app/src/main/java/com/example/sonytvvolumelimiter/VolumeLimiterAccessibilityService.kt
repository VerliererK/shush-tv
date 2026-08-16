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

        if (event.action == KeyEvent.ACTION_DOWN) {
            val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val target = VolumeLimiterLogic.nextVolumeUp(
                currentVolume = current,
                configuredLimit = limiterPreferences.maxVolume,
                systemMax = systemMax,
            )
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                target,
                AudioManager.FLAG_SHOW_UI,
            )
        }

        // Consume both ACTION_DOWN and ACTION_UP to preserve a well-formed event stream.
        return true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        handler.removeCallbacks(volumeCheck)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(volumeCheck)
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
        private const val CHECK_INTERVAL_MS = 500L
    }
}
