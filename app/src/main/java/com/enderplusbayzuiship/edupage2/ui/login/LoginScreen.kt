package com.enderplusbayzuiship.edupage2.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    prefillUsername: String = "",
    prefillSubdomain: String = "",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val haptics = rememberAppHaptics()

    var username       by remember { mutableStateOf(prefillUsername) }
    var password       by remember { mutableStateOf("") }
    var subdomain      by remember { mutableStateOf(prefillSubdomain) }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpCode        by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> { haptics.confirm(); onLoginSuccess() }
            is LoginUiState.Error   -> haptics.reject()
            else                    -> Unit
        }
    }

    val isTwoFactor = uiState is LoginUiState.TwoFactorRequired
    val isLoading   = uiState is LoginUiState.Loading

    val fieldShape = RoundedCornerShape(16.dp)

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simple top spacing + title
                Spacer(modifier = Modifier.height(64.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isTwoFactor)
                            stringResource(R.string.login_subtitle_2fa)
                        else
                            stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedVisibility(
                            visible = !isTwoFactor,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Subdomain field
                                OutlinedTextField(
                                    value = subdomain,
                                    onValueChange = { subdomain = it.trim() },
                                    label = { Text(stringResource(R.string.login_school_subdomain)) },
                                    placeholder = { Text(stringResource(R.string.login_school_placeholder)) },
                                    supportingText = {
                                        Text(stringResource(R.string.login_school_url_format, subdomain.ifEmpty { "..." }))
                                    },
                                    singleLine = true,
                                    shape = fieldShape,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Username field
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text(stringResource(R.string.login_username)) },
                                    singleLine = true,
                                    shape = fieldShape,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Password field
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(stringResource(R.string.login_password)) },
                                    singleLine = true,
                                    shape = fieldShape,
                                    visualTransformation = if (passwordVisible)
                                        VisualTransformation.None
                                    else
                                        PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                                    keyboardActions = KeyboardActions(
                                                        onDone = {
                                                            focusManager.clearFocus()
                                                            haptics.click()
                                                            viewModel.login(username, password, subdomain)
                                                        }
                                                    ),
                                    trailingIcon = {
                                                    TextButton(onClick = { haptics.tick(); passwordVisible = !passwordVisible }) {
                                            Text(
                                                if (passwordVisible)
                                                    stringResource(R.string.login_password_hide)
                                                else
                                                    stringResource(R.string.login_password_show),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isTwoFactor,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.login_2fa_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it.trim() },
                                    label = { Text(stringResource(R.string.login_verification_code)) },
                                    singleLine = true,
                                    shape = fieldShape,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            haptics.click()
                                            val s = uiState as? LoginUiState.TwoFactorRequired
                                                ?: return@KeyboardActions
                                            viewModel.verify2FA(s.twoFactorLogin, otpCode)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                TextButton(
                                    onClick = { haptics.tick(); viewModel.resetState(); otpCode = "" },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text(stringResource(R.string.login_back_to_login))
                                }
                            }
                        }

                        // Error card
                        AnimatedVisibility(
                            visible = uiState is LoginUiState.Error,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = when (val error = (uiState as? LoginUiState.Error)?.error) {
                                            is LoginError.EmptyFields -> stringResource(R.string.login_error_empty_fields)
                                            is LoginError.BadCredentials -> stringResource(R.string.login_error_bad_credentials)
                                            is LoginError.Captcha -> stringResource(R.string.login_error_captcha)
                                            is LoginError.LoginFailed -> stringResource(R.string.login_error_failed, error.message ?: "")
                                            is LoginError.EmptyCode -> stringResource(R.string.login_error_empty_code)
                                            is LoginError.VerificationFailed -> stringResource(R.string.login_error_verification_failed, error.message ?: "")
                                            null -> ""
                                        },
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Primary action button
                        Button(
                            onClick = {
                                haptics.click()
                                if (isTwoFactor) {
                                    val s = uiState as? LoginUiState.TwoFactorRequired ?: return@Button
                                    viewModel.verify2FA(s.twoFactorLogin, otpCode)
                                } else {
                                    viewModel.login(username, password, subdomain)
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (isTwoFactor)
                                        stringResource(R.string.login_button_verify)
                                    else
                                        stringResource(R.string.login_button_signin),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Login – Idle Light", showBackground = true)
@Preview(name = "Login – Idle Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenIdlePreview() {
    Edupage2Theme {
        LoginScreen(onLoginSuccess = {})
    }
}

@Preview(name = "Login – Error", showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    Edupage2Theme {
        // Render only the static shell; error card needs the live VM state so we show the idle form.
        LoginScreen(
            onLoginSuccess = {},
            prefillUsername = "john.doe",
            prefillSubdomain = "myschool"
        )
    }
}

@Preview(name = "Login – 2FA", showBackground = true)
@Composable
private fun LoginScreen2FAPreview() {
    Edupage2Theme {
        LoginScreen(onLoginSuccess = {})
    }
}
