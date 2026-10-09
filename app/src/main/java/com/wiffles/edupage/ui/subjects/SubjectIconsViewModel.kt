package com.wiffles.edupage.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.google.gson.JsonParser
import com.wiffles.edupage.data.AiCredentialsStore
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.SubjectStyle
import com.wiffles.edupage.data.SubjectStyleStore
import com.wiffles.edupage.network.AiMessage
import com.wiffles.edupage.network.AiService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectInfo(
    val name: String,
    val shortName: String? = null,
    val teachers: List<String> = emptyList(),
)

sealed interface SubjectsState {
    object Loading : SubjectsState
    data class Ready(
        val mine: List<SubjectInfo>,
        val others: List<SubjectInfo>,
    ) : SubjectsState
    data class Error(val message: String) : SubjectsState
}

sealed interface SubjectAiState {
    object Idle : SubjectAiState
    object Running : SubjectAiState
    data class Done(val count: Int) : SubjectAiState
    data class Error(val message: String) : SubjectAiState
}

@HiltViewModel
class SubjectIconsViewModel @Inject constructor(
    private val edupage: Edupage,
    private val store: SubjectStyleStore,
    private val prefs: AppPreferences,
    private val aiService: AiService,
    private val aiStore: AiCredentialsStore,
) : ViewModel() {

    companion object {
        private const val SYSTEM_PROMPT =
            "You match school subjects to the most fitting icon from a fixed list. " +
                "You always answer with a single JSON object and nothing else."
    }

    val styles: StateFlow<Map<String, SubjectStyle>> = store.styles

    val enabled: StateFlow<Boolean> = prefs.subjectIconsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, prefs.subjectIconsEnabled)

    /** True when both experimental features are on and an AI key is configured. */
    val aiAvailable: StateFlow<Boolean> = combine(
        prefs.subjectIconsEnabledFlow,
        prefs.aiQuizEnabledFlow,
        aiStore.config,
    ) { icons, ai, config -> icons && ai && config.apiKey.isNotBlank() }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            prefs.subjectIconsEnabled && prefs.aiQuizEnabled && aiStore.current().apiKey.isNotBlank(),
        )

    private val _subjectsState = MutableStateFlow<SubjectsState>(SubjectsState.Loading)
    val subjectsState: StateFlow<SubjectsState> = _subjectsState.asStateFlow()

    private val _aiState = MutableStateFlow<SubjectAiState>(SubjectAiState.Idle)
    val aiState: StateFlow<SubjectAiState> = _aiState.asStateFlow()

    private var loadStarted = false

    /** Loads the subject list once, on demand (e.g. when the editor sheet opens). */
    fun ensureLoaded() {
        if (loadStarted) return
        loadSubjects()
    }

    fun setStyle(subject: String, style: SubjectStyle) = store.set(subject, style)

    /**
     * Loads the subjects the user actually has (from grades + today's timetable)
     * first, then the remaining school subjects, so the editor can offer both.
     */
    fun loadSubjects() {
        loadStarted = true
        viewModelScope.launch {
            _subjectsState.value = SubjectsState.Loading
            _subjectsState.value = computeSubjects()
        }
    }

    private suspend fun computeSubjects(): SubjectsState {
        return try {
            val allSubjects = runCatching { edupage.getSubjects() }.getOrNull().orEmpty()
            val grades = runCatching { edupage.getGrades() }.getOrDefault(emptyList())
            val timetable = runCatching { edupage.getMyTimetable(LocalDate.now()) }.getOrNull()

            val shortByName = LinkedHashMap<String, String?>()
            allSubjects.forEach { subject ->
                val name = subject.name?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
                if (!shortByName.containsKey(name)) shortByName[name] = subject.shortName
            }

            val teachersBySubject = LinkedHashMap<String, MutableSet<String>>()
            val mine = LinkedHashSet<String>()

            timetable?.lessons?.forEach { lesson ->
                val name = lesson.subject?.name?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
                mine.add(name)
                if (!shortByName.containsKey(name)) shortByName[name] = lesson.subject?.shortName
                val teachers = teachersBySubject.getOrPut(name) { linkedSetOf() }
                lesson.teachers?.mapNotNull { it.name }?.forEach { teachers.add(it) }
            }

            grades.forEach { grade ->
                val name = grade.subjectName?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
                mine.add(name)
            }

            mine.addAll(store.styles.value.keys)

            val mineNames = mine.sorted()
            val mineSet = mineNames.toSet()
            val otherNames = allSubjects
                .mapNotNull { it.name?.trim()?.takeIf { name -> name.isNotBlank() } }
                .distinct()
                .filter { it !in mineSet }
                .sorted()

            fun infoFor(name: String) = SubjectInfo(
                name = name,
                shortName = shortByName[name],
                teachers = teachersBySubject[name]?.toList().orEmpty().sorted(),
            )

            SubjectsState.Ready(
                mine = mineNames.map { infoFor(it) },
                others = otherNames.map { infoFor(it) },
            )
        } catch (e: Exception) {
            SubjectsState.Error(e.message ?: "error")
        }
    }

    private suspend fun ensureSubjectsReady(): SubjectsState {
        (_subjectsState.value as? SubjectsState.Ready)?.let { return it }
        loadStarted = true
        _subjectsState.value = SubjectsState.Loading
        val state = computeSubjects()
        _subjectsState.value = state
        return state
    }

    /**
     * Asks the configured AI to assign an icon to every known subject in one go.
     * Runs only when subject icons and the AI features are both enabled.
     */
    fun autoAssignIcons() {
        if (_aiState.value is SubjectAiState.Running) return
        viewModelScope.launch {
            _aiState.value = SubjectAiState.Running
            try {
                val config = aiStore.current()
                if (config.apiKey.isBlank()) {
                    throw IllegalStateException("Add an AI API key in Settings first")
                }
                val ready = ensureSubjectsReady() as? SubjectsState.Ready
                    ?: throw IllegalStateException("No subjects to assign")
                val subjectNames = (ready.mine + ready.others)
                    .map { it.name.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (subjectNames.isEmpty()) throw IllegalStateException("No subjects to assign")

                val text = aiService.complete(
                    config,
                    listOf(
                        AiMessage("system", SYSTEM_PROMPT),
                        AiMessage("user", buildPrompt(subjectNames)),
                    ),
                    jsonMode = true,
                )

                val mapping = parseMapping(text)
                if (mapping.isEmpty()) throw IllegalStateException("The AI returned no usable mapping")

                val validSubjects = subjectNames.toSet()
                var applied = 0
                mapping.forEach { (name, key) ->
                    val subject = validSubjects.firstOrNull { it.equals(name, ignoreCase = true) }
                        ?: return@forEach
                    if (SubjectIconCatalog.byKey(key) == null) return@forEach
                    val existing = store.get(subject) ?: SubjectStyle()
                    store.set(subject, existing.copy(iconKey = key))
                    applied++
                }
                if (applied == 0) throw IllegalStateException("The AI returned no usable mapping")
                _aiState.value = SubjectAiState.Done(applied)
            } catch (e: Exception) {
                _aiState.value = SubjectAiState.Error(e.message ?: "AI request failed")
            }
        }
    }

    fun resetAiState() {
        _aiState.value = SubjectAiState.Idle
    }

    private fun buildPrompt(subjects: List<String>): String = buildString {
        append("Assign exactly one icon key to each school subject below.\n\n")
        append("Available icons (key — meaning):\n")
        SubjectIconCatalog.icons.forEach {
            append("- ").append(it.key).append(" — ").append(it.description).append("\n")
        }
        append("\nSubjects:\n")
        subjects.forEach { append("- ").append(it).append("\n") }
        append(
            "\nReturn JSON only: an object whose keys are the subject names exactly as " +
                "written above and whose values are icon keys. Every subject must get one " +
                "key; use \"ideas\" if nothing fits."
        )
    }

    private fun parseMapping(text: String): Map<String, String> {
        val raw = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
            .let {
                if (it.startsWith("{")) it
                else {
                    val start = it.indexOf('{')
                    val end = it.lastIndexOf('}')
                    if (start in 0 until end) it.substring(start, end + 1) else it
                }
            }
        val obj = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            ?: return emptyMap()
        val out = LinkedHashMap<String, String>()
        obj.entrySet().forEach { (entryKey, value) ->
            val key = runCatching { value.asString }.getOrNull()?.trim().orEmpty()
            if (entryKey.isNotBlank() && key.isNotBlank()) out[entryKey] = key
        }
        return out
    }
}
