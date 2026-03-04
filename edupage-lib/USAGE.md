# edupage-lib Usage Guide

> Practical examples for the `com.edupage.api` Kotlin library.

---

## Installation

### Gradle (Kotlin DSL)

Add the `:edupage-lib` module to your project and declare the dependency:

```kotlin
// settings.gradle.kts
include(":edupage-lib")

// app/build.gradle.kts
dependencies {
    implementation(project(":edupage-lib"))

    // Required transitive deps (not re-exported as `api`)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
}
```

---

## Setup

Create a single `Edupage` instance. In an Android app, scope it as a singleton via your DI framework.

```kotlin
val edupage = Edupage(timeoutSeconds = 30L)
```

All network calls are `suspend` functions — call them from a coroutine or a `ViewModel`.

---

## Login

### Basic login (known subdomain)

```kotlin
val twoFactor = edupage.login(
    username = "jan.novak",
    password = "hunter2",
    subdomain = "zsmesto"           // → zsmesto.edupage.org
)

if (twoFactor != null) {
    // School has 2FA enabled — ask the user for their code
    twoFactor.verify(code = "123456")
}

println("Logged in: ${edupage.isLoggedIn}")
```

### Auto-login (subdomain unknown)

```kotlin
val twoFactor = edupage.loginAuto(
    username = "jan.novak",
    password = "hunter2"
)
twoFactor?.verify("123456")
```

### Restore from saved session

```kotlin
val sessionId = prefs.getString("session_id", null) ?: return
val subdomain  = prefs.getString("subdomain", null)  ?: return
val username   = prefs.getString("username", null)   ?: return

try {
    val edupage = Edupage.fromSessionId(sessionId, subdomain, username)
    println("Session restored, logged in: ${edupage.isLoggedIn}")
} catch (e: NotLoggedInException) {
    // Session expired — fall back to full login
}
```

### Persist the session after login

```kotlin
val host = "${edupage.subdomain}.edupage.org"
val sessionId = edupage.session.cookieJar.getSessionId(host)

prefs.edit()
    .putString("session_id", sessionId)
    .putString("subdomain", edupage.subdomain)
    .putString("username", edupage.username)
    .apply()
```

---

## Error Handling

Wrap calls in a `try/catch` to handle API errors gracefully:

```kotlin
try {
    val timetable = edupage.getMyTimetable(LocalDate.now())
    // ...
} catch (e: BadCredentialsException) {
    // Wrong username or password
} catch (e: NotLoggedInException) {
    // Session expired — redirect to login screen
} catch (e: CaptchaException) {
    // EduPage is showing a CAPTCHA; manual browser intervention needed
} catch (e: InsufficientPermissionsException) {
    // The logged-in account cannot access this resource
} catch (e: RequestError) {
    println("Network error: ${e.message}")
} catch (e: Exception) {
    println("Unexpected: ${e.message}")
}
```

---

## Timetable

### Get your own timetable

```kotlin
val today = LocalDate.now()
val timetable = edupage.getMyTimetable(today)

timetable?.lessons?.forEach { lesson ->
    val start   = lesson.startTime?.toString() ?: "?"
    val end     = lesson.endTime?.toString()   ?: "?"
    val subject = lesson.subject?.name         ?: "Unknown"
    val room    = lesson.classrooms?.firstOrNull()?.name ?: "-"
    println("$start–$end  $subject  ($room)")
}
```

**Sample output:**
```
08:00–08:45  Mathematics  (A101)
09:00–09:45  English  (B202)
10:00–10:45  Physics  (Lab1)
```

---

### Check current / next lesson

```kotlin
val now = LocalTime.now()
val timetable = edupage.getMyTimetable(LocalDate.now())

val current = timetable?.getLessonAtTime(now)
val next    = timetable?.getNextLessonAtTime(now)

println("Now:  ${current?.subject?.name ?: "No lesson"}")
println("Next: ${next?.subject?.name    ?: "Nothing more today"}")
```

---

### Check for online lessons

```kotlin
timetable?.lessons?.forEach { lesson ->
    if (lesson.isOnlineLesson()) {
        println("Online: ${lesson.subject?.name} → ${lesson.onlineLessonLink}")
    }
}
```

---

### Get a teacher's timetable

```kotlin
val teachers = edupage.getTeachers() ?: emptyList()
val smith    = teachers.first { it.name == "John Smith" }
val tt       = edupage.getTimetable(target = smith, date = LocalDate.now())

tt?.lessons?.forEach { println(it.subject?.name) }
```

---

### Get a class timetable

```kotlin
val classes = edupage.getClasses() ?: emptyList()
val form3a  = classes.first { it.name == "3A" }
val tt      = edupage.getTimetable(target = form3a, date = LocalDate.now())
```

---

## Grades

### All grades for the current year

```kotlin
val grades = edupage.getGrades()

grades.forEach { grade ->
    val score   = grade.gradeN ?: "—"
    val subject = grade.subjectName ?: "Unknown subject"
    val date    = grade.date.toLocalDate()
    println("$date  $subject  $score")
}
```

### Filter numeric vs verbal grades

```kotlin
val numeric = grades.filter { !it.verbal }
val verbal  = grades.filter {  it.verbal }
```

### Grades for a specific term

```kotlin
val firstTerm = edupage.getGradesForTerm(year = 2024, term = Term.FIRST)
val avg = firstTerm
    .mapNotNull { (it.gradeN as? Double) }
    .average()
println("Term average: %.2f".format(avg))
```

---

## People

### List all students

```kotlin
val students = edupage.getStudents() ?: emptyList()
students.forEach { println("${it.name} (class ${it.classId})") }
```

### Lightweight student list

```kotlin
val skeletons = edupage.getAllStudents() ?: emptyList()
// Faster — returns only personId, nameShort, classId
skeletons.forEach { println(it.nameShort) }
```

### List all teachers

```kotlin
val teachers = edupage.getTeachers() ?: emptyList()
teachers.forEach { println(it.name) }
```

---

## School Structure

```kotlin
val classes    = edupage.getClasses()    ?: emptyList()
val classrooms = edupage.getClassrooms() ?: emptyList()
val subjects   = edupage.getSubjects()   ?: emptyList()

println("Classes:    ${classes.joinToString { it.name ?: "?" }}")
println("Classrooms: ${classrooms.joinToString { it.name ?: "?" }}")
println("Subjects:   ${subjects.joinToString { it.name ?: "?" }}")
```

---

## Notifications

### Fetch recent notifications

```kotlin
val events = edupage.getNotifications()

events.forEach { event ->
    println("[${event.timestamp}] ${event.authorName}: ${event.title}")
}
```

### Notification history since a date

```kotlin
val since  = LocalDate.now().minusDays(7)
val events = edupage.getNotificationHistory(dateFrom = since)
```

---

## Messages

### Send to one person

```kotlin
val teachers = edupage.getTeachers() ?: emptyList()
val recipient = teachers.first { it.name == "Jane Doe" }

val timelineId = edupage.sendMessage(
    recipient = recipient,
    body = "Hello, I'll be absent tomorrow."
)
println("Message sent, timeline ID: $timelineId")
```

### Send to multiple people

```kotlin
val students = edupage.getStudents() ?: emptyList()
val class3a  = students.filter { it.classId == 42 }

edupage.sendMessage(
    recipients = class3a,
    body = "Reminder: test on Friday."
)
```

---

## Meals (Canteen)

```kotlin
val meals = edupage.getMeals(date = LocalDate.now())

meals?.meals?.forEach { meal ->
    val status = when {
        meal.isOrdered  -> "ordered"
        meal.canOrder   -> "available"
        else            -> "unavailable"
    }
    val allergens = meal.allergens?.joinToString(", ") ?: "none"
    println("${meal.name}  [$status]  allergens: $allergens")
}
```

---

## Substitutions

### Missing teachers today

```kotlin
val missing = edupage.getMissingTeachers(LocalDate.now())
missing.forEach { println("Absent: ${it.name}") }
```

### Timetable changes

```kotlin
val changes = edupage.getTimetableChanges(LocalDate.now())
changes.forEach { change ->
    println(
        "Period ${change.lessonIndex}: ${change.type} — " +
        "${change.subjectName} in ${change.classroomName}"
    )
}
```

---

## Bell Schedule

```kotlin
val now  = LocalDateTime.now()
val bell = edupage.getNextRingingTime(now)

if (bell != null) {
    val label = when (bell.type) {
        RingingType.LESSON -> "Lesson starts"
        RingingType.BREAK  -> "Break starts"
    }
    println("$label at ${bell.time}")
}
```

---

## Cloud Upload

```kotlin
val file      = File("/path/to/homework.pdf")
val uploaded  = edupage.cloudUpload(file)

println("Uploaded: ${uploaded.fileName}")
println("Path:     ${uploaded.uploadPath}")
```

---

## Parent Accounts

```kotlin
val students = edupage.getStudents() ?: emptyList()
val myChild  = students.first { it.name == "Tomáš Novák" }

// Switch to child's view
edupage.switchToChild(myChild)
val childTimetable = edupage.getMyTimetable(LocalDate.now())

// Switch back to parent view
edupage.switchToParent()
```

---

## Custom Requests

For endpoints not covered by the library, use `customRequest` to send arbitrary requests with the active authenticated session:

```kotlin
val response = edupage.customRequest(
    url    = "https://myschool.edupage.org/gcall",
    method = "POST",
    data   = """{"action":"getCalendar"}""",
    headers = mapOf("Content-Type" to "application/json")
)

val json = com.google.gson.JsonParser.parseString(response).asJsonObject
```

---

## Android / ViewModel Pattern

```kotlin
@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load(LocalDate.now()) }

    fun load(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val timetable = edupage.getMyTimetable(date)
                _uiState.value = UiState.Success(timetable?.lessons ?: emptyList())
            } catch (e: NotLoggedInException) {
                _uiState.value = UiState.SessionExpired
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        credentialStore.clear()
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }
}
```

---

## Complete Login Flow (Android)

```kotlin
// SplashViewModel.kt
init {
    viewModelScope.launch {
        val savedId   = credentialStore.sessionId
        val subdomain = credentialStore.subdomain
        val username  = credentialStore.username

        if (savedId != null && subdomain != null && username != null) {
            try {
                Edupage.fromSessionId(savedId, subdomain, username)
                    .also { /* swap in the singleton if needed */ }
                onAutoLoginSuccess()
                return@launch
            } catch (_: NotLoggedInException) { /* fall through */ }
        }

        // No valid saved session
        onAutoLoginFailed(
            username  = credentialStore.username  ?: "",
            subdomain = credentialStore.subdomain ?: ""
        )
    }
}

// LoginViewModel.kt
fun login(username: String, password: String, subdomain: String) {
    viewModelScope.launch {
        try {
            val twoFactor = edupage.login(username, password, subdomain)
            twoFactor?.verify(userEnteredCode)

            // Persist session
            val host = "$subdomain.edupage.org"
            val sid  = edupage.session.cookieJar.getSessionId(host)
            credentialStore.save(sid, subdomain, username)

            onLoginSuccess()
        } catch (e: BadCredentialsException) {
            _error.value = "Wrong username or password"
        } catch (e: CaptchaException) {
            _error.value = "Please log in via the browser first"
        }
    }
}
```
