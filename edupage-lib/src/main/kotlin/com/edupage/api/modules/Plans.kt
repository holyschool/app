package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.SchoolPlan
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class Plans(private val session: EdupageSession) {

    private fun JsonObject.str(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull) return null
        return try { el.asString } catch (_: Exception) { null }
    }

    private fun JsonObject.int(key: String): Int? = str(key)?.toIntOrNull()

    private fun JsonObject.strList(key: String): List<String> = runCatching {
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.asString } ?: emptyList()
    }.getOrElse { emptyList() }

    suspend fun getPlans(): List<SchoolPlan> = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val dbi = session.data?.getAsJsonObject("dbi") ?: return@withContext emptyList()
        val plans = dbi.getAsJsonObject("plans") ?: return@withContext emptyList()

        val subjects = dbi.getAsJsonObject("subjects")
        val teachers = dbi.getAsJsonObject("teachers")
        val classes = dbi.getAsJsonObject("classes")

        plans.entrySet().mapNotNull { (_, value) ->
            if (value == null || value.isJsonNull) return@mapNotNull null
            val p = try { value.asJsonObject } catch (_: Exception) { return@mapNotNull null }
            val planId = p.str("planid")?.toIntOrNull() ?: return@mapNotNull null

            val subjectId = p.str("predmetid")?.toIntOrNull()
            val subjectName = subjectId?.let { subjects?.getAsJsonObject(it.toString())
                ?.let { s -> s.str("name") ?: s.str("short") } }

            val teacherId = p.str("ucitelid")?.toIntOrNull() ?: p.strList("ucitelids").firstOrNull()?.toIntOrNull()
            val teacherName = teacherId?.let {
                val t = teachers?.getAsJsonObject(it.toString())
                    ?: teachers?.getAsJsonObject((-it).toString())
                t?.let { tr -> "${tr.str("firstname").orEmpty()} ${tr.str("lastname").orEmpty()}".trim().ifEmpty { null } }
            }

            val classIds = p.strList("triedy")
            val className = classIds.firstOrNull()?.let { c ->
                classes?.getAsJsonObject(c)?.let { cl -> cl.str("name") ?: cl.str("short") }
            }

            SchoolPlan(
                planId = planId,
                subjectId = subjectId,
                subjectName = subjectName,
                teacherId = teacherId,
                teacherName = teacherName,
                year = p.str("rok")?.toIntOrNull(),
                seasonId = p.str("obdobie"),
                name = p.str("nazovPlanu") ?: subjectName,
                className = className,
                topicsCount = p.int("countTopics") ?: 0,
                taughtCount = p.int("countTaught") ?: 0,
                standardsCount = p.int("countStandards") ?: 0,
                isPublic = p.str("zverejnit_studentom") == "1",
                isValid = p.str("valid") == "1",
            )
        }
    }
}

