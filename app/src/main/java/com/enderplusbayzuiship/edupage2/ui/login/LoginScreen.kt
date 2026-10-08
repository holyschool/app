package com.enderplusbayzuiship.edupage2.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.modifiers.BlurStepTransition
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

private enum class LoginStep {
    SCHOOL,
    ACCOUNT,
    VERIFY,
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    prefillUsername: String = "",
    prefillSubdomain: String = "",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()

    var currentStep by remember { mutableStateOf(LoginStep.SCHOOL) }
    var username by remember { mutableStateOf(prefillUsername) }
    var password by remember { mutableStateOf("") }
    var subdomain by remember { mutableStateOf(prefillSubdomain) }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                haptics.confirm()
                onLoginSuccess()
            }
            is LoginUiState.Error -> haptics.reject()
            is LoginUiState.TwoFactorRequired -> {
                haptics.virtualKey()
                currentStep = LoginStep.VERIFY
            }
            else -> Unit
        }
    }

    val isLoading = uiState is LoginUiState.Loading

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        BlurStepTransition(
            targetState = currentStep,
            isForward = { from, to -> to.ordinal > from.ordinal },
        ) { step ->
            when (step) {
                LoginStep.SCHOOL -> SchoolStepContent(
                    subdomain = subdomain,
                    onSubdomainChange = { subdomain = it.trim() },
                    isLoading = isLoading,
                    error = (uiState as? LoginUiState.Error)?.error,
                    onNext = {
                        haptics.virtualKey()
                        if (subdomain.isNotBlank()) {
                            currentStep = LoginStep.ACCOUNT
                        }
                    },
                )
                LoginStep.ACCOUNT -> AccountStepContent(
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = {
                        haptics.uiTick()
                        passwordVisible = it
                    },
                    subdomain = subdomain,
                    isLoading = isLoading,
                    error = (uiState as? LoginUiState.Error)?.error,
                    onBack = {
                        haptics.virtualKey()
                        viewModel.resetState()
                        currentStep = LoginStep.SCHOOL
                    },
                    onSignIn = {
                        haptics.virtualKey()
                        viewModel.login(username, password, subdomain)
                    },
                )
                LoginStep.VERIFY -> VerifyStepContent(
                    otpCode = otpCode,
                    onOtpChange = { otpCode = it.trim() },
                    isLoading = isLoading,
                    error = (uiState as? LoginUiState.Error)?.error,
                    onBack = {
                        haptics.virtualKey()
                        viewModel.resetState()
                        otpCode = ""
                        currentStep = LoginStep.ACCOUNT
                    },
                    onVerify = {
                        haptics.virtualKey()
                        val s = uiState as? LoginUiState.TwoFactorRequired ?: return@VerifyStepContent
                        viewModel.verify2FA(s.twoFactorLogin, otpCode)
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginHero(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp),
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SchoolStepContent(
    subdomain: String,
    onSubdomainChange: (String) -> Unit,
    isLoading: Boolean,
    error: LoginError?,
    onNext: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val fieldShape = RoundedCornerShape(20.dp)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.weight(1f))

            LoginHero(
                icon = Icons.Rounded.School,
                title = stringResource(R.string.login_title),
                subtitle = stringResource(R.string.login_subtitle),
            )

            Spacer(modifier = Modifier.height(24.dp))

            RoundedCardContainer {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedTextField(
                            value = subdomain,
                            onValueChange = onSubdomainChange,
                            label = { Text(stringResource(R.string.login_school_subdomain)) },
                            placeholder = { Text(stringResource(R.string.login_school_placeholder)) },
                            supportingText = {
                                Text(stringResource(R.string.login_school_url_format, subdomain.ifEmpty { "..." }))
                            },
                            singleLine = true,
                            shape = fieldShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onNext()
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            LoginErrorCard(error = error)

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(2.dp))
        }

        LoginBottomBar(
            onBack = null,
            onNext = onNext,
            nextLabel = stringResource(R.string.onboarding_next),
            nextIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            nextEnabled = subdomain.isNotBlank() && !isLoading,
        )
    }
}

@Composable
private fun AccountStepContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    subdomain: String,
    isLoading: Boolean,
    error: LoginError?,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val fieldShape = RoundedCornerShape(20.dp)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.weight(1f))

            LoginHero(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.login_username),
                subtitle = stringResource(
                    R.string.login_school_url_format,
                    subdomain.ifEmpty { "..." },
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            RoundedCardContainer {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = onUsernameChange,
                            label = { Text(stringResource(R.string.login_username)) },
                            singleLine = true,
                            shape = fieldShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = { Text(stringResource(R.string.login_password)) },
                            singleLine = true,
                            shape = fieldShape,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSignIn()
                                },
                            ),
                            trailingIcon = {
                                TextButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                                    Text(
                                        if (passwordVisible) {
                                            stringResource(R.string.login_password_hide)
                                        } else {
                                            stringResource(R.string.login_password_show)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            LoginErrorCard(error = error)

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(2.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                        .size(28.dp),
                    strokeWidth = 3.dp,
                )
            }
        } else {
            LoginBottomBar(
                onBack = onBack,
                onNext = onSignIn,
                nextLabel = stringResource(R.string.login_button_signin),
                nextIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                nextEnabled = username.isNotBlank() && password.isNotBlank(),
            )
        }
    }
}

@Composable
private fun VerifyStepContent(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    error: LoginError?,
    onBack: () -> Unit,
    onVerify: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val fieldShape = RoundedCornerShape(20.dp)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.weight(1f))

            LoginHero(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.login_subtitle_2fa),
                subtitle = stringResource(R.string.login_2fa_message),
            )

            Spacer(modifier = Modifier.height(24.dp))

            RoundedCardContainer {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = onOtpChange,
                            label = { Text(stringResource(R.string.login_verification_code)) },
                            singleLine = true,
                            shape = fieldShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onVerify()
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            LoginErrorCard(error = error)

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(2.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                        .size(28.dp),
                    strokeWidth = 3.dp,
                )
            }
        } else {
            LoginBottomBar(
                onBack = onBack,
                onNext = onVerify,
                nextLabel = stringResource(R.string.login_button_verify),
                nextIcon = Icons.Rounded.Check,
                nextEnabled = otpCode.isNotBlank(),
            )
        }
    }
}

@Composable
private fun LoginErrorCard(error: LoginError?) {
    if (error == null) return
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = when (error) {
                    is LoginError.EmptyFields -> stringResource(R.string.login_error_empty_fields)
                    is LoginError.BadCredentials -> stringResource(R.string.login_error_bad_credentials)
                    is LoginError.Captcha -> stringResource(R.string.login_error_captcha)
                    is LoginError.LoginFailed -> stringResource(R.string.login_error_failed, error.message ?: "")
                    is LoginError.EmptyCode -> stringResource(R.string.login_error_empty_code)
                    is LoginError.VerificationFailed -> stringResource(R.string.login_error_verification_failed, error.message ?: "")
                },
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LoginBottomBar(
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    nextLabel: String,
    nextIcon: ImageVector,
    nextEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Text(
                text = nextLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = nextIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

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

