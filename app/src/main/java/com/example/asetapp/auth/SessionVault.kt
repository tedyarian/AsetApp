package com.example.asetapp.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionVault(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "asetapp_secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(token: String, email: String, name: String, role: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_ROLE, role ?: "")
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserName(): String? = prefs.getString(KEY_NAME, null)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun clearSession(keepBiometricSetting: Boolean = true) {
        val biometricState = isBiometricEnabled()
        prefs.edit().clear().apply()
        if (keepBiometricSetting) {
            setBiometricEnabled(biometricState)
        }
    }

    companion object {
        private const val KEY_TOKEN = "sanctum_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}