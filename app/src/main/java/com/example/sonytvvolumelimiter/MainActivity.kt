package com.example.sonytvvolumelimiter

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var audioManager: AudioManager
    private lateinit var limiterPreferences: LimiterPreferences
    private lateinit var pinStore: PinStore
    private lateinit var statusText: TextView
    private lateinit var currentVolumeText: TextView
    private lateinit var limitText: TextView
    private lateinit var limitSeekBar: SeekBar
    private lateinit var saveButton: Button
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        limiterPreferences = LimiterPreferences(this)
        pinStore = PinStore(this)

        statusText = findViewById(R.id.statusText)
        currentVolumeText = findViewById(R.id.currentVolumeText)
        limitText = findViewById(R.id.limitText)
        limitSeekBar = findViewById(R.id.limitSeekBar)
        saveButton = findViewById(R.id.saveButton)
        toggleButton = findViewById(R.id.toggleButton)

        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        saveButton.setOnClickListener { requestPinToSaveLimit() }
        toggleButton.setOnClickListener { requestPinToToggleLimiter() }

        configureVolumeControls()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun configureVolumeControls() {
        val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val initialLimit = limiterPreferences.maxVolume
            .takeUnless { it == LimiterPreferences.UNSET_VOLUME }
            ?: current

        limitSeekBar.max = systemMax
        limitSeekBar.progress = VolumeLimiterLogic.normalizeLimit(initialLimit, systemMax)
        updateLimitText(limitSeekBar.progress, systemMax)
        limitSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLimitText(progress, systemMax)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun refreshStatus() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolumeText.text = getString(R.string.current_volume_format, current, systemMax)

        val fixedVolume = audioManager.isVolumeFixed
        val serviceEnabled = isAccessibilityServiceEnabled()
        val limiterEnabled = limiterPreferences.limiterEnabled
        statusText.text = when {
            fixedVolume -> getString(R.string.status_fixed_volume)
            !serviceEnabled -> getString(R.string.status_service_disabled)
            limiterEnabled -> getString(R.string.status_active)
            else -> getString(R.string.status_inactive)
        }
        statusText.setTextColor(
            if (!fixedVolume && serviceEnabled && limiterEnabled) Color.rgb(89, 214, 142)
            else Color.rgb(255, 190, 80),
        )

        limitSeekBar.isEnabled = !fixedVolume
        saveButton.isEnabled = !fixedVolume
        toggleButton.isEnabled = !fixedVolume && limiterPreferences.maxVolume != LimiterPreferences.UNSET_VOLUME
        toggleButton.text = if (limiterEnabled) {
            getString(R.string.disable_limiter)
        } else {
            getString(R.string.enable_limiter)
        }
    }

    private fun requestPinToSaveLimit() {
        val creatingPin = !pinStore.hasPin()
        showPinDialog(
            title = if (creatingPin) R.string.create_pin_title else R.string.enter_pin_title,
            positiveLabel = if (creatingPin) R.string.create_and_save else R.string.save_limit,
        ) { pin ->
            if (creatingPin) {
                if (!PinStore.isValidPin(pin)) {
                    showInvalidPinMessage()
                    return@showPinDialog
                }
                pinStore.setPin(pin)
            } else if (!pinStore.verify(pin)) {
                showInvalidPinMessage()
                return@showPinDialog
            }

            limiterPreferences.maxVolume = limitSeekBar.progress
            limiterPreferences.limiterEnabled = true
            clampCurrentVolume()
            Toast.makeText(this, R.string.limit_saved, Toast.LENGTH_SHORT).show()
            refreshStatus()
        }
    }

    private fun requestPinToToggleLimiter() {
        showPinDialog(
            title = R.string.enter_pin_title,
            positiveLabel = R.string.confirm,
        ) { pin ->
            if (!pinStore.verify(pin)) {
                showInvalidPinMessage()
                return@showPinDialog
            }
            limiterPreferences.limiterEnabled = !limiterPreferences.limiterEnabled
            if (limiterPreferences.limiterEnabled) clampCurrentVolume()
            refreshStatus()
        }
    }

    private fun showPinDialog(title: Int, positiveLabel: Int, onConfirmed: (String) -> Unit) {
        val pinInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.pin_hint)
            maxLines = 1
        }
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding)
        val container = android.widget.FrameLayout(this).apply {
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(
                pinInput,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(positiveLabel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onConfirmed(pinInput.text.toString())
                if (PinStore.isValidPin(pinInput.text.toString()) &&
                    (!pinStore.hasPin() || pinStore.verify(pinInput.text.toString()))
                ) {
                    dialog.dismiss()
                }
            }
            pinInput.requestFocus()
        }
        dialog.show()
    }

    private fun clampCurrentVolume() {
        val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = VolumeLimiterLogic.clampVolume(current, limiterPreferences.maxVolume, systemMax)
        if (target != current) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    private fun updateLimitText(limit: Int, systemMax: Int) {
        limitText.text = getString(R.string.limit_volume_format, limit, systemMax)
    }

    private fun showInvalidPinMessage() {
        Toast.makeText(this, R.string.invalid_pin, Toast.LENGTH_SHORT).show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { serviceInfo ->
                val info = serviceInfo.resolveInfo.serviceInfo
                info.packageName == packageName &&
                    info.name == VolumeLimiterAccessibilityService::class.java.name
            }
    }
}
