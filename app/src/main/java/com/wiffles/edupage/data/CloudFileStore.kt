package com.wiffles.edupage.data

import android.content.Context
import com.edupage.api.model.EduCloudFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local registry of files uploaded through the app.
 *
 * EduPage's uploader returns a permanent cloud link but does not expose a
 * discoverable listing endpoint, so uploads are remembered here and merged with
 * whatever the server-side list returns. Newest uploads come first.
 */
@Singleton
class CloudFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "edupage_cloud_files"
        private const val KEY_FILES = "files_json"

        /** Stable identity for a cloud file (falls back to its path). */
        fun keyOf(file: EduCloudFile): String = file.fileId.ifBlank { file.uploadPath }
    }

    private val prefs by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    private val _files = MutableStateFlow(load())
    val files: StateFlow<List<EduCloudFile>> = _files.asStateFlow()

    fun current(): List<EduCloudFile> = _files.value

    fun add(file: EduCloudFile) {
        if (file.uploadPath.isBlank()) return
        val key = keyOf(file)
        val updated = listOf(file) + _files.value.filterNot { keyOf(it) == key }
        _files.value = updated
        save(updated)
    }

    fun remove(file: EduCloudFile) {
        val key = keyOf(file)
        val updated = _files.value.filterNot { keyOf(it) == key }
        _files.value = updated
        save(updated)
    }

    private fun load(): List<EduCloudFile> {
        val raw = prefs.getString(KEY_FILES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<EduCloudFile>>() {}.type
            gson.fromJson<List<EduCloudFile>>(raw, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun save(files: List<EduCloudFile>) {
        runCatching { prefs.edit().putString(KEY_FILES, gson.toJson(files)).apply() }
    }
}