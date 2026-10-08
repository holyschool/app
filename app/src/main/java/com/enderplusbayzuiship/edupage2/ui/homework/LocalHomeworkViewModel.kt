package com.enderplusbayzuiship.edupage2.ui.homework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.enderplusbayzuiship.edupage2.data.HomeworkItem
import com.enderplusbayzuiship.edupage2.data.LocalHomeworkStore
import com.enderplusbayzuiship.edupage2.ui.widgets.WidgetUpdater
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _items = MutableStateFlow<List<HomeworkItem>>(store.getAll())
    val items: StateFlow<List<HomeworkItem>> = _items.asStateFlow()

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

