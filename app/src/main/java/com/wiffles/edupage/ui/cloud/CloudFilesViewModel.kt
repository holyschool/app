package com.wiffles.edupage.ui.cloud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.EduCloudFile
import com.wiffles.edupage.R
import com.wiffles.edupage.data.CloudFileStore
import com.wiffles.edupage.data.SessionRepository
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface CloudUiState {
    object Loading : CloudUiState
    data class Error(val message: String) : CloudUiState
    data class Success(
        val files: List<EduCloudFile>,
        val isRefreshing: Boolean = false,
        val deletingId: String? = null,
        val uploading: Boolean = false,
        val openingId: String? = null,
    ) : CloudUiState
}

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val sessionRepository: SessionRepository,
    private val cloudFileStore: CloudFileStore,
) : ViewModel() {

    companion object { private const val TAG = "CloudFilesViewModel" }

    private val _uiState = MutableStateFlow<CloudUiState>(CloudUiState.Loading)
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val local = cloudFileStore.current()
            val cur = _uiState.value
            if (cur is CloudUiState.Success) {
                _uiState.value = cur.copy(isRefreshing = true)
            } else {
                _uiState.value = CloudUiState.Success(local, isRefreshing = true)
            }
            try {
                val server = try {
                    edupage.cloudList()
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.cloudList()
                }
                val files = merge(local, server)
                _uiState.value = CloudUiState.Success(files, isRefreshing = false)
                Log.i(TAG, "cloud has ${files.size} files (${local.size} local)")
            } catch (e: Exception) {
                Log.e(TAG, "failed to list cloud files: ${e.message}", e)
                val latest = _uiState.value as? CloudUiState.Success
                if (latest != null && latest.files.isNotEmpty()) {
                    // Keep showing locally remembered uploads.
                    _uiState.value = latest.copy(isRefreshing = false)
                } else {
                    _uiState.value = CloudUiState.Error(
                        if (e.isNetworkError()) context.getString(R.string.network_error)
                        else e.message ?: context.getString(R.string.cloud_error_loading)
                    )
                }
            }
        }
    }

    /** Local uploads first, then any server-listed files, de-duplicated by identity. */
    private fun merge(local: List<EduCloudFile>, server: List<EduCloudFile>): List<EduCloudFile> {
        val seen = HashSet<String>()
        return (local + server).filter { seen.add(CloudFileStore.keyOf(it)) }
    }

    fun delete(file: EduCloudFile) {
        viewModelScope.launch {
            val cur = _uiState.value as? CloudUiState.Success ?: return@launch
            val key = CloudFileStore.keyOf(file)
            _uiState.value = cur.copy(deletingId = key)

            // Remembered uploads only live locally, so always drop them from the list;
            // the server call is best-effort (the listing endpoint doesn't track them).
            cloudFileStore.remove(file)

            if (file.fileId.isNotBlank()) {
                try {
                    val ok = try {
                        edupage.cloudDelete(file.fileId)
                    } catch (e: NotLoggedInException) {
                        Log.w(TAG, "session expired, re-authenticating and retrying once")
                        sessionRepository.ensureValidSession()
                        edupage.cloudDelete(file.fileId)
                    }
                    if (!ok) Log.w(TAG, "cloud delete returned false for ${file.fileId}")
                } catch (e: Exception) {
                    Log.e(TAG, "cloud delete failed: ${e.message}", e)
                }
            }

            val latest = _uiState.value as? CloudUiState.Success ?: cur
            _uiState.value = latest.copy(
                files = latest.files.filterNot { CloudFileStore.keyOf(it) == key },
                deletingId = null,
            )
        }
    }

    fun upload(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            val cur = _uiState.value as? CloudUiState.Success ?: return@launch
            _uiState.value = cur.copy(uploading = true)
            try {
                val name = displayName?.takeIf { it.isNotBlank() } ?: "upload"
                val temp = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "cloud_upload").apply { mkdirs() }
                    val file = File(dir, name)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw RuntimeException("Could not read the selected file")
                    file
                }
                val uploaded = edupage.cloudUpload(temp)
                temp.delete()
                cloudFileStore.add(uploaded)
                val latest = _uiState.value as? CloudUiState.Success ?: cur
                _uiState.value = latest.copy(
                    files = merge(cloudFileStore.current(), latest.files),
                    uploading = false,
                )
                _messages.tryEmit(R.string.cloud_upload_success)
            } catch (e: Exception) {
                Log.e(TAG, "cloud upload failed: ${e.message}", e)
                val latest = _uiState.value as? CloudUiState.Success ?: cur
                _uiState.value = latest.copy(uploading = false)
                _messages.tryEmit(R.string.cloud_upload_failed)
            }
        }
    }

    fun copyLink(file: EduCloudFile) {
        if (file.uploadPath.isBlank()) return
        val link = edupage.cloudFileLink(file.uploadPath)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(file.fileName, link))
        _messages.tryEmit(R.string.cloud_link_copied)
    }

    fun open(file: EduCloudFile) {
        viewModelScope.launch {
            val cur = _uiState.value as? CloudUiState.Success ?: return@launch
            _uiState.value = cur.copy(openingId = file.fileId)
            try {
                val downloaded = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "cloud").apply { mkdirs() }
                    val safeName = file.fileName.ifBlank { file.fileId }.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    edupage.cloudDownload(file.uploadPath, File(dir, safeName))
                }
                launchFile(downloaded)
            } catch (e: Exception) {
                Log.e(TAG, "cloud open failed: ${e.message}", e)
                _messages.tryEmit(R.string.cloud_open_failed)
            } finally {
                val latest = _uiState.value as? CloudUiState.Success
                if (latest != null) _uiState.value = latest.copy(openingId = null)
            }
        }
    }

    private fun launchFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase())
            ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "no activity to open file", e)
            _messages.tryEmit(R.string.cloud_no_app)
        }
    }
}
