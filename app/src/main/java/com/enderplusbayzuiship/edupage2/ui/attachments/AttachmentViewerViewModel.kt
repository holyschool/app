package com.enderplusbayzuiship.edupage2.ui.attachments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.MessageAttachment
import com.enderplusbayzuiship.edupage2.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** How an attachment can be rendered inside the app. */
enum class AttachmentKind { IMAGE, PDF, TEXT, OTHER }

sealed interface AttachmentViewState {
    object Idle : AttachmentViewState
    object Loading : AttachmentViewState
    data class Ready(
        val file: File,
        val kind: AttachmentKind,
        val name: String,
    ) : AttachmentViewState
    data class Error(val message: String) : AttachmentViewState
}

@HiltViewModel
class AttachmentViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
) : ViewModel() {

    companion object { private const val TAG = "AttachmentViewer" }

    private val _state = MutableStateFlow<AttachmentViewState>(AttachmentViewState.Idle)
    val state: StateFlow<AttachmentViewState> = _state.asStateFlow()

    private var loadedUrl: String? = null

    /** Downloads [attachment] (once per URL) and prepares it for in-app preview. */
    fun load(attachment: MessageAttachment) {
        if (loadedUrl == attachment.url && _state.value is AttachmentViewState.Ready) return
        loadedUrl = attachment.url
        viewModelScope.launch {
            _state.value = AttachmentViewState.Loading
            try {
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
                    val safeName = sanitize(attachment.name.ifBlank { "attachment" })
                    edupage.cloudDownload(attachment.url, File(dir, safeName))
                }
                if (file.length() == 0L) {
                    throw RuntimeException("empty file")
                }
                _state.value = AttachmentViewState.Ready(
                    file = file,
                    kind = kindOf(attachment.name, file),
                    name = attachment.name.ifBlank { file.name },
                )
            } catch (e: Exception) {
                Log.e(TAG, "download failed for ${attachment.url}: ${e.message}", e)
                _state.value = AttachmentViewState.Error(
                    context.getString(R.string.attachment_download_failed),
                )
            }
        }
    }

    fun reset() {
        loadedUrl = null
        _state.value = AttachmentViewState.Idle
    }

    /** Hands the downloaded file to an external viewer of the user's choice. */
    fun openExternal() {
        val ready = _state.value as? AttachmentViewState.Ready ?: return
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", ready.file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeOf(ready.name, ready.kind))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "no activity to open attachment", e)
            Toast.makeText(
                context, R.string.attachment_no_app, Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "attachment" }

    private fun kindOf(name: String, file: File): AttachmentKind {
        val ext = (name.substringAfterLast('.', "") .ifBlank { file.extension }).lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> AttachmentKind.IMAGE
            "pdf" -> AttachmentKind.PDF
            "txt", "csv", "json", "xml", "log", "md", "yml", "yaml", "html", "htm",
            "ini", "properties" -> AttachmentKind.TEXT
            else -> AttachmentKind.OTHER
        }
    }

    private fun mimeOf(name: String, kind: AttachmentKind): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (!fromMap.isNullOrBlank()) return fromMap
        return when (kind) {
            AttachmentKind.IMAGE -> "image/*"
            AttachmentKind.PDF -> "application/pdf"
            AttachmentKind.TEXT -> "text/plain"
            AttachmentKind.OTHER -> "*/*"
        }
    }
}
