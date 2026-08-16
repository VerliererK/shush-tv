package com.example.sonytvvolumelimiter

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class PinStore(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(KEY_PIN_HASH) && preferences.contains(KEY_PIN_SALT)

    fun setPin(pin: String) {
        require(isValidPin(pin))
        val salt = ByteArray(SALT_LENGTH).also(SecureRandom()::nextBytes)
        val hash = hashPin(pin, salt)
        preferences.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verify(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val encodedSalt = preferences.getString(KEY_PIN_SALT, null) ?: return false
        val encodedHash = preferences.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(encodedSalt, Base64.NO_WRAP)
        val expectedHash = Base64.decode(encodedHash, Base64.NO_WRAP)
        return MessageDigest.isEqual(expectedHash, hashPin(pin, salt))
    }

    companion object {
        private const val FILE_NAME = "volume_limiter_pin"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val SALT_LENGTH = 16

        fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all(Char::isDigit)

        private fun hashPin(pin: String, salt: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray(Charsets.UTF_8))
    }
}
