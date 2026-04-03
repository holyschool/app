package com.edupage.api.model.people

import java.time.LocalDateTime

class EduTeacher(
    personId: Int,
    name: String,
    gender: Gender?,
    inSchoolSince: LocalDateTime?,
    val classroomName: String?,
    val teacherTo: LocalDateTime?
) : EduAccount(personId, name, gender, inSchoolSince, EduAccountType.TEACHER) {

    override fun toString(): String =
        "EduTeacher(personId=$personId, name='$name', classroomName=$classroomName)"
}
