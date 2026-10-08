package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.grades.Assignment
import com.edupage.api.model.grades.AssignmentType
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class Assignments(private val session: EdupageSession) {

    private val datetimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun JsonObject.str(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull) return null
        return try { el.asString } catch (_: Exception) { null }
    }

    fun parseAssignmentFromItem(item: JsonObject, subjectName: String?): Assignment? {
        val dataStr = item.str("data")
        if (dataStr.isNullOrBlank() || dataStr == "[]") return null

        val dataObj = runCatching { JsonParser.parseString(dataStr).asJsonObject }.getOrNull() ?: return null

        val title = dataObj.str("nazov") ?: item.str("text") ?: return null
        val dueStr = dataObj.str("date") ?: dataObj.str("datetimefrom")
        val due: LocalDateTime? = dueStr?.let {
            runCatching { LocalDateTime.parse(it, datetimeFmt) }.getOrNull()
                ?: runCatching { java.time.LocalDate.parse(it, dateFmt).atStartOfDay() }.getOrNull()
        }
        val dateFromStr = dataObj.str("datetimefrom") ?: dataObj.str("datefrom")
        val dateFrom: LocalDateTime? = dateFromStr?.let {
            runCatching { LocalDateTime.parse(it, datetimeFmt) }.getOrNull()
        }

        return Assignment(
            id = dataObj.str("homeworkid") ?: item.str("homeworkid"),
            superId = dataObj.str("superid") ?: dataObj.str("e_superid") ?: item.str("superid"),
            testId = dataObj.str("testid"),
            type = AssignmentType.parse(dataObj.str("typ") ?: item.str("typ")),
            title = title,
            details = dataObj.str("details"),
            date = dateFrom,
            dateTo = due,
            subjectId = dataObj.str("predmetid"),
            subjectName = subjectName,
            teacherId = dataObj.str("ucitelid"),
            isFinished = dataObj.str("skoncil") == "1",
        )
    }

    suspend fun getAssignmentData(superId: String): JsonObject? = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val url = "https://${session.subdomain}.edupage.org/elearning/?cmd=EtestCreator&akcia=getResultsData"
        val formBody = FormBody.Builder()
            .add("superid", superId)
            .build()
        val request = Request.Builder().url(url).post(formBody).build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext null
        if (responseStr == "0") return@withContext null
        runCatching { JsonParser.parseString(responseStr).asJsonObject }.getOrNull()
    }
}

