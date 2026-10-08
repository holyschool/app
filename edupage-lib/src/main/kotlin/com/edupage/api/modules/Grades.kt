package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.FailedToParseGradeDataException
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.SLOVAK_GRADE_MAP
import com.edupage.api.model.grades.Term
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.people.Gender
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

internal class Grades(private val session: EdupageSession) {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val log = Logger.getLogger("EduGrades")

    private fun JsonElement?.safeDouble(): Double? {
        if (this == null || isJsonNull) return null
        return try { asDouble } catch (_: Exception) { null }
    }

    private fun JsonElement?.safeString(): String? {
        if (this == null || isJsonNull) return null
        return try { asString } catch (_: Exception) { null }
    }

    private fun JsonElement?.safeObject(): JsonObject? {
        if (this == null || isJsonNull) return null
        return try { asJsonObject } catch (_: Exception) { null }
    }

    private fun JsonObject.safeObjectMember(key: String): JsonObject? = get(key).safeObject()

    private fun parseGradeData(html: String): JsonObject {
        return try {
            val marker = ".znamkyStudentViewer("
            val markerIdx = html.indexOf(marker)
            if (markerIdx == -1) {
                log.severe("Marker not found in grades HTML (first 500): ${html.take(500)}")
                throw IllegalStateException("Marker not found")
            }
            val afterMarker = html.substring(markerIdx + marker.length)
            val jsonStr = when {
                afterMarker.contains(");\r\n\t\t});\r\n\t\t</script>") ->
                    afterMarker.substringBefore(");\r\n\t\t});\r\n\t\t</script>")
                afterMarker.contains(");\n\t\t});\n\t\t</script>") ->
                    afterMarker.substringBefore(");\n\t\t});\n\t\t</script>")
                afterMarker.contains(");") ->
                    afterMarker.substringBefore(");")
                else -> throw IllegalStateException("No terminator found in grades HTML")
            }
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
        val gradeDetails = gradeData.safeObjectMember("vsetkyUdalosti")
            ?.safeObjectMember("edupage") ?: return emptyList()

        val subjectMap: Map<Int, String> = buildMap {
            gradeData.get("predmety")
                ?.takeIf { !it.isJsonNull && it.isJsonObject }
                ?.asJsonObject?.entrySet()?.forEach { (key, value) ->
                    val id = key.toIntOrNull() ?: return@forEach
                    val obj = value?.takeIf { !it.isJsonNull }?.asJsonObject ?: return@forEach
                    val name = obj.get("p_meno").safeString()
                        ?: obj.get("p_skratka").safeString()
                        ?: return@forEach
                    put(id, name)
                }
        }

        val teacherMap: Map<Int, EduTeacher> = buildMap {
            gradeData.getAsJsonObject("ucitelia")?.entrySet()?.forEach { (key, value) ->
                val id = key.toIntOrNull() ?: return@forEach
                val obj = value?.takeIf { !it.isJsonNull }?.asJsonObject ?: return@forEach
                val firstname = obj.get("firstname").safeString() ?: ""
                val lastname = obj.get("lastname").safeString() ?: ""
                val fullName = "$firstname $lastname".trim().ifEmpty { return@forEach }
                val gender = Gender.parse(obj.get("gender").safeString())
                put(id, EduTeacher(id, fullName, gender, null, null, null))
            }
        }

        val output = mutableListOf<EduGrade>()
        for (gradeElem in grades) {
            try {
                val grade = gradeElem.asJsonObject

                val eventIdStr = grade.get("udalostid").safeString() ?: continue
                val eventId = eventIdStr.toIntOrNull() ?: continue

                val details = gradeDetails.get(eventIdStr).safeObject() ?: continue
                val title = details.get("p_meno").safeString() ?: ""

                val dateStr = grade.get("datum").safeString() ?: continue
                val date = try { LocalDateTime.parse(dateStr, dateFmt) } catch (_: Exception) { continue }

                val subjectIdStr = grade.get("predmetid").safeString()
                    ?.takeIf { it != "vsetky" }
                    ?: details.get("PredmetID").safeString()
                        ?.takeIf { it != "vsetky" }
                    ?: continue
                val subjectId = subjectIdStr.toIntOrNull() ?: continue
                val subjectName = subjectMap[subjectId]

                val teacher: EduTeacher? = grade.get("ucitelid").safeString()?.toIntOrNull()
                    ?.let { teacherMap[it] }

                val gradeType = details.get("p_typ_udalosti").safeString()
                var maxPoints: Double? = null
                var importance: Double? = null
                when (gradeType) {
                    "1" -> importance = details.get("p_vaha").safeDouble()?.div(20)
                    "2" -> maxPoints = details.get("p_vaha").safeDouble()
                    "3" -> {
                        maxPoints = details.get("p_vaha_body").safeDouble()
                        importance = details.get("p_vaha").safeDouble()?.div(20)
                    }
                }

                val moreDetailsRaw = details.get("moredata")
                val moreDetails: List<String>? = when {
                    moreDetailsRaw == null || moreDetailsRaw.isJsonNull -> null
                    moreDetailsRaw.isJsonArray -> moreDetailsRaw.asJsonArray
                        .filterNot { it.isJsonNull }
                        .map { it.asString }
                    else -> listOf(moreDetailsRaw.asString)
                }

                val dataElem = grade.get("data")
                if (dataElem == null || dataElem.isJsonNull) continue
                val dataStr = dataElem.asString
                if (dataStr.isBlank()) continue

                val splitIdx = dataStr.indexOf(" (")
                val gradeStr = if (splitIdx >= 0) dataStr.substring(0, splitIdx) else dataStr
                val comment = if (splitIdx >= 0 && dataStr.endsWith(")"))
                    dataStr.substring(splitIdx + 2, dataStr.length - 1) else null

                val gradeN: Any? = gradeStr.toDoubleOrNull() ?: gradeStr

                val numericValue = gradeStr.toDoubleOrNull()
                    ?: SLOVAK_GRADE_MAP[gradeStr.lowercase()]
                val verbal = numericValue == null
                val percent: Double? = if (numericValue != null && maxPoints != null && maxPoints > 0)
                    Math.round(numericValue / maxPoints * 100 * 100).toDouble() / 100
                else null

                val classGradeAvg = details.get("priemer").safeString()?.toDoubleOrNull()

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
            } catch (e: Exception) {
                log.warning("Skipping grade entry due to error: ${e.message}")
            }
        }
        return output
    }
}

