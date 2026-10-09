package com.wiffles.edupage.ui.homework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.wiffles.edupage.data.HomeworkItem
import com.wiffles.edupage.data.LocalHomeworkStore
import com.wiffles.edupage.ui.widgets.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalHomeworkViewModel @Inject constructor(
    private val store: LocalHomeworkStore,
    private val edupage: com.edupage.api.Edupage,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _items = MutableStateFlow<List<HomeworkItem>>(store.getAll())
    val items: StateFlow<List<HomeworkItem>> = _items.asStateFlow()

    private val _subjects = MutableStateFlow<List<String>>(emptyList())
    val subjects: StateFlow<List<String>> = _subjects.asStateFlow()

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        viewModelScope.launch {
            val fetched = runCatching { edupage.getSubjects() }
                .getOrNull()
                ?.mapNotNull { it.name?.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                .orEmpty()
            if (fetched.isNotEmpty()) _subjects.value = fetched
        }
    }

    private fun refreshWidgets() {
        viewModelScope.launch { WidgetUpdater.refreshAll(context) }
    }

    fun addOrUpdate(item: HomeworkItem) {
        viewModelScope.launch {
            store.addOrUpdate(item)
            _items.update { store.getAll() }
            refreshWidgets()
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            store.remove(id)
            _items.update { store.getAll() }
            refreshWidgets()
        }
    }

    fun toggleDone(id: String) {
        viewModelScope.launch {
            store.toggleDone(id)
            _items.update { store.getAll() }
            refreshWidgets()
        }
    }

    fun reload() {
        _items.update { store.getAll() }
    }
}

