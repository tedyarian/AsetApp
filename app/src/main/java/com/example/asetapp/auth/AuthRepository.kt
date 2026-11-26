package com.example.asetapp.auth

import com.example.asetapp.data.remote.AuthApiService
import com.example.asetapp.data.remote.LoginRequest
import com.example.asetapp.data.remote.UserDto

sealed class AuthResult {
    data class Success(val user: UserDto, val token: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val api: AuthApiService,
    private val sessionVault: SessionVault
) {
    suspend fun loginWithPassword(email: String, password: String): AuthResult {
        return try {
            val response = api.login(LoginRequest(email.trim(), password))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && !body.token.isNullOrBlank() && body.user != null) {
                sessionVault.saveSession(
                    token = body.token,
                    email = body.user.email,
                    name = body.user.name,
                    role = body.user.role
                )
                AuthResult.Success(body.user, body.token)
            } else {
                AuthResult.Error(body?.message ?: "Login gagal (${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Gagal terhubung ke server")
        }
    }

    fun enableBiometricLogin() {
        sessionVault.setBiometricEnabled(true)
    }

    fun canUseBiometricLogin(biometricHelper: BiometricAuthHelper): Boolean {
        return sessionVault.isBiometricEnabled() &&
                !sessionVault.getToken().isNullOrBlank() &&
                biometricHelper.canAuthenticate()
    }

    fun getStoredToken(): String? = sessionVault.getToken()

    fun logoutLocal(keepBiometricSetting: Boolean = true) {
        sessionVault.clearSession(keepBiometricSetting = keepBiometricSetting)
    }
}