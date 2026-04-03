package com.edupage.api.model.people

import java.time.LocalDateTime

class EduStudent(
    personId: Int,
    name: String,
    gender: Gender?,
    inSchoolSince: LocalDateTime?,
    val classId: Int?,
    val numberInClass: Int?
) : EduAccount(personId, name, gender, inSchoolSince, EduAccountType.STUDENT) {

    private var studentOnly: Boolean = false

    fun setStudentOnly(value: Boolean) {
        studentOnly = value
    }

    override fun getId(): String {
        val base = super.getId()
        return if (studentOnly) base.replace("Student", "StudentOnly") else base
    }

    override fun toString(): String =
        "EduStudent(personId=$personId, name='$name', classId=$classId, numberInClass=$numberInClass)"
}
