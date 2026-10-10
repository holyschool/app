package com.wiffles.edupage.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AiCredentialsStore
import com.wiffles.edupage.data.FlashcardDeck
import com.wiffles.edupage.data.FlashcardStore
import com.wiffles.edupage.data.MaterialLoadState
import com.wiffles.edupage.data.StudyMaterial
import com.wiffles.edupage.data.StudyMaterialLoader
import com.wiffles.edupage.network.AiService
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiFlashcardsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiService: AiService,
    private val aiCredentialsStore: AiCredentialsStore,
    private val flashcardStore: FlashcardStore,
    private val materialLoader: StudyMaterialLoader,
) : ViewModel() {

    sealed interface GenerateState {
        data object Idle : GenerateState
        data object Generating : GenerateState
        data class Error(val message: String) : GenerateState
    }

    val decks: StateFlow<List<FlashcardDeck>> = flashcardStore.decks

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    private val _examMaterials = MutableStateFlow<MaterialLoadState>(MaterialLoadState.Idle)
    val examMaterials: StateFlow<MaterialLoadState> = _examMaterials.asStateFlow()

    fun hasApiKey(): Boolean = aiCredentialsStore.current().apiKey.isNotBlank()

    private fun languageHint(): String? = aiCredentialsStore.studySettings.value.contextHint()

    fun homeworkMaterials(): List<StudyMaterial> = materialLoader.homework()

    fun loadExamMaterials(force: Boolean = false) {
        if (_examMaterials.value is MaterialLoadState.Loading) return
        if (!force && _examMaterials.value is MaterialLoadState.Loaded) return
        _examMaterials.value = MaterialLoadState.Loading
        viewModelScope.launch {
            try {
                _examMaterials.value = MaterialLoadState.Loaded(materialLoader.exams())
            } catch (e: Exception) {
                _examMaterials.value = MaterialLoadState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
                )
            }
        }
    }

    fun generate(
        topic: String,
        count: Int,
        material: String? = null,
        onCreated: (FlashcardDeck) -> Unit,
    ) {
        if (topic.isBlank()) return
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            _generateState.value = GenerateState.Error(context.getString(R.string.flash_no_key))
            return
        }
        viewModelScope.launch {
            _generateState.value = GenerateState.Generating
            try {
                val cards = aiService.generateFlashcards(config, topic.trim(), count, material, languageHint())
                val deck = FlashcardDeck(topic = topic.trim(), cards = cards)
                flashcardStore.add(deck)
                _generateState.value = GenerateState.Idle
                onCreated(deck)
            } catch (e: Exception) {
                _generateState.value = GenerateState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
                )
            }
        }
    }

    fun consumeError() {
        if (_generateState.value is GenerateState.Error) _generateState.value = GenerateState.Idle
    }

    fun delete(id: String) = flashcardStore.remove(id)

    fun recordProgress(id: String, knownIndices: Collection<Int>) =
        flashcardStore.recordProgress(id, knownIndices)
}