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
import com.edupage.api.model.grades.computeStats
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

    suspend fun computeGradeStats(grades: List<EduGrade>): com.edupage.api.model.grades.GradeStats =
        grades.computeStats()

    suspend fun sendMessage(recipients: List<EduAccount>, body: String, important: Boolean = false, files: List<EduCloudFile> = emptyList()): Int =
        Messages(session).sendMessage(recipients, body, important, files)

    suspend fun sendMessage(recipient: EduAccount, body: String, important: Boolean = false, files: List<EduCloudFile> = emptyList()): Int =
        sendMessage(listOf(recipient), body, important, files)

    suspend fun createPoll(
        recipients: List<EduAccount>,
        question: String,
        answers: List<String>,
        anonymous: Boolean = false,
        singleChoice: Boolean = false,
        files: List<EduCloudFile> = emptyList(),
    ): Int = Messages(session).createPoll(recipients, question, answers, anonymous, singleChoice, files)

    suspend fun sendReply(
        recipientUserString: String?,
        body: String,
        replyToTimelineId: Int,
        files: List<EduCloudFile> = emptyList(),
    ): Int = Messages(session).sendReply(recipientUserString, body, replyToTimelineId, files)

    suspend fun deleteMessage(timelineId: Int): Boolean =
        Messages(session).deleteItem(timelineId)

    suspend fun markMessageSeen(timelineId: Int): Boolean =
        Messages(session).markAsSeen(timelineId)

    suspend fun likeMessage(timelineId: Int, liked: Boolean = true): Boolean =
        Messages(session).markAsLiked(timelineId, liked)

    suspend fun toggleMessageDone(timelineId: Int, done: Boolean): Boolean =
        Messages(session).markAsDone(timelineId, done)

    suspend fun toggleMessageStarred(timelineId: Int, starred: Boolean): Boolean =
        Messages(session).markAsStarred(timelineId, starred)

    suspend fun voteOnPoll(timelineId: Int, answerIds: List<String>): Boolean =
        Messages(session).voteOnPoll(timelineId, answerIds)

    suspend fun getNotifications(): List<TimelineEvent> =
        Timeline(session).getNotifications()

    suspend fun getNotificationHistory(dateFrom: LocalDate): List<TimelineEvent> =
        Timeline(session).getNotificationsHistory(dateFrom)

    suspend fun getAssignments(dateFrom: LocalDate): List<com.edupage.api.model.grades.Assignment> =
        Timeline(session).getAssignments(dateFrom)

    suspend fun getAssignmentData(superId: String): com.google.gson.JsonObject? =
        com.edupage.api.modules.Assignments(session).getAssignmentData(superId)

    suspend fun getPlans(): List<com.edupage.api.model.SchoolPlan> =
        com.edupage.api.modules.Plans(session).getPlans()

    suspend fun getAbsences(dateFrom: java.time.LocalDate): List<com.edupage.api.model.Absence> =
        com.edupage.api.modules.Attendance(session).getAbsences(dateFrom)

    fun extractAbsences(events: List<TimelineEvent>): List<com.edupage.api.model.Absence> =
        com.edupage.api.modules.Attendance(session).fromTimeline(events)

    fun searchHistory(events: List<TimelineEvent>, query: String): List<TimelineEvent> =
        com.edupage.api.modules.Timeline(session).searchHistory(events, query)

    suspend fun getMeals(date: LocalDate): Meals? =
        Lunches(session).getMeals(date)

    suspend fun orderMeal(date: LocalDate, mealTypeIndex: String, choice: String, boarderId: String): Boolean =
        Lunches(session).orderMeal(date, mealTypeIndex, choice, boarderId)

    suspend fun cancelMeal(date: LocalDate, mealTypeIndex: String, boarderId: String): Boolean =
        Lunches(session).cancelMeal(date, mealTypeIndex, boarderId)

    suspend fun getMissingTeachers(date: LocalDate): List<EduTeacher> =
        Substitution(session).getMissingTeachers(date)

    suspend fun getTimetableChanges(date: LocalDate): List<TimetableChange> =
        Substitution(session).getTimetableChanges(date)

    suspend fun getNextRingingTime(dateTime: LocalDateTime): RingingTime? =
        Ringing(session).getNextRingingTime(dateTime)

    suspend fun cloudUpload(file: File): EduCloudFile =
        Cloud(session).uploadFile(file)

    suspend fun cloudList(): List<EduCloudFile> =
        Cloud(session).listCloudFiles()

    suspend fun cloudDelete(fileId: String): Boolean =
        Cloud(session).deleteCloudFile(fileId)

    suspend fun cloudDownload(uploadPath: String, destination: File): File =
        Cloud(session).downloadFile(uploadPath, destination)

    val children: List<EduAccount>?
        get() = session.children

    val isParent: Boolean
        get() = session.isParentAccount

    val currentChildId: Int?
        get() = session.activeChildId

    fun getCurrentChild(): EduAccount? {
        val childId = session.activeChildId ?: return null
        return session.children?.find { it.personId == childId }
    }

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

