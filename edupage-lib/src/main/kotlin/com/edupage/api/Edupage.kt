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
import com.edupage.api.modules.*
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Main entry point for the EduPage Kotlin API.
 *
 * Mirrors the Python `Edupage` class — all methods are direct ports.
 * All I/O operations are `suspend` functions and must be called from a coroutine.
 *
 * Usage:
 * ```kotlin
 * val edupage = Edupage()
 * edupage.login("username", "password", "myschool")
 * val timetable = edupage.getMyTimetable(LocalDate.now())
 * ```
 */
class Edupage(timeoutSeconds: Long = 15L) {

    val session = EdupageSession(timeoutSeconds)

    /** Whether the user is currently logged in. */
    val isLoggedIn: Boolean get() = session.isLoggedIn

    /** The school subdomain (e.g. "myschool" for myschool.edupage.org). */
    val subdomain: String? get() = session.subdomain

    /** The logged-in username. */
    val username: String? get() = session.username

    // ─── Authentication ──────────────────────────────────────────────────────

    /**
     * Login with an explicit school subdomain.
     *
     * @return null if no 2FA required, or a [TwoFactorLogin] to complete 2FA.
     * @throws com.edupage.api.exceptions.BadCredentialsException on wrong credentials.
     * @throws com.edupage.api.exceptions.CaptchaException if captcha is required.
     */
    suspend fun login(username: String, password: String, subdomain: String): TwoFactorLogin? =
        Login(session).login(username, password, subdomain)

    /**
     * Login via portal.edupage.org (auto-detects subdomain).
     * Falls back to [login] if portal login fails.
     *
     * @return null if no 2FA required, or a [TwoFactorLogin] to complete 2FA.
     */
    suspend fun loginAuto(username: String, password: String): TwoFactorLogin? =
        Login(session).loginAuto(username, password)

    /**
     * Create an [Edupage] instance from an existing PHPSESSID cookie.
     */
    companion object {
        suspend fun fromSessionId(sessionId: String, subdomain: String, username: String): Edupage {
            val instance = Edupage()
            Login(instance.session).reloadData(subdomain, sessionId, username)
            return instance
        }
    }

    // ─── User Identity ────────────────────────────────────────────────────────

    /** Returns the EduPage user ID of the logged-in account. */
    fun getUserId(): String? = session.getUserId()

    /** Returns the current school year (starting year). */
    fun getSchoolYear(): Int? = session.getSchoolYear()

    // ─── Timetable ────────────────────────────────────────────────────────────

    /**
     * Get the timetable for the logged-in user on a specific date.
     */
    suspend fun getMyTimetable(date: LocalDate): Timetable? =
        Timetables(session).getMyTimetable(date)

    /**
     * Get the timetable for a teacher, student, class, or classroom on a specific date.
     *
     * @param target An [EduTeacher], [EduStudent], [EduClass], or [Classroom].
     */
    suspend fun getTimetable(target: Any, date: LocalDate): Timetable? =
        Timetables(session).getTimetable(target, date)

    // ─── People ───────────────────────────────────────────────────────────────

    /** Get all students in the logged-in user's class. */
    suspend fun getStudents(): List<EduStudent>? =
        People(session).getStudents()

    /** Get a lightweight list of all students in the entire school. */
    suspend fun getAllStudents(): List<EduStudentSkeleton>? =
        People(session).getAllStudents()

    /** Get all teachers in the school. */
    suspend fun getTeachers(): List<EduTeacher>? =
        People(session).getTeachers()

    // ─── School Structure ─────────────────────────────────────────────────────

    /** Get all classes. */
    suspend fun getClasses(): List<EduClass>? =
        Classes(session).getClasses()

    /** Get all classrooms. */
    suspend fun getClassrooms(): List<Classroom>? =
        Classrooms(session).getClassrooms()

    /** Get all subjects. */
    suspend fun getSubjects(): List<Subject>? =
        Subjects(session).getSubjects()

    // ─── Grades ───────────────────────────────────────────────────────────────

    /** Get all available grades for the logged-in student. */
    suspend fun getGrades(): List<EduGrade> =
        Grades(session).getGrades()

    /**
     * Get grades for a specific school year and term.
     *
     * @param year  The starting year of the school year (e.g. 2024 for 2024/2025).
     * @param term  [Term.FIRST] or [Term.SECOND].
     */
    suspend fun getGradesForTerm(year: Int, term: Term): List<EduGrade> =
        Grades(session).getGrades(term, year)

    // ─── Messages ─────────────────────────────────────────────────────────────

    /**
     * Send a message to one or more recipients.
     *
     * @param recipients List of [EduAccount] (students, teachers, etc.).
     * @param body       The message text.
     * @return The timeline ID of the created message.
     */
    suspend fun sendMessage(recipients: List<EduAccount>, body: String): Int =
        Messages(session).sendMessage(recipients, body)

    /** Convenience overload for a single recipient. */
    suspend fun sendMessage(recipient: EduAccount, body: String): Int =
        sendMessage(listOf(recipient), body)

    // ─── Notifications / Timeline ─────────────────────────────────────────────

    /** Get all available notifications (up to ~1 month back). */
    suspend fun getNotifications(): List<TimelineEvent> =
        Timeline(session).getNotifications()

    /**
     * Get all notifications since [dateFrom] until now.
     * Use this if you need history older than 1 month.
     */
    suspend fun getNotificationHistory(dateFrom: LocalDate): List<TimelineEvent> =
        Timeline(session).getNotificationsHistory(dateFrom)

    // ─── Meals / Lunches ──────────────────────────────────────────────────────

    /**
     * Get lunch/meal options for a given date.
     */
    suspend fun getMeals(date: LocalDate): Meals? =
        Lunches(session).getMeals(date)

    // ─── Substitutions ────────────────────────────────────────────────────────

    /** Get the list of teachers absent on a given date. */
    suspend fun getMissingTeachers(date: LocalDate): List<EduTeacher> =
        Substitution(session).getMissingTeachers(date)

    /** Get timetable changes (substitutions) for a given date. */
    suspend fun getTimetableChanges(date: LocalDate): List<TimetableChange> =
        Substitution(session).getTimetableChanges(date)

    // ─── Ringing times ────────────────────────────────────────────────────────

    /**
     * Get the next bell ringing time (lesson start or end) after the given moment.
     */
    suspend fun getNextRingingTime(dateTime: LocalDateTime): RingingTime? =
        Ringing(session).getNextRingingTime(dateTime)

    // ─── Cloud ────────────────────────────────────────────────────────────────

    /**
     * Upload a file to EduPage cloud storage.
     */
    suspend fun cloudUpload(file: File): EduCloudFile =
        Cloud(session).uploadFile(file)

    // ─── Parent features ──────────────────────────────────────────────────────

    /**
     * Switch to a child account (parent accounts only).
     * After calling this, all API methods return data for the child.
     *
     * @param child An [EduAccount] or person ID of the child.
     */
    suspend fun switchToChild(child: EduAccount) =
        Parent(session).switchToChild(child)

    suspend fun switchToChild(personId: Int) =
        Parent(session).switchToChild(personId)

    /** Switch back to the parent account (parent accounts only). */
    suspend fun switchToParent() =
        Parent(session).switchToParent()

    // ─── Custom requests ──────────────────────────────────────────────────────

    /**
     * Send a custom HTTP request to any EduPage endpoint.
     *
     * @param url     Full URL.
     * @param method  "GET" or "POST".
     * @param data    Body data for POST.
     * @param headers Additional headers.
     * @return Raw response string.
     */
    suspend fun customRequest(
        url: String,
        method: String,
        data: String = "",
        headers: Map<String, String> = emptyMap()
    ): String = CustomRequest(session).customRequest(url, method, data, headers)
}
