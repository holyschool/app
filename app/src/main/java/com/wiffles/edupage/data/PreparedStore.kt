package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreparedStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "prepared_lessons.json"
        private const val TAG = "PreparedStore"
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<Map<String, Set<String>>>(loadFromDisk() ?: emptyMap())
    val state: StateFlow<Map<String, Set<String>>> = _state.asStateFlow()

    fun getDay(date: LocalDate): Set<String> =
        _state.value[date.toString()] ?: emptySet()

    fun mark(date: LocalDate, key: String) {
        val day = date.toString()
        val updated = (_state.value[day] ?: emptySet()) + key
        _state.value = _state.value + (day to updated)
        persist()
    }

    fun clearDay(date: LocalDate) {
        val day = date.toString()
        if (_state.value.containsKey(day)) {
            _state.value = _state.value - day
            persist()
        }
    }

    private fun persist() {
        val snapshot = _state.value
        ioScope.launch {
            try {
                file.writeText(gson.toJson(snapshot))
            } catch (e: Exception) {
                Log.e(TAG, "failed to persist prepared lessons: ${e.message}", e)
            }
        }
    }

    private fun loadFromDisk(): Map<String, Set<String>>? {
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<Map<String, Set<String>>>() {}.type
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            Log.e(TAG, "failed to load prepared lessons: ${e.message}", e)
            null
        }
    }
}

