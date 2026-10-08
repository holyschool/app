package com.enderplusbayzuiship.edupage2.ui.cloud

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.EduCloudFile
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.SessionRepository
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CloudUiState {
    object Loading : CloudUiState
    data class Error(val message: String) : CloudUiState
    data class Success(
        val files: List<EduCloudFile>,
        val isRefreshing: Boolean = false,
        val deletingId: String? = null,
    ) : CloudUiState
}

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    companion object { private const val TAG = "CloudFilesViewModel" }

    private val _uiState = MutableStateFlow<CloudUiState>(CloudUiState.Loading)
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val cur = _uiState.value
            if (cur is CloudUiState.Success) _uiState.value = cur.copy(isRefreshing = true)
            else _uiState.value = CloudUiState.Loading
            try {
                val files = try {
                    edupage.cloudList()
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.cloudList()
                }
                _uiState.value = CloudUiState.Success(files, isRefreshing = false)
                Log.i(TAG, "cloud has ${files.size} files")
            } catch (e: Exception) {
                Log.e(TAG, "failed to list cloud files: ${e.message}", e)
                val prev = _uiState.value
                if (prev is CloudUiState.Success) {
                    _uiState.value = prev.copy(isRefreshing = false)
                } else {
                    _uiState.value = CloudUiState.Error(
                        if (e.isNetworkError()) context.getString(R.string.network_error)
                        else e.message ?: context.getString(R.string.cloud_error_loading)
                    )
                }
            }
        }
    }

    fun delete(fileId: String) {
        viewModelScope.launch {
            val cur = _uiState.value as? CloudUiState.Success ?: return@launch
            _uiState.value = cur.copy(deletingId = fileId)
            try {
                val ok = try {
                    edupage.cloudDelete(fileId)
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.cloudDelete(fileId)
                }
                val latest = _uiState.value as? CloudUiState.Success ?: cur
                if (ok) {
                    _uiState.value = latest.copy(
                        files = latest.files.filterNot { it.fileId == fileId },
                        deletingId = null,
                    )
                } else {
                    Log.w(TAG, "cloud delete returned false for $fileId")
                    _uiState.value = latest.copy(deletingId = null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "cloud delete failed: ${e.message}", e)
                val latest = _uiState.value as? CloudUiState.Success ?: cur
                _uiState.value = latest.copy(deletingId = null)
            }
        }
    }
}

