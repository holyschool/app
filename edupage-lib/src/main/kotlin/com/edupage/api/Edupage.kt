package com.edupage.api

import com.edupage.api.model.Classroom
import com.edupage.api.model.EduClass
import com.edupage.api.model.EduCloudFile
import com.edupage.api.model.Meals
import com.edupage.api.model.RingingTime
import com.edupage.api.model.Subject
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.TimetableChange
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.Term
import com.edupage.api.model.people.EduAccount
import com.edupage.api.model.people.EduStudent
import com.edupage.api.model.people.EduStudentSkeleton
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.timetable.Timetable
import com.edupage.api.modules.Login
import com.edupage.api.modules.TwoFactorLogin
import com.edupage.api.modules.Timetables
import com.edupage.api.modules.People
import com.edupage.api.modules.Classes
import com.edupage.api.modules.Classrooms
import com.edupage.api.modules.Subjects
import com.edupage.api.modules.Grades
import com.edupage.api.modules.Messages
import com.edupage.api.modules.Timeline
import com.edupage.api.modules.Substitution
import com.edupage.api.modules.Lunches
import com.edupage.api.modules.Ringing
import com.edupage.api.modules.Cloud
import com.edupage.api.modules.Parent
import com.edupage.api.modules.CustomRequest
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class Edupage(timeoutSeconds: Long = 15L) {

    val session = EdupageSession(timeoutSeconds)

    val isLoggedIn: Boolean get() = session.isLoggedIn

    val subdomain: String? get() = session.subdomain

    val username: String? get() = session.username

    suspend fun login(username: String, password: String, subdomain: String): TwoFactorLogin? =
        Login(session).login(username, password, subdomain)

    suspend fun loginAuto(username: String, password: String): TwoFactorLogin? =
        Login(session).loginAuto(username, password)

    companion object {
        suspend fun fromSessionId(sessionId: String, subdomain: String, username: String): Edupage {
            val instance = Edupage()
            Login(instance.session).reloadData(subdomain, sessionId, username)
            return instance
        }
    }

    fun getUserId(): String? = session.getUserId()

    fun getSchoolYear(): Int? = session.getSchoolYear()

    suspend fun getMyTimetable(date: LocalDate): Timetable? =
        Timetables(session).getMyTimetable(date)

    suspend fun getTimetable(target: Any, date: LocalDate): Timetable? =
        Timetables(session).getTimetable(target, date)

    suspend fun getStudents(): List<EduStudent>? =
        People(session).getStudents()

    suspend fun getAllStudents(): List<EduStudentSkeleton>? =
        People(session).getAllStudents()

    suspend fun getTeachers(): List<EduTeacher>? =
        People(session).getTeachers()

    suspend fun getClasses(): List<EduClass>? =
        Classes(session).getClasses()

    suspend fun getClassrooms(): List<Classroom>? =
        Classrooms(session).getClassrooms()

    suspend fun getSubjects(): List<Subject>? =
        Subjects(session).getSubjects()

    suspend fun getGrades(): List<EduGrade> =
        Grades(session).getGrades()

    suspend fun getGradesForTerm(year: Int, term: Term): List<EduGrade> =
        Grades(session).getGrades(term, year)

    suspend fun sendMessage(recipients: List<EduAccount>, body: String): Int =
        Messages(session).sendMessage(recipients, body)

    suspend fun sendMessage(recipient: EduAccount, body: String): Int =
        sendMessage(listOf(recipient), body)

    suspend fun getNotifications(): List<TimelineEvent> =
        Timeline(session).getNotifications()

    suspend fun getNotificationHistory(dateFrom: LocalDate): List<TimelineEvent> =
        Timeline(session).getNotificationsHistory(dateFrom)

    suspend fun getMeals(date: LocalDate): Meals? =
        Lunches(session).getMeals(date)

    suspend fun getMissingTeachers(date: LocalDate): List<EduTeacher> =
        Substitution(session).getMissingTeachers(date)

    suspend fun getTimetableChanges(date: LocalDate): List<TimetableChange> =
        Substitution(session).getTimetableChanges(date)

    suspend fun getNextRingingTime(dateTime: LocalDateTime): RingingTime? =
        Ringing(session).getNextRingingTime(dateTime)

    suspend fun cloudUpload(file: File): EduCloudFile =
        Cloud(session).uploadFile(file)

    suspend fun switchToChild(child: EduAccount) =
        Parent(session).switchToChild(child)

    suspend fun switchToChild(personId: Int) =
        Parent(session).switchToChild(personId)

    suspend fun switchToParent() =
        Parent(session).switchToParent()

    suspend fun customRequest(
        url: String,
        method: String,
        data: String = "",
        headers: Map<String, String> = emptyMap()
    ): String = CustomRequest(session).customRequest(url, method, data, headers)
}
