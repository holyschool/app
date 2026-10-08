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
    val sourceTimelineId: Int? = null,
    val sourceLabel: String? = null,
    val iconKey: String? = null,
    val colorArgb: Int? = null,
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

    private val importableTypes = setOf(
        "hw", "homework", "h_homework",
        "test", "bexam", "oexam", "sexam", "rexam", "pexam",
        "testing", "testpridelenie",
    )

    fun importFromMessages(events: List<com.edupage.api.model.TimelineEvent>): Int {
        val knownIds = items.mapNotNull { it.sourceTimelineId }.toSet()
        var added = 0
        for (event in events) {
            val type = event.type?.lowercase() ?: continue
            if (type !in importableTypes) continue
            if (event.reactionTo != null && event.reactionTo != 0) continue
            if (event.timelineId in knownIds) continue
            val rawTitle = event.text?.ifBlank { null } ?: event.title ?: continue
            val title = rawTitle
                .replace(Regex("<[^>]*>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim()
                .take(140)
            if (title.isBlank()) continue
            val date = try {
                event.timestamp?.toLocalDate()?.toString()
            } catch (_: Exception) {
                null
            } ?: java.time.LocalDate.now().toString()
            val kindLabel = if (type in setOf("hw", "homework", "h_homework")) "Homework" else "Test"
            val sourceLabel = buildString {
                append(kindLabel)
                event.authorName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
            items.add(
                HomeworkItem(
                    title = title,
                    date = date,
                    sourceTimelineId = event.timelineId,
                    sourceLabel = sourceLabel,
                )
            )
            added++
        }
        if (added > 0) persist()
        return added
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

