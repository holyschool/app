package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import android.util.Log
import com.edupage.api.model.MessageAttachment
import com.edupage.api.model.TimelineEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private data class CachedAttachment(
    val url: String?,
    val name: String?,
)

private data class CachedTimelineEvent(
    val timelineId: Int,
    val type: String?,
    val timestampIso: String?,
    val authorId: String?,
    val authorName: String?,
    val title: String?,
    val text: String?,
    val reactionTo: Int?,
    val subjectId: Int? = null,
    val attachments: List<CachedAttachment> = emptyList(),
)

private data class TimelineCacheFile(
    val fetchedAtMs: Long,
    val events: List<CachedTimelineEvent>,
)

@Singleton
class TimelineCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "timeline_cache.json"
        private const val TAG = "TimelineCache"
        private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        const val STALE_MS = 60L * 60 * 1_000
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun save(events: List<TimelineEvent>) {
        Log.i(TAG, "saving ${events.size} timeline events")
        val cached = events.map { e ->
            CachedTimelineEvent(
                timelineId   = e.timelineId,
                type         = e.type,
                timestampIso = e.timestamp?.format(DATETIME_FMT),
                authorId     = e.authorId,
                authorName   = e.authorName,
                title        = e.title,
                text         = e.text,
                reactionTo   = e.reactionTo,
                subjectId    = e.subjectId,
                attachments  = e.attachments.map { CachedAttachment(it.url, it.name) },
            )
        }
        val cacheFile = TimelineCacheFile(
            fetchedAtMs = System.currentTimeMillis(),
            events      = cached,
        )
        file.writeText(gson.toJson(cacheFile))
    }

    fun load(): Pair<List<TimelineEvent>, Boolean>? {
        if (!file.exists()) {
            Log.i(TAG, "cache miss: no file")
            return null
        }
        return try {
            val type = object : TypeToken<TimelineCacheFile>() {}.type
            val cf: TimelineCacheFile = gson.fromJson(file.readText(), type)
            val events = cf.events.map { it.toTimelineEvent() }

            val isStale = events.isEmpty() || (System.currentTimeMillis() - cf.fetchedAtMs) > STALE_MS
            Log.i(TAG, "cache hit: ${events.size} events, stale=$isStale")
            events to isStale
        } catch (e: Exception) {
            Log.e(TAG, "failed to read cache: ${e.message}", e)
            null
        }
    }

    fun clear() {
        Log.i(TAG, "clearing timeline cache")
        file.delete()
    }

    private fun CachedTimelineEvent.toTimelineEvent(): TimelineEvent {
        val ts = timestampIso?.let {
            try { LocalDateTime.parse(it, DATETIME_FMT) } catch (_: Exception) { null }
        }
        return TimelineEvent(
            timelineId = timelineId,
            type       = type,
            timestamp  = ts,
            authorId   = authorId,
            authorName = authorName,
            title      = title,
            text       = text,
            reactionTo = reactionTo,
            subjectId  = subjectId,
            attachments = attachments.mapNotNull { att ->
                val url = att.url ?: return@mapNotNull null
                MessageAttachment(url = url, name = att.name ?: "attachment")
            },
        )
    }
}

