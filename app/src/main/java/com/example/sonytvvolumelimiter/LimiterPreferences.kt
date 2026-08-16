package com.example.sonytvvolumelimiter

import android.content.Context

class LimiterPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var maxVolume: Int
        get() = preferences.getInt(KEY_MAX_VOLUME, UNSET_VOLUME)
        set(value) = preferences.edit().putInt(KEY_MAX_VOLUME, value).apply()

    var limiterEnabled: Boolean
        get() = preferences.getBoolean(KEY_LIMITER_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_LIMITER_ENABLED, value).apply()

    companion object {
        const val UNSET_VOLUME = -1
        private const val FILE_NAME = "volume_limiter_preferences"
        private const val KEY_MAX_VOLUME = "max_volume"
        private const val KEY_LIMITER_ENABLED = "limiter_enabled"
    }
}
