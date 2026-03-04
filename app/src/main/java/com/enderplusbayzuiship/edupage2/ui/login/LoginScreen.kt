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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    prefillUsername: String = "",
    prefillSubdomain: String = "",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var username       by remember { mutableStateOf(prefillUsername) }
    var password       by remember { mutableStateOf("") }
    var subdomain      by remember { mutableStateOf(prefillSubdomain) }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpCode        by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
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
                        text = "EduPage",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isTwoFactor) "Two-factor authentication" else "Sign in to continue",
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
                                    label = { Text("School subdomain") },
                                    placeholder = { Text("e.g. myschool") },
                                    supportingText = {
                                        Text("https://${subdomain.ifEmpty { "..." }}.edupage.org")
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
                                    label = { Text("Username") },
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
                                    label = { Text("Password") },
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
                                            viewModel.login(username, password, subdomain)
                                        }
                                    ),
                                    trailingIcon = {
                                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Text(
                                                if (passwordVisible) "Hide" else "Show",
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
                                    text = "Enter the verification code sent to your device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it.trim() },
                                    label = { Text("Verification code") },
                                    singleLine = true,
                                    shape = fieldShape,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            val s = uiState as? LoginUiState.TwoFactorRequired
                                                ?: return@KeyboardActions
                                            viewModel.verify2FA(s.twoFactorLogin, otpCode)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                TextButton(
                                    onClick = { viewModel.resetState(); otpCode = "" },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Text("Back to login")
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
                                        text = (uiState as? LoginUiState.Error)?.message ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Primary action button
                        Button(
                            onClick = {
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
                                    text = if (isTwoFactor) "Verify" else "Sign In",
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
