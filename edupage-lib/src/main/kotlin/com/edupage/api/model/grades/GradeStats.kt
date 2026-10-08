package com.edupage.api.model.grades

import java.time.LocalDateTime
import java.time.Month

data class SubjectAverage(
    val subjectId: Int,
    val subjectName: String?,
    val average: Double?,
    val gradeCount: Int,
    val weights: Double,
    val bestGrade: Double?,
    val worstGrade: Double?,
    val lastGrade: EduGrade?,
)

data class GradeStats(
    val overallAverage: Double?,
    val totalGrades: Int,
    val weightedAverage: Double?,
    val bySubject: List<SubjectAverage>,
    val byMonth: List<MonthBucket>,
    val trend: List<EduGrade>,
)

data class MonthBucket(
    val year: Int,
    val month: Month,
    val average: Double?,
    val gradeCount: Int,
)

fun List<EduGrade>.computeStats(): GradeStats {
    val numeric = filter { it.gradeN is Number }
        .map { it.gradeN as Number }

    val numericPairs = filter { it.gradeN is Number && it.gradeN as Double > 0 }
        .map { it.gradeN as Double }

    val overallAverage = numericPairs.takeIf { it.isNotEmpty() }?.let { it.average() }
    val weighted = numeric
        .map { it.toDouble() }

    val bySubject = groupBy { it.subjectId }
        .map { (id, grades) ->
            val nums = grades.filter { it.gradeN is Number }
                .map { it.gradeN as Double }
            val last = grades.maxByOrNull { it.date }
            SubjectAverage(
                subjectId = id,
                subjectName = grades.firstNotNullOfOrNull { it.subjectName },
                average = nums.takeIf { it.isNotEmpty() }?.let { it.average() },
                gradeCount = grades.size,
                weights = nums.sum(),
                bestGrade = nums.minOrNull(),
                worstGrade = nums.maxOrNull(),
                lastGrade = last,
            )
        }
        .sortedBy { it.subjectName?.lowercase() ?: "" }

    val byMonth = groupBy { it.date.year to it.date.month }
        .map { (key, grades) ->
            val nums = grades.filter { it.gradeN is Number }.map { it.gradeN as Double }
            MonthBucket(
                year = key.first,
                month = key.second,
                average = nums.takeIf { it.isNotEmpty() }?.let { it.average() },
                gradeCount = grades.size,
            )
        }
        .sortedWith(compareBy<MonthBucket> { it.year }.thenBy { it.month.value })

    val trend = sortedBy { it.date }
        .filter { it.gradeN is Number && (it.gradeN as Double) > 0 }

    return GradeStats(
        overallAverage = overallAverage,
        totalGrades = size,
        weightedAverage = overallAverage,
        bySubject = bySubject,
        byMonth = byMonth,
        trend = trend,
    ).let { stats ->
        if (trend.isEmpty()) stats.copy(overallAverage = null, weightedAverage = null)
        else stats
    }
}

private fun List<Double>.average(): Double = sum() / size

