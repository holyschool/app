package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import android.util.Log
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.Term
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.people.Gender
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private data class CachedGrade(
    val eventId: Int,
    val title: String,
    val gradeNDouble: Double?,
    val gradeNString: String?,
    val comment: String?,
    val dateIso: String,
    val subjectId: Int,
    val subjectName: String?,
    val teacherId: Int?,
    val teacherName: String?,
    val teacherGender: String?,
    val maxPoints: Double?,
    val moreDetails: List<String>?,
    val importance: Double?,
    val verbal: Boolean,
    val percent: Double?,
    val classGradeAvg: Double?,
)

private data class GradesCacheFile(
    val term: String,
    val year: Int,
    val fetchedAtMs: Long,
    val grades: List<CachedGrade>,
)

@Singleton
class GradesCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private fun fileName(termKey: String) = "grades_cache_$termKey.json"
        private const val TAG = "GradesCache"
    }

    private val gson = Gson()

    private fun file(termKey: String): File =
        File(context.filesDir, fileName(termKey))

    fun save(term: Term, year: Int, grades: List<EduGrade>) {
        val termKey = term.termKey()
        Log.i(TAG, "saving ${grades.size} grades for $termKey year=$year")
        val cached = grades.map { g ->
            CachedGrade(
                eventId       = g.eventId,
                title         = g.title,
                gradeNDouble  = (g.gradeN as? Double),
                gradeNString  = (g.gradeN as? String),
                comment       = g.comment,
                dateIso       = g.date.format(DATE_FMT),
                subjectId     = g.subjectId,
                subjectName   = g.subjectName,
                teacherId     = g.teacher?.personId,
                teacherName   = g.teacher?.name,
                teacherGender = g.teacher?.gender?.value,
                maxPoints     = g.maxPoints,
                moreDetails   = g.moreDetails,
                importance    = g.importance,
                verbal        = g.verbal,
                percent       = g.percent,
                classGradeAvg = g.classGradeAvg,
            )
        }
        val cacheFile = GradesCacheFile(
            term        = termKey,
            year        = year,
            fetchedAtMs = System.currentTimeMillis(),
            grades      = cached,
        )
        file(termKey).writeText(gson.toJson(cacheFile))
    }

    fun load(term: Term, year: Int): List<EduGrade>? {
        val termKey = term.termKey()
        val f = file(termKey)
        if (!f.exists()) {
            Log.i(TAG, "cache miss: no file for $termKey")
            return null
        }
        return try {
            val type = object : TypeToken<GradesCacheFile>() {}.type
            val cf: GradesCacheFile = gson.fromJson(f.readText(), type)
            if (cf.year != year) {
                Log.i(TAG, "cache stale: cached year=${cf.year}, requested year=$year for $termKey")
                return null
            }
            val grades = cf.grades.mapNotNull { it.toEduGrade() }
            Log.i(TAG, "cache hit: loaded ${grades.size} grades for $termKey year=$year")
            grades
        } catch (e: Exception) {
            Log.e(TAG, "failed to read cache for $termKey: ${e.message}", e)
            null
        }
    }

    fun hasCacheFor(term: Term): Boolean = file(term.termKey()).exists()

    fun clear() {
        Log.i(TAG, "clearing all grade caches")
        file("T1").delete()
        file("T2").delete()
    }

    private fun CachedGrade.toEduGrade(): EduGrade? {
        val date = try { LocalDateTime.parse(dateIso, DATE_FMT) } catch (_: Exception) { return null }
        val gradeN: Any? = when {
            gradeNDouble != null -> gradeNDouble
            gradeNString != null -> gradeNString
            else                 -> null
        }
        val teacher: EduTeacher? = if (teacherId != null && teacherName != null) {
            EduTeacher(
                personId       = teacherId,
                name           = teacherName,
                gender         = Gender.parse(teacherGender),
                inSchoolSince  = null,
                classroomName  = null,
                teacherTo      = null,
            )
        } else null

        return EduGrade(
            eventId       = eventId,
            title         = title,
            gradeN        = gradeN,
            comment       = comment,
            date          = date,
            subjectId     = subjectId,
            subjectName   = subjectName,
            teacher       = teacher,
            maxPoints     = maxPoints,
            moreDetails   = moreDetails,
            importance    = importance,
            verbal        = verbal,
            percent       = percent,
            classGradeAvg = classGradeAvg,
        )
    }

    private fun Term.termKey() = if (this == Term.FIRST) "T1" else "T2"
}

