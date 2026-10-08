package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

data class HomeworkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String = java.time.LocalDate.now().toString(),
    val subject: String = "",
    val notes: String = "",
    val done: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Singleton
class LocalHomeworkStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "local_homework.json"
        private const val TAG = "LocalHomeworkStore"
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    private val items = CopyOnWriteArrayList<HomeworkItem>().apply {
        loadFromDisk()?.let { addAll(it) }
    }

    init {
        items.sortWith(compareBy<HomeworkItem> { it.done }.then { a, b -> a.date.compareTo(b.date) })
    }

    fun getAll(): List<HomeworkItem> = items.sortedWith(
        compareBy<HomeworkItem> { it.done }.then { a, b -> a.date.compareTo(b.date) }
    )

    fun addOrUpdate(item: HomeworkItem) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = item else items.add(item)
        persist()
    }

    fun remove(id: String) {
        items.removeIf { it.id == id }
        persist()
    }

    fun toggleDone(id: String) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            items[idx] = items[idx].copy(done = !items[idx].done)
            persist()
        }
    }

    fun clear() {
        items.clear()
        persist()
    }

    private fun persist() {
        try {
            file.writeText(gson.toJson(items))
        } catch (e: Exception) {
            Log.e(TAG, "failed to persist homework: ${e.message}", e)
        }
    }

    private fun loadFromDisk(): List<HomeworkItem>? {
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<List<HomeworkItem>>() {}.type
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            Log.e(TAG, "failed to load homework: ${e.message}", e)
            null
        }
    }
}

