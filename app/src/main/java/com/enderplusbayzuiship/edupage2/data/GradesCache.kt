package com.enderplusbayzuiship.edupage2.data

import android.content.Context
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

// ── Serializable flat representation ─────────────────────────────────────────

/**
 * Flat, Gson-serializable snapshot of an [EduGrade].
 * [gradeNDouble] and [gradeNString] encode the [Any?] gradeN field:
 *   - numeric grade  → gradeNDouble set, gradeNString null
 *   - string grade   → gradeNString set, gradeNDouble null
 *   - null           → both null
 */
private data class CachedGrade(
    val eventId: Int,
    val title: String,
    val gradeNDouble: Double?,
    val gradeNString: String?,
    val comment: String?,
    val dateIso: String,            // ISO local date-time: "yyyy-MM-dd HH:mm:ss"
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
    val term: String,               // "T1" or "T2"
    val year: Int,
    val fetchedAtMs: Long,
    val grades: List<CachedGrade>,
)

// ── Cache ─────────────────────────────────────────────────────────────────────

@Singleton
class GradesCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private fun fileName(termKey: String) = "grades_cache_$termKey.json"
    }

    private val gson = Gson()

    private fun file(termKey: String): File =
        File(context.filesDir, fileName(termKey))

    /** Persist a fresh list of [EduGrade] for the given term + year. */
    fun save(term: Term, year: Int, grades: List<EduGrade>) {
        val termKey = term.termKey()
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

    /**
     * Load cached grades for [term] + [year], or null if no cache exists for that combination.
     * No staleness check — callers decide when to refresh.
     */
    fun load(term: Term, year: Int): List<EduGrade>? {
        val termKey = term.termKey()
        val f = file(termKey)
        if (!f.exists()) return null
        return try {
            val type = object : TypeToken<GradesCacheFile>() {}.type
            val cf: GradesCacheFile = gson.fromJson(f.readText(), type)
            if (cf.year != year) return null     // different school year — stale
            cf.grades.mapNotNull { it.toEduGrade() }
        } catch (_: Exception) {
            null
        }
    }

    /** Returns true if a cache file exists for [term] (regardless of staleness). */
    fun hasCacheFor(term: Term): Boolean = file(term.termKey()).exists()

    /** Delete all grade cache files (e.g. on logout). */
    fun clear() {
        file("T1").delete()
        file("T2").delete()
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

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
