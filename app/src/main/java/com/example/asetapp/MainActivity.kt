package com.example.asetapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.asetapp.auth.AuthRepository
import com.example.asetapp.auth.AuthResult
import com.example.asetapp.auth.BiometricAuthHelper
import com.example.asetapp.auth.SessionVault
import com.example.asetapp.data.remote.ApiClient
import com.example.asetapp.ui.theme.LoginScreen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var sessionVault: SessionVault
    private lateinit var biometricHelper: BiometricAuthHelper
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionVault = SessionVault(this)
        biometricHelper = BiometricAuthHelper(this)
        val authApi = ApiClient.createAuthApi(sessionVault)
        authRepository = AuthRepository(authApi, sessionVault)

        setContent {
            MaterialTheme {
                var isLoading by remember { mutableStateOf(false) }
                var isAuthenticated by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var showEnableBiometricDialog by remember { mutableStateOf(false) }

                val canUseBiometric = !isLoading &&
                        !isAuthenticated &&
                        authRepository.canUseBiometricLogin(biometricHelper)

                fun triggerBiometric() {
                    errorMessage = null
                    biometricHelper.authenticate(
                        title = "Autentikasi Sidik Jari",
                        subtitle = "Verifikasi sidik jari untuk masuk ke AsetApp",
                        onSuccess = {
                            val token = authRepository.getStoredToken()
                            if (!token.isNullOrBlank()) {
                                isAuthenticated = true
                            } else {
                                errorMessage = "Token tidak ditemukan, silakan login dengan password."
                            }
                        },
                        onError = { msg ->
                            errorMessage = msg
                        }
                    )
                }

                LaunchedEffect(Unit) {
                    if (authRepository.canUseBiometricLogin(biometricHelper)) {
                        triggerBiometric()
                    }
                }

                if (isAuthenticated) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Halo, ${sessionVault.getUserName() ?: "Pengguna"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(
                                    onClick = {
                                        if (!sessionVault.isBiometricEnabled()) {
                                            authRepository.logoutLocal(keepBiometricSetting = false)
                                        }
                                        isAuthenticated = false
                                    }
                                ) {
                                    Text("Keluar")
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                AddAssetScreen()
                            }
                        }
                    }
                } else {
                    LoginScreen(
                        isLoading = isLoading,
                        showBiometricButton = canUseBiometric,
                        errorMessage = errorMessage,
                        showEnableBiometricDialog = showEnableBiometricDialog,
                        onLoginClick = { email, password ->
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Email dan password wajib diisi."
                                return@LoginScreen
                            }
                            isLoading = true
                            errorMessage = null

                            lifecycleScope.launch {
                                val res = authRepository.loginWithPassword(email, password)
                                isLoading = false
                                when (res) {
                                    is AuthResult.Success -> {
                                        if (biometricHelper.canAuthenticate() && !sessionVault.isBiometricEnabled()) {
                                            showEnableBiometricDialog = true
                                        } else {
                                            isAuthenticated = true
                                        }
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = res.message
                                    }
                                }
                            }
                        },
                        onBiometricClick = { triggerBiometric() },
                        onConfirmEnableBiometric = {
                            authRepository.enableBiometricLogin()
                            showEnableBiometricDialog = false
                            isAuthenticated = true
                        },
                        onDismissEnableBiometric = {
                            showEnableBiometricDialog = false
                            isAuthenticated = true
                        }
                    )
                }
            }
        }
    }
}