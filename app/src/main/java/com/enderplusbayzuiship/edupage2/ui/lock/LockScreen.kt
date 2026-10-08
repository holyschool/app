package com.enderplusbayzuiship.edupage2.ui.lock

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.util.BiometricHelper
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LockScreen(
    viewModel: LockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()

    val biometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val biometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    BackHandler {
        (context as? Activity)?.finish()
    }

    fun triggerBiometric() {
        if (activity != null && biometricEnabled && biometricAvailable) {
            BiometricHelper.showPrompt(
                activity = activity,
                title = context.getString(R.string.lock_biometric_prompt_title),
                subtitle = context.getString(R.string.lock_biometric_prompt_subtitle),
                negativeButtonText = context.getString(R.string.lock_biometric_prompt_cancel),
                onSuccess = {
                    haptics.confirm()
                    viewModel.unlockBiometric()
                },
                onError = {

                }
            )
        }
    }

    LaunchedEffect(biometricEnabled, biometricAvailable) {
        if (biometricEnabled && biometricAvailable) {
            triggerBiometric()
        }
    }

    fun verifyPinAttempt(candidate: String) {
        val ok = viewModel.unlock(candidate)
        if (ok) {
            haptics.confirm()
            error = false
        } else {
            haptics.reject()
            error = true
            pin = ""
            scope.launch {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        -24f at 50
                        24f at 100
                        -18f at 150
                        18f at 200
                        -10f at 250
                        10f at 300
                        0f at 400
                    }
                )
            }
        }
    }

    fun onDigit(d: String) {
        if (pin.length < 8) {
            haptics.virtualKey()
            error = false
            val newPin = pin + d
            pin = newPin
            if (newPin.length >= 4) {
                if (viewModel.unlock(newPin)) {
                    haptics.confirm()
                    return
                }
                if (newPin.length == 8) {
                    verifyPinAttempt(newPin)
                }
            }
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) {
            haptics.virtualKey()
            pin = pin.dropLast(1)
            error = false
        }
    }

    fun onClear() {
        if (pin.isNotEmpty()) {
            haptics.virtualKey()
            pin = ""
            error = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(x = shakeOffset.value.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.lock_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (error) {
                    Text(
                        text = stringResource(R.string.lock_wrong_pin),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_lock_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = maxOf(4, pin.length)
                    for (i in 0 until dotCount) {
                        val isFilled = i < pin.length
                        val dotColor = when {
                            error -> MaterialTheme.colorScheme.error
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isFilled) 16.dp else 12.dp)
                                .background(color = dotColor, shape = CircleShape)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    KeypadNumberButton("1") { onDigit("1") }
                    KeypadNumberButton("2") { onDigit("2") }
                    KeypadNumberButton("3") { onDigit("3") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    KeypadNumberButton("4") { onDigit("4") }
                    KeypadNumberButton("5") { onDigit("5") }
                    KeypadNumberButton("6") { onDigit("6") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    KeypadNumberButton("7") { onDigit("7") }
                    KeypadNumberButton("8") { onDigit("8") }
                    KeypadNumberButton("9") { onDigit("9") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (biometricEnabled && biometricAvailable) {
                        KeypadIconButton(
                            icon = Icons.Rounded.Fingerprint,
                            contentDescription = stringResource(R.string.settings_lock_biometrics),
                            onClick = { triggerBiometric() }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    KeypadNumberButton("0") { onDigit("0") }

                    KeypadIconButton(
                        icon = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = stringResource(R.string.homework_delete),
                        onClick = { onBackspace() },
                        onLongClick = { onClear() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun KeypadNumberButton(
    digit: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

