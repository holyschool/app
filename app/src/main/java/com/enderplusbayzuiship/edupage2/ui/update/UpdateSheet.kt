package com.enderplusbayzuiship.edupage2.ui.update

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.network.AppUpdateInfo
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableSheet(
    info: AppUpdateInfo,
    downloadProgress: Int?,
    onDownload: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = stringResource(R.string.update_available_title, info.versionName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            if (info.releaseNotes.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.update_release_notes, info.versionName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedCardContainer {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceBright,
                        ) {
                            Text(
                                text = info.releaseNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    haptics.virtualKey()
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        info.pageUrl.toUri(),
                    )
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_view_on_github))
            }

            if (downloadProgress != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(
                        progress = { (downloadProgress ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.update_downloading_progress,
                            downloadProgress ?: 0,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                var menuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                haptics.virtualKey()
                                onDownload()
                            },
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                bottomStart = 20.dp,
                                topEnd = 4.dp,
                                bottomEnd = 4.dp,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.update_install),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = {
                                haptics.virtualKey()
                                menuExpanded = true
                            },
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                                topEnd = 20.dp,
                                bottomEnd = 20.dp,
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.update_more_options),
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(if (menuExpanded) 180f else 0f),
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.update_skip_version, info.versionName)) },
                            onClick = {
                                haptics.virtualKey()
                                menuExpanded = false
                                onSkip()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
