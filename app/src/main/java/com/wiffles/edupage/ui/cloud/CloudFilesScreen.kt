package com.wiffles.edupage.ui.cloud

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.EduCloudFile
import com.wiffles.edupage.R
import com.wiffles.edupage.ui.core.containers.RoundedCardContainer
import com.wiffles.edupage.ui.util.ShimmerBox
import com.wiffles.edupage.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudFilesScreen(
    onBack: () -> Unit,
    viewModel: CloudFilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmDelete by remember { mutableStateOf<EduCloudFile?>(null) }
    val haptics = rememberAppHaptics()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.upload(uri, queryFileName(context, uri))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { res ->
            android.widget.Toast.makeText(context, context.getString(res), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cloud_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); picker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Rounded.Upload, contentDescription = stringResource(R.string.cloud_upload_action))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cloud_refresh))
                    }
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is CloudUiState.Loading -> Skeleton(Modifier.fillMaxSize().padding(paddingValues))
            is CloudUiState.Error -> ErrorState(state.message, { viewModel.refresh() }, Modifier.fillMaxSize().padding(paddingValues))
            is CloudUiState.Success -> {
                if (state.files.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Cloud, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.cloud_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (state.uploading) {
                            item(key = "cloud-uploading") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.cloud_upload), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item(key = "cloud-files-${state.files.size}") {
                            RoundedCardContainer {
                                state.files.forEach { file ->
                                    key(file.fileId) {
                                        CloudFileRow(
                                            file = file,
                                            deleting = state.deletingId == file.fileId,
                                            opening = state.openingId == file.fileId,
                                            onOpen = { viewModel.open(file) },
                                            onDelete = { confirmDelete = file }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.cloud_delete_title)) },
            text = { Text(stringResource(R.string.cloud_delete_confirm, file.fileName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(file.fileId)
                    confirmDelete = null
                }) { Text(stringResource(R.string.cloud_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cloud_cancel)) }
            }
        )
    }
}

@Composable
private fun CloudFileRow(
    file: EduCloudFile,
    deleting: Boolean,
    opening: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val busy = deleting || opening
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy) { haptics.virtualKey(); onOpen() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.InsertDriveFile, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(
                text = file.fileName.ifBlank { file.fileId },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when {
                deleting || opening -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                else -> {
                    IconButton(onClick = { haptics.virtualKey(); onDelete() }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.cloud_delete_action),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun queryFileName(context: android.content.Context, uri: android.net.Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberAppHaptics()
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.Warning, contentDescription = null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { haptics.virtualKey(); onRetry() },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.cloud_retry),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun Skeleton(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) {
            Surface(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(Modifier.size(28.dp), height = 28.dp, cornerRadius = 8.dp)
                    ShimmerBox(Modifier.weight(1f), height = 16.dp, cornerRadius = 8.dp)
                }
            }
        }
    }
}

