package com.edupage.api.dbi

import com.edupage.api.EdupageSession
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class DbiHelper(private val session: EdupageSession) {

    private var cachedDbi: JsonObject? = null

    private suspend fun fetchDbi(): JsonObject {
        cachedDbi?.let { return it }
        return withContext(Dispatchers.IO) {

            val result = session.data?.getAsJsonObject("dbi")
                ?: throw IllegalStateException("No 'dbi' key in session data")
            cachedDbi = result
            result
        }
    }

    private suspend fun getDbiData(): JsonObject = fetchDbi()

    suspend fun fetchTeacherName(teacherId: Int): String? {
        val dbi = getDbiData()
        val teachers = dbi.getAsJsonObject("teachers") ?: return null

        val teacher = teachers.getAsJsonObject(teacherId.toString())
            ?: teachers.getAsJsonObject((-teacherId).toString())
            ?: run {
                System.err.println("DBI: fetchTeacherName($teacherId) miss; keys=${teachers.keySet().take(5)}")
                return null
            }
        val firstname = teacher.get("firstname")?.asString ?: ""
        val lastname = teacher.get("lastname")?.asString ?: ""
        return "$firstname $lastname".trim().ifEmpty { null }
    }

    suspend fun fetchStudentName(studentId: Int): String? {
        val dbi = getDbiData()
        val students = dbi.getAsJsonObject("students") ?: return null
        val student = students.getAsJsonObject(studentId.toString())
            ?: students.getAsJsonObject((-studentId).toString())
            ?: return null
        val firstname = student.get("firstname")?.asString ?: ""
        val lastname = student.get("lastname")?.asString ?: ""
        return "$firstname $lastname".trim().ifEmpty { null }
    }

    suspend fun fetchSubjectName(subjectId: Int): String? {
        val dbi = getDbiData()
        val subjects = dbi.getAsJsonObject("subjects") ?: return null
        val subject = subjects.getAsJsonObject(subjectId.toString()) ?: return null
        return subject.get("name")?.asString
    }

    suspend fun fetchSubjectShortName(subjectId: Int): String? {
        val dbi = getDbiData()
        val subjects = dbi.getAsJsonObject("subjects") ?: return null
        val subject = subjects.getAsJsonObject(subjectId.toString()) ?: return null
        return subject.get("short")?.asString
    }

    suspend fun fetchClassroomNumber(classroomId: String?): String? {
        if (classroomId == null) return null
        val dbi = getDbiData()
        val classrooms = dbi.getAsJsonObject("classrooms") ?: return null
        val classroom = classrooms.getAsJsonObject(classroomId) ?: return null
        return classroom.get("name")?.asString ?: classroom.get("short")?.asString
    }

    suspend fun fetchTeacherData(teacherId: Int): JsonObject? {
        val dbi = getDbiData()
        val teachers = dbi.getAsJsonObject("teachers") ?: return null
        return teachers.getAsJsonObject(teacherId.toString())
    }

    suspend fun fetchStudentList(): JsonObject? {
        val dbi = getDbiData()
        return dbi.getAsJsonObject("students")
    }

    suspend fun fetchTeacherList(): JsonObject? {
        val dbi = getDbiData()
        return dbi.getAsJsonObject("teachers")
    }

    suspend fun fetchClassList(): JsonObject? {
        val dbi = getDbiData()
        return dbi.getAsJsonObject("classes")
    }

    suspend fun fetchClassroomList(): JsonObject? {
        val dbi = getDbiData()
        return dbi.getAsJsonObject("classrooms")
    }

    suspend fun fetchSubjectList(): JsonObject? {
        val dbi = getDbiData()
        return dbi.getAsJsonObject("subjects")
    }

    suspend fun fetchAllStudentsFromServer(schoolYear: Int): List<com.google.gson.JsonObject> {
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/rpr/server/maindbi.js?__func=mainDBIAccessor"
            val body = com.google.gson.JsonObject().apply {
                add("__args", com.google.gson.JsonArray().apply {
                    add(com.google.gson.JsonNull.INSTANCE)
                    add(schoolYear)
                    add(JsonObject())
                    add(JsonObject().apply {
                        addProperty("op", "fetch")
                        add("needed_part", JsonObject().apply {
                            add("students", com.google.gson.JsonArray().apply {
                                add("id"); add("classid"); add("short")
                            })
                        })
                    })
                })
                addProperty("__gsh", session.gsecHash)
            }
            val requestBody = body.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext emptyList()
            val json = JsonParser.parseString(responseStr).asJsonObject
            val tables = json.getAsJsonObject("r")?.getAsJsonArray("tables") ?: return@withContext emptyList()
            val dataRows = tables[0]?.asJsonObject?.getAsJsonArray("data_rows") ?: return@withContext emptyList()
            dataRows.map { it.asJsonObject }
        }
    }

    fun invalidateCache() {
        cachedDbi = null
    }
}
