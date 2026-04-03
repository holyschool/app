package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.dbi.DbiHelper
import com.edupage.api.exceptions.MissingDataException
import com.edupage.api.model.EduClass
import com.edupage.api.model.Classroom
import com.edupage.api.model.Subject
import com.edupage.api.model.people.Gender
import com.edupage.api.model.people.EduStudent
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.people.EduStudentSkeleton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class People(private val session: EdupageSession) {

    private val dbi = DbiHelper(session)
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun getStudents(): List<EduStudent>? {
        checkLoggedIn()
        val students = dbi.fetchStudentList() ?: return null
        val result = mutableListOf<EduStudent>()
        for (key in students.keySet()) {
            if (key.isNullOrEmpty()) continue
            val id = key.toIntOrNull() ?: continue
            val data = students.getAsJsonObject(key) ?: continue
            val name = dbi.fetchStudentName(id) ?: continue
            val gender = Gender.parse(data.get("gender")?.asString)
            val since = parseDate(data.get("datefrom")?.asString)
            val classId = data.get("classid")?.asString?.toIntOrNull()
            val numberInClass = data.get("numberinclass")?.asString?.toIntOrNull()
            result.add(EduStudent(id, name, gender, since, classId, numberInClass))
        }
        return result
    }

    suspend fun getAllStudents(): List<EduStudentSkeleton>? {
        checkLoggedIn()
        val year = session.getSchoolYear() ?: throw MissingDataException("No school year in session data")
        val rows = dbi.fetchAllStudentsFromServer(year)
        return rows.map { row ->
            EduStudentSkeleton(
                personId = row.get("id")?.asInt ?: 0,
                nameShort = row.get("short")?.asString ?: "",
                classId = row.get("classid")?.asString?.toIntOrNull()
            )
        }
    }

    suspend fun getTeachers(): List<EduTeacher>? {
        checkLoggedIn()
        val teachers = dbi.fetchTeacherList() ?: return null
        val result = mutableListOf<EduTeacher>()
        for (key in teachers.keySet()) {
            if (key.isNullOrEmpty()) continue
            val id = key.toIntOrNull() ?: continue
            val data = teachers.getAsJsonObject(key) ?: continue
            val name = dbi.fetchTeacherName(id) ?: continue
            val gender = Gender.parse(data.get("gender")?.asString)
            val since = parseDate(data.get("datefrom")?.asString)
            val to = parseDate(data.get("dateto")?.asString)
            val classroomId = data.get("classroomid")?.asString
            val classroomName = dbi.fetchClassroomNumber(classroomId)
            result.add(EduTeacher(id, name, gender, since, classroomName, to))
        }
        return result
    }

    suspend fun getTeacher(teacherId: Int): EduTeacher? {
        checkLoggedIn()
        val teachers = dbi.fetchTeacherList() ?: return null
        val data = teachers.getAsJsonObject(teacherId.toString()) ?: return null
        val name = dbi.fetchTeacherName(teacherId) ?: return null
        val gender = Gender.parse(data.get("gender")?.asString)
        val since = parseDate(data.get("datefrom")?.asString)
        val to = parseDate(data.get("dateto")?.asString)
        val classroomId = data.get("classroomid")?.asString
        val classroomName = dbi.fetchClassroomNumber(classroomId)
        return EduTeacher(teacherId, name, gender, since, classroomName, to)
    }

    suspend fun getStudent(studentId: Int): EduStudent? {
        checkLoggedIn()
        val students = dbi.fetchStudentList() ?: return null
        val data = students.getAsJsonObject(studentId.toString()) ?: return null
        val name = dbi.fetchStudentName(studentId) ?: return null
        val gender = Gender.parse(data.get("gender")?.asString)
        val since = parseDate(data.get("datefrom")?.asString)
        val classId = data.get("classid")?.asString?.toIntOrNull()
        val numberInClass = data.get("numberinclass")?.asString?.toIntOrNull()
        return EduStudent(studentId, name, gender, since, classId, numberInClass)
    }

    private fun checkLoggedIn() {
        if (!session.isLoggedIn) throw com.edupage.api.exceptions.NotLoggedInException()
    }

    private fun parseDate(value: String?): LocalDateTime? {
        if (value.isNullOrEmpty()) return null
        return try {
            LocalDateTime.parse(value + "T00:00:00")
        } catch (e: Exception) {
            null
        }
    }
}

internal class Classes(private val session: EdupageSession) {
    private val dbi = DbiHelper(session)

    suspend fun getClasses(): List<EduClass>? {
        if (!session.isLoggedIn) throw com.edupage.api.exceptions.NotLoggedInException()
        val classes = dbi.fetchClassList() ?: return null
        return classes.keySet().mapNotNull { key ->
            val id = key.toIntOrNull() ?: return@mapNotNull null
            val data = classes.getAsJsonObject(key) ?: return@mapNotNull null
            EduClass(
                classId = id,
                name = data.get("name")?.asString,
                grade = data.get("grade")?.asString?.toIntOrNull(),
                teacherId = data.get("teacherid")?.asString?.toIntOrNull()
            )
        }
    }

    suspend fun getClass(classId: String?): EduClass? {
        val id = classId?.toIntOrNull() ?: return null
        val classes = dbi.fetchClassList() ?: return null
        val data = classes.getAsJsonObject(id.toString()) ?: return null
        return EduClass(
            classId = id,
            name = data.get("name")?.asString,
            grade = data.get("grade")?.asString?.toIntOrNull(),
            teacherId = data.get("teacherid")?.asString?.toIntOrNull()
        )
    }
}

internal class Classrooms(private val session: EdupageSession) {
    private val dbi = DbiHelper(session)

    suspend fun getClassrooms(): List<Classroom>? {
        if (!session.isLoggedIn) throw com.edupage.api.exceptions.NotLoggedInException()
        val classrooms = dbi.fetchClassroomList() ?: return null
        return classrooms.keySet().mapNotNull { key ->
            val id = key.toIntOrNull() ?: return@mapNotNull null
            val data = classrooms.getAsJsonObject(key) ?: return@mapNotNull null
            Classroom(
                classroomId = id,
                name = data.get("name")?.asString,
                shortName = data.get("short")?.asString
            )
        }
    }

    suspend fun getClassroom(classroomId: String?): Classroom? {
        val id = classroomId?.toIntOrNull() ?: return null
        val classrooms = dbi.fetchClassroomList() ?: return null
        val data = classrooms.getAsJsonObject(id.toString()) ?: return null
        return Classroom(
            classroomId = id,
            name = data.get("name")?.asString,
            shortName = data.get("short")?.asString
        )
    }
}

internal class Subjects(private val session: EdupageSession) {
    private val dbi = DbiHelper(session)

    suspend fun getSubjects(): List<Subject>? {
        if (!session.isLoggedIn) throw com.edupage.api.exceptions.NotLoggedInException()
        val subjects = dbi.fetchSubjectList() ?: return null
        return subjects.keySet().mapNotNull { key ->
            val id = key.toIntOrNull() ?: return@mapNotNull null
            val data = subjects.getAsJsonObject(key) ?: return@mapNotNull null
            Subject(
                subjectId = id,
                name = data.get("name")?.asString,
                shortName = data.get("short")?.asString
            )
        }
    }

    suspend fun getSubject(subjectId: String?): Subject? {
        val id = subjectId?.toIntOrNull() ?: return null
        val subjects = dbi.fetchSubjectList() ?: return null
        val data = subjects.getAsJsonObject(id.toString()) ?: return null
        return Subject(
            subjectId = id,
            name = data.get("name")?.asString,
            shortName = data.get("short")?.asString
        )
    }
}
