package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.dbi.DbiHelper
import com.edupage.api.exceptions.FailedToParseGradeDataException
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.Term
import com.edupage.api.model.people.EduTeacher
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Provides grades fetching functionality.
 * Mirrors Python's Grades class.
 */
internal class Grades(private val session: EdupageSession) {

    private val dbi = DbiHelper(session)
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun parseGradeData(html: String): JsonObject {
        return try {
            val marker = ".znamkyStudentViewer("
            val jsonStr = html.substringAfter(marker)
                .substringBefore(");\r\n\t\t});\r\n\t\t</script>")
                .also { if (it == html) throw IllegalStateException("Marker not found") }
            JsonParser.parseString(jsonStr).asJsonObject
        } catch (e: Exception) {
            throw FailedToParseGradeDataException("Failed to parse grade data: ${e.message}")
        }
    }

    private suspend fun fetchGradeData(): JsonObject {
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/znamky/"
            val response = session.httpClient.newCall(Request.Builder().url(url).get().build()).execute()
            val html = response.body?.string() ?: throw FailedToParseGradeDataException("Empty response")
            parseGradeData(html)
        }
    }

    private suspend fun fetchGradeDataForTerm(term: Term, year: Int): JsonObject {
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/znamky/?what=studentviewer&znamky_yearid=$year&nadobdobie=${term.value}"
            val response = session.httpClient.newCall(Request.Builder().url(url).post("".toRequestBody()).build()).execute()
            val html = response.body?.string() ?: throw FailedToParseGradeDataException("Empty response")
            parseGradeData(html)
        }
    }

    suspend fun getGrades(term: Term? = null, year: Int? = null): List<EduGrade> {
        if (!session.isLoggedIn) throw NotLoggedInException()

        val gradeData = if (term != null && year != null) {
            fetchGradeDataForTerm(term, year)
        } else {
            fetchGradeData()
        }

        val grades = gradeData.getAsJsonArray("vsetkyZnamky") ?: return emptyList()
        val gradeDetails = gradeData.getAsJsonObject("vsetkyUdalosti")
            ?.getAsJsonObject("edupage") ?: return emptyList()

        val output = mutableListOf<EduGrade>()
        for (gradeElem in grades) {
            val grade = gradeElem.asJsonObject

            val eventIdStr = grade.get("udalostid")?.asString ?: continue
            val eventId = eventIdStr.toIntOrNull() ?: continue

            val details = gradeDetails.getAsJsonObject(eventIdStr) ?: continue
            val title = details.get("p_meno")?.asString ?: ""

            val dateStr = grade.get("datum")?.asString ?: continue
            val date = try { LocalDateTime.parse(dateStr, dateFmt) } catch (e: Exception) { continue }

            val subjectIdStr = details.get("PredmetID")?.asString
            if (subjectIdStr == null || subjectIdStr == "vsetky") continue
            val subjectId = subjectIdStr.toIntOrNull() ?: continue
            val subjectName = dbi.fetchSubjectName(subjectId)

            // Teacher
            val teacher: EduTeacher? = details.get("UcitelID")?.asString?.toIntOrNull()?.let { teacherId ->
                val teacherData = dbi.fetchTeacherData(teacherId) ?: return@let null
                val teacherName = dbi.fetchTeacherName(teacherId) ?: return@let null
                val gender = com.edupage.api.model.people.Gender.parse(teacherData.get("gender")?.asString)
                val classroomId = teacherData.get("classroomid")?.asString
                val classroomName = dbi.fetchClassroomNumber(classroomId)
                EduTeacher(teacherId, teacherName, gender, null, classroomName, null)
            }

            val gradeType = details.get("p_typ_udalosti")?.asString
            var maxPoints: Double? = null
            var importance: Double? = null
            when (gradeType) {
                "1" -> importance = details.get("p_vaha")?.asDouble?.div(20)
                "2" -> maxPoints = details.get("p_vaha")?.asDouble
                "3" -> {
                    maxPoints = details.get("p_vaha_body")?.asDouble
                    importance = details.get("p_vaha")?.asDouble?.div(20)
                }
            }

            val moreDetailsRaw = details.get("moredata")
            val moreDetails: List<String>? = when {
                moreDetailsRaw == null || moreDetailsRaw.isJsonNull -> null
                moreDetailsRaw.isJsonArray -> moreDetailsRaw.asJsonArray.map { it.asString }
                else -> listOf(moreDetailsRaw.asString)
            }

            val gradeRaw = grade.get("data")?.asString?.split(" (", limit = 2) ?: continue
            val gradeN: Any? = if (gradeRaw[0].first().isDigit()) {
                gradeRaw[0].toDoubleOrNull() ?: gradeRaw[0]
            } else gradeRaw[0]

            val comment = if (gradeRaw.size > 1) gradeRaw[1].dropLast(1) else null

            var verbal = false
            var percent: Double? = null
            try {
                val gradeDouble = when (gradeN) {
                    is Double -> gradeN
                    is String -> gradeN.toDouble()
                    else -> null
                }
                if (gradeDouble != null) {
                    percent = when {
                        maxPoints != null && maxPoints > 0 ->
                            Math.round(gradeDouble / maxPoints * 100 * 100).toDouble() / 100
                        maxPoints == 0.0 -> Double.POSITIVE_INFINITY
                        else -> null
                    }
                }
            } catch (e: Exception) {
                verbal = true
            }

            val classGradeAvg = details.get("priemer")?.asString?.toDoubleOrNull()

            output.add(
                EduGrade(
                    eventId = eventId,
                    title = title,
                    gradeN = gradeN,
                    comment = comment,
                    date = date,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    teacher = teacher,
                    maxPoints = maxPoints,
                    moreDetails = moreDetails,
                    importance = importance,
                    verbal = verbal,
                    percent = percent,
                    classGradeAvg = classGradeAvg
                )
            )
        }
        return output
    }
}
