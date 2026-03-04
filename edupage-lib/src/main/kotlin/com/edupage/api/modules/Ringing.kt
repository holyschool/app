package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.RingingTime
import com.edupage.api.model.RingingType
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Provides ringing time lookups.
 * Mirrors Python's RingingTimes class.
 */
internal class Ringing(private val session: EdupageSession) {

    suspend fun getNextRingingTime(dateTime: LocalDateTime): RingingTime? {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val dp = session.data?.getAsJsonObject("dp") ?: return@withContext null
            val periods = dp.getAsJsonObject("periods") ?: return@withContext null
            val currentTime = dateTime.toLocalTime()

            // Find ringing times from the period definitions
            val ringingTimes = mutableListOf<Pair<LocalTime, RingingType>>()
            for (key in periods.keySet()) {
                val period = periods.getAsJsonObject(key) ?: continue
                val startStr = period.get("starttime")?.asString
                val endStr = period.get("endtime")?.asString
                if (startStr != null) ringingTimes.add(Pair(parseTime(startStr), RingingType.LESSON))
                if (endStr != null) ringingTimes.add(Pair(parseTime(endStr), RingingType.BREAK))
            }

            ringingTimes.sortBy { it.first }
            val next = ringingTimes.firstOrNull { it.first > currentTime }
            next?.let { RingingTime(it.second, it.first) }
        }
    }

    private fun parseTime(value: String): LocalTime {
        val parts = value.split(":")
        return LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }
}
