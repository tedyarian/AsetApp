package com.example.asetapp.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private object LoginPalette {
    val TopGradStart = Color(0xFF0F172A)
    val TopGradMid = Color(0xFF1E3A8A)
    val TopGradEnd = Color(0xFF0F766E)
    val ScreenBg = Color(0xFFF1F5F9)
    val CardBg = Color(0xFFFFFFFF)
    val AccentBlue = Color(0xFF2563EB)
    val TealAccent = Color(0xFF0F766E)
    val SoftTealBg = Color(0xFFF0FDFA)
    val SoftTealBorder = Color(0xFF99F6E4)
    val ErrorBg = Color(0xFFFEE2E2)
    val ErrorBorder = Color(0xFFF87171)
    val ErrorText = Color(0xFF7F1D1D)
    val TextDark = Color(0xFF0F172A)
    val TextMuted = Color(0xFF64748B)
    val BorderLight = Color(0xFFE2E8F0)
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    showBiometricButton: Boolean,
    errorMessage: String?,
    showEnableBiometricDialog: Boolean,
    onLoginClick: (email: String, password: String) -> Unit,
    onBiometricClick: () -> Unit,
    onConfirmEnableBiometric: () -> Unit,
    onDismissEnableBiometric: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Animasi masuk bertahap (staggered entrance)
    var showHeader by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeader = true
        delay(120)
        showCard = true
    }

    // Animasi ambient floating orb di latar belakang
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_motion")
    val orbOffsetY by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_offset_y"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "biometric_pulse"
    )

    // Animasi getar (shake) saat errorMessage muncul/berubah
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var shakeOffsetX by remember { mutableStateOf(0f) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            shakeTrigger++
            val steps = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            for (step in steps) {
                shakeOffsetX = step
                delay(45)
            }
        } else {
            shakeOffsetX = 0f
        }
    }

    val animatedShakeX by animateFloatAsState(
        targetValue = shakeOffsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "shake_x"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isLoading) 0.98f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "button_scale"
    )

    if (showEnableBiometricDialog) {
        AlertDialog(
            onDismissRequest = onDismissEnableBiometric,
            containerColor = LoginPalette.CardBg,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LoginPalette.SoftTealBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = LoginPalette.TealAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Aktifkan Login Sidik Jari?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LoginPalette.TextDark,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Token sesi Anda tersimpan terenkripsi. Gunakan sidik jari agar masuk lebih cepat dan aman tanpa mengetik ulang password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LoginPalette.TextMuted,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmEnableBiometric,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginPalette.TealAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Aktifkan", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEnableBiometric) {
                    Text("Nanti Saja", color = LoginPalette.TextMuted)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginPalette.ScreenBg)
    ) {
        // Banner gradien atas dengan aksen lingkaran mengambang
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            LoginPalette.TopGradStart,
                            LoginPalette.TopGradMid,
                            LoginPalette.TopGradEnd
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .offset(x = (-36).dp, y = (20 + orbOffsetY).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = (55 - orbOffsetY).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2DD4BF).copy(alpha = 0.14f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Brand
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(tween(450)) + slideInVerticallySmooth(initialOffsetY = 24)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp, bottom = 22.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "AsetApp BMN",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF99F6E4),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sanctum Token • Enkripsi & Biometrik",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            // Kartu Form Login
            AnimatedVisibility(
                visible = showCard,
                enter = fadeIn(tween(500)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = LoginPalette.CardBg,
                    border = BorderStroke(1.dp, LoginPalette.BorderLight),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = animatedShakeX }
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Masuk ke Akun",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LoginPalette.TextDark
                            )
                            Text(
                                text = "Gunakan kredensial akun Anda untuk mengelola aset.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoginPalette.TextMuted
                            )
                        }

                        // Pesan error dengan animasi expand/collapse
                        AnimatedVisibility(
                            visible = !errorMessage.isNullOrBlank(),
                            enter = fadeIn(tween(220)) + expandVertically(),
                            exit = fadeOut(tween(180)) + shrinkVertically()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = LoginPalette.ErrorBg,
                                border = BorderStroke(1.dp, LoginPalette.ErrorBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = LoginPalette.ErrorText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = LoginPalette.ErrorText
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            placeholder = { Text("nama@email.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = LoginPalette.AccentBlue
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoginPalette.AccentBlue,
                                unfocusedBorderColor = LoginPalette.BorderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            placeholder = { Text("Masukkan password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LoginPalette.AccentBlue
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    enabled = !isLoading
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Sembunyikan password"
                                        } else {
                                            "Tampilkan password"
                                        },
                                        tint = LoginPalette.TextMuted
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onLoginClick(email, password)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoginPalette.AccentBlue,
                                unfocusedBorderColor = LoginPalette.BorderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Tombol Masuk dengan animasi status loading
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onLoginClick(email, password)
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginPalette.AccentBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(buttonScale)
                        ) {
                            AnimatedContent(
                                targetState = isLoading,
                                transitionSpec = {
                                    fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                                },
                                label = "login_btn_state"
                            ) { loading ->
                                if (loading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.2.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Memverifikasi...",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Masuk Sekarang",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Tombol Sidik Jari dengan pulse lembut saat tersedia
                        AnimatedVisibility(
                            visible = showBiometricButton,
                            enter = fadeIn(tween(260)) + expandVertically(),
                            exit = fadeOut(tween(200)) + shrinkVertically()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    onBiometricClick()
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 13.dp),
                                border = BorderStroke(1.2.dp, LoginPalette.SoftTealBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = LoginPalette.SoftTealBg,
                                    contentColor = LoginPalette.TealAccent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(if (isLoading) 1f else pulseScale)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Masuk dengan Sidik Jari",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sesi dilindungi EncryptedSharedPreferences",
                style = MaterialTheme.typography.labelSmall,
                color = LoginPalette.TextMuted,
                modifier = Modifier.alpha(if (showCard) 0.9f else 0f)
            )
        }
    }
}

private fun slideInVerticallySmooth(initialOffsetY: Int) =
    androidx.compose.animation.slideInVertically(
        initialOffsetY = { initialOffsetY },
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
    )