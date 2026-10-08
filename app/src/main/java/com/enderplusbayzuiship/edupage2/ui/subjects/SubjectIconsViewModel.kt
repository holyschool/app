package com.enderplusbayzuiship.edupage2.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.SubjectStyle
import com.enderplusbayzuiship.edupage2.data.SubjectStyleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class SubjectIconsViewModel @Inject constructor(
    private val edupage: Edupage,
    private val store: SubjectStyleStore,
    private val prefs: AppPreferences,
) : ViewModel() {

    val styles: StateFlow<Map<String, SubjectStyle>> = store.styles

    val enabled: StateFlow<Boolean> = prefs.subjectIconsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, prefs.subjectIconsEnabled)

    private val _subjectsState = MutableStateFlow<SubjectsState>(SubjectsState.Loading)
    val subjectsState: StateFlow<SubjectsState> = _subjectsState.asStateFlow()

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
            try {
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

                _subjectsState.value = SubjectsState.Ready(
                    mine = mineNames.map { infoFor(it) },
                    others = otherNames.map { infoFor(it) },
                )
            } catch (e: Exception) {
                _subjectsState.value = SubjectsState.Error(e.message ?: "error")
            }
        }
    }
}
