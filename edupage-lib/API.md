# edupage-lib API Reference

> Kotlin/JVM library — `com.edupage.api`

---

## Entry Point

### `Edupage`

The single entry point for all API operations.

```kotlin
class Edupage(timeoutSeconds: Long = 15L)
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `timeoutSeconds` | `Long` | `15` | HTTP connect/read/write timeout |

**Properties**

| Name | Type | Description |
|---|---|---|
| `session` | `EdupageSession` | The current authenticated session |
| `isLoggedIn` | `Boolean` | `true` after a successful login |
| `subdomain` | `String?` | School subdomain (e.g. `"myschool"` for `myschool.edupage.org`) |
| `username` | `String?` | Username of the logged-in account |

---

## Authentication

### `login`

```kotlin
suspend fun login(username: String, password: String, subdomain: String): TwoFactorLogin?
```

Logs in directly to a known subdomain. Returns a `TwoFactorLogin` object if the school has 2FA enabled, otherwise `null` (login complete).

Throws `BadCredentialsException` on wrong credentials; `CaptchaException` if EduPage is showing a CAPTCHA.

---

### `loginAuto`

```kotlin
suspend fun loginAuto(username: String, password: String): TwoFactorLogin?
```

Resolves the correct subdomain automatically from the EduPage federation service, then calls `login`. Useful when the subdomain is not known in advance.

---

### `Edupage.fromSessionId` (companion)

```kotlin
companion object {
    suspend fun fromSessionId(
        sessionId: String,
        subdomain: String,
        username: String
    ): Edupage
}
```

Restores an existing session from a PHP session cookie (`PHPSESSID`). Throws `NotLoggedInException` if the session is expired or invalid.

---

### `TwoFactorLogin`

Returned by `login` / `loginAuto` when 2FA is required.

```kotlin
class TwoFactorLogin {
    suspend fun verify(code: String)
}
```

Call `verify` with the one-time code sent to the user's device to complete login.

---

## Session

### `EdupageSession`

Accessible via `Edupage.session`. Holds all authenticated state.

| Field | Type | Description |
|---|---|---|
| `cookieJar` | `SessionCookieJar` | In-memory HTTP cookie store |
| `httpClient` | `OkHttpClient` | Pre-configured OkHttp client |
| `data` | `JsonObject?` | Raw parsed JSON from EduPage's home page |
| `isLoggedIn` | `Boolean` | Whether a valid session is active |
| `subdomain` | `String?` | Active school subdomain |
| `gsecHash` | `String?` | `ASC.gsechash` token used in API requests |
| `username` | `String?` | Logged-in username |

**Methods**

| Signature | Returns | Description |
|---|---|---|
| `getSchoolYear()` | `Int?` | Current school year from session data |
| `getUserId()` | `String?` | Internal user ID from session data |

---

### `SessionCookieJar`

Accessible via `Edupage.session.cookieJar`. Useful for manual session persistence.

| Method | Parameters | Returns | Description |
|---|---|---|---|
| `getSessionId(host)` | `host: String` | `String?` | Returns `PHPSESSID` value for the given host |
| `setSessionId(host, sessionId)` | `host: String, sessionId: String` | `Unit` | Injects a `PHPSESSID` cookie manually |

---

## Timetable

### `getMyTimetable`

```kotlin
suspend fun getMyTimetable(date: LocalDate): Timetable?
```

Returns the timetable for the logged-in user on the given date. Returns `null` if no data is available.

---

### `getTimetable`

```kotlin
suspend fun getTimetable(target: Any, date: LocalDate): Timetable?
```

Returns the timetable for another entity on the given date. The `target` parameter must be one of:

| Type | Description |
|---|---|
| `EduTeacher` | A specific teacher |
| `EduStudent` | A specific student |
| `EduClass` | A class/form |
| `Classroom` | A physical room |

Throws `IllegalArgumentException` for unsupported target types.

---

### `Timetable`

```kotlin
data class Timetable(val lessons: List<Lesson>) : Iterable<Lesson>
```

| Method | Parameters | Returns | Description |
|---|---|---|---|
| `getLessonAtTime(time)` | `LocalTime` | `Lesson?` | Lesson currently in progress at `time` |
| `getNextLessonAtTime(time)` | `LocalTime` | `Lesson?` | Next lesson starting after `time` |
| `getNextOnlineLessonAtTime(time)` | `LocalTime` | `Lesson?` | Next online lesson after `time` |
| `getFirstLesson()` | — | `Lesson?` | First lesson of the day |
| `getLastLesson()` | — | `Lesson?` | Last lesson of the day |

---

### `Lesson`

```kotlin
data class Lesson(
    val period: Int?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val duration: Int,
    val subject: Subject?,
    val classes: List<EduClass>?,
    val groups: List<String>?,
    val teachers: List<EduTeacher>?,
    val classrooms: List<Classroom>?,
    val curriculum: String?,
    val onlineLessonLink: String?,
    val isCancelled: Boolean,
    val isEvent: Boolean
)
```

| Method | Returns | Description |
|---|---|---|
| `isOnlineLesson()` | `Boolean` | `true` when `onlineLessonLink` is not null |

---

## People

### `getStudents`

```kotlin
suspend fun getStudents(): List<EduStudent>?
```

Returns full student records for the school. May be `null` if the account lacks permission.

---

### `getAllStudents`

```kotlin
suspend fun getAllStudents(): List<EduStudentSkeleton>?
```

Returns lightweight student stubs (ID, short name, class) — faster than `getStudents`.

---

### `getTeachers`

```kotlin
suspend fun getTeachers(): List<EduTeacher>?
```

Returns all teachers in the school.

---

### `EduAccount` (base class)

```kotlin
open class EduAccount(
    val personId: Int,
    val name: String,
    val gender: Gender?,
    val inSchoolSince: LocalDateTime?,
    val accountType: EduAccountType
)
```

`open fun getId(): String` — returns `"${accountType.value}$personId"` (e.g. `"Student42"`).

---

### `EduStudent`

Extends `EduAccount`. Additional fields:

| Field | Type |
|---|---|
| `classId` | `Int?` |
| `numberInClass` | `Int?` |

---

### `EduStudentSkeleton`

```kotlin
data class EduStudentSkeleton(
    val personId: Int,
    val nameShort: String,
    val classId: Int?
)
```

---

### `EduTeacher`

Extends `EduAccount`. Additional fields:

| Field | Type |
|---|---|
| `classroomName` | `String?` |
| `teacherTo` | `LocalDateTime?` |

---

### `EduParent`

Extends `EduAccount`. No additional fields. `accountType` is always `EduAccountType.PARENT`.

---

### `EduAccountType`

```kotlin
enum class EduAccountType(val value: String) {
    STUDENT("Student"),
    TEACHER("Teacher"),
    PARENT("Rodic")
}
```

---

### `Gender`

```kotlin
enum class Gender(val value: String) {
    MALE("M"),
    FEMALE("F");

    companion object {
        fun parse(value: String?): Gender?
    }
}
```

---

## School Structure

### `getClasses`

```kotlin
suspend fun getClasses(): List<EduClass>?
```

### `getClassrooms`

```kotlin
suspend fun getClassrooms(): List<Classroom>?
```

### `getSubjects`

```kotlin
suspend fun getSubjects(): List<Subject>?
```

---

### `EduClass`

```kotlin
data class EduClass(
    val classId: Int,
    val name: String?,
    val grade: Int?,
    val teacherId: Int?
)
```

### `Classroom`

```kotlin
data class Classroom(
    val classroomId: Int,
    val name: String?,
    val shortName: String?
)
```

### `Subject`

```kotlin
data class Subject(
    val subjectId: Int,
    val name: String?,
    val shortName: String?
)
```

---

## Grades

### `getGrades`

```kotlin
suspend fun getGrades(): List<EduGrade>
```

Returns all grades for the current school year across both terms.

---

### `getGradesForTerm`

```kotlin
suspend fun getGradesForTerm(year: Int, term: Term): List<EduGrade>
```

Returns grades for a specific year and term.

---

### `Term`

```kotlin
enum class Term(val value: String) {
    FIRST("P1"),
    SECOND("P2")
}
```

---

### `EduGrade`

```kotlin
data class EduGrade(
    val eventId: Int,
    val title: String,
    val gradeN: Any?,          // Double for numeric, String for verbal
    val comment: String?,
    val date: LocalDateTime,
    val subjectId: Int,
    val subjectName: String?,
    val teacher: EduTeacher?,
    val maxPoints: Double?,
    val moreDetails: List<String>?,
    val importance: Double?,
    val verbal: Boolean,
    val percent: Double?,
    val classGradeAvg: Double?
)
```

---

## Messages

### `sendMessage`

```kotlin
// To multiple recipients
suspend fun sendMessage(recipients: List<EduAccount>, body: String): Int

// To a single recipient
suspend fun sendMessage(recipient: EduAccount, body: String): Int
```

Returns the timeline ID of the created message. Returns `-1` on server-side failure.

---

## Notifications / Timeline

### `getNotifications`

```kotlin
suspend fun getNotifications(): List<TimelineEvent>
```

Returns recent notification events.

---

### `getNotificationHistory`

```kotlin
suspend fun getNotificationHistory(dateFrom: LocalDate): List<TimelineEvent>
```

Returns all notification events since `dateFrom`.

---

### `TimelineEvent`

```kotlin
data class TimelineEvent(
    val timelineId: Int,
    val type: String?,
    val timestamp: LocalDateTime?,
    val authorId: String?,
    val authorName: String?,
    val title: String?,
    val text: String?,
    val reactionTo: Int?
)
```

---

## Meals (Canteen)

### `getMeals`

```kotlin
suspend fun getMeals(date: LocalDate): Meals?
```

Returns the canteen menu for the given date. Returns `null` if no menu is available.

---

### `Meals`

```kotlin
data class Meals(val date: LocalDate, val meals: List<Meal>)
```

### `Meal`

```kotlin
data class Meal(
    val mealId: String?,
    val name: String,
    val canOrder: Boolean,
    val isOrdered: Boolean,
    val allergens: List<String>?,
    val weight: String?
)
```

---

## Substitutions

### `getMissingTeachers`

```kotlin
suspend fun getMissingTeachers(date: LocalDate): List<EduTeacher>
```

Returns teachers marked as absent on the given date.

---

### `getTimetableChanges`

```kotlin
suspend fun getTimetableChanges(date: LocalDate): List<TimetableChange>
```

Returns substitution/change records for the given date.

---

### `TimetableChange`

```kotlin
data class TimetableChange(
    val date: LocalDate,
    val lessonIndex: Int?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val type: String?,
    val subjectName: String?,
    val teacherName: String?,
    val classroomName: String?,
    val className: String?,
    val note: String?
)
```

---

## Bell Schedule

### `getNextRingingTime`

```kotlin
suspend fun getNextRingingTime(dateTime: LocalDateTime): RingingTime?
```

Returns the next bell time (start of lesson or end of lesson/break) after `dateTime`.

---

### `RingingTime`

```kotlin
data class RingingTime(val type: RingingType, val time: LocalTime)
```

### `RingingType`

```kotlin
enum class RingingType { LESSON, BREAK }
```

---

## Cloud Storage

### `cloudUpload`

```kotlin
suspend fun cloudUpload(file: File): EduCloudFile
```

Uploads a file to EduPage cloud storage.

---

### `EduCloudFile`

```kotlin
data class EduCloudFile(
    val fileId: String,
    val fileName: String,
    val uploadPath: String
)
```

---

## Parent Accounts

These methods are only available when logged in as a parent account. Calling them on a non-parent account throws `NotParentException`.

### `switchToChild`

```kotlin
suspend fun switchToChild(child: EduAccount)
suspend fun switchToChild(personId: Int)
```

Switches the active session to view a specific child's data.

---

### `switchToParent`

```kotlin
suspend fun switchToParent()
```

Switches back to the parent's own view.

---

## Custom Requests

### `customRequest`

```kotlin
suspend fun customRequest(
    url: String,
    method: String,
    data: String = "",
    headers: Map<String, String> = emptyMap()
): String
```

Sends a raw HTTP request using the authenticated session. Returns the raw response body as a string. Useful for accessing EduPage endpoints not yet covered by the library.

---

## Exceptions

All exceptions extend `Exception`.

| Class | Default Message | When Thrown |
|---|---|---|
| `BadCredentialsException` | `"Invalid username or password"` | Wrong username or password |
| `CaptchaException` | `"Captcha required"` | EduPage is requiring CAPTCHA |
| `NotLoggedInException` | `"You are not logged in"` | Session is missing or expired |
| `MissingDataException` | `"Missing data in response"` | Server returned incomplete data |
| `RequestError` | *(custom message required)* | HTTP request failed |
| `InsufficientPermissionsException` | *(custom message required)* | Account lacks permission for the operation |
| `UnknownServerError` | *(custom message required)* | Unrecognised server-side error |
| `FailedToParseGradeDataException` | *(custom message required)* | Grade response could not be parsed |
| `NotParentException` | `"This method can only be used on parent accounts"` | Parent-only method called on non-parent |
| `NotAnOnlineLessonException` | `"This lesson is not an online lesson"` | Online lesson operation on non-online lesson |
