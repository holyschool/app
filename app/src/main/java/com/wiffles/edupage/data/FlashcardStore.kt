package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

data class Flashcard(
    val front: String,
    val back: String,
    val hint: String? = null,
)

data class FlashcardDeck(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val cards: List<Flashcard> = emptyList(),
    /** Indices of cards the learner has marked as known in the last study session. */
    val knownIndices: List<Int> = emptyList(),
    val lastStudiedMs: Long = 0L,
    val reviewCount: Int = 0,
)

@Singleton
class FlashcardStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "ai_flashcards.json"
        private const val TAG = "FlashcardStore"
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val items = CopyOnWriteArrayList<FlashcardDeck>().apply {
        loadFromDisk()?.let { addAll(it) }
    }

    private val _decks = MutableStateFlow(all())
    val decks: StateFlow<List<FlashcardDeck>> = _decks.asStateFlow()

    fun all(): List<FlashcardDeck> = items.sortedByDescending { it.createdAtMs }

    fun add(deck: FlashcardDeck) {
        items.add(0, deck)
        publish()
    }

    fun remove(id: String) {
        items.removeIf { it.id == id }
        publish()
    }

    /** Records which cards were marked known and bumps the review counter. */
    fun recordProgress(id: String, knownIndices: Collection<Int>) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = items[idx]
        items[idx] = current.copy(
            knownIndices = knownIndices.toList(),
            lastStudiedMs = System.currentTimeMillis(),
            reviewCount = current.reviewCount + 1,
        )
        publish()
    }

    private fun publish() {
        persist()
        _decks.value = all()
    }

    private fun persist() {
        val snapshot = items.toList()
        ioScope.launch {
            runCatching {
                file.writeText(gson.toJson(snapshot))
            }.onFailure { Log.e(TAG, "failed to persist flashcards", it) }
        }
    }

    private fun loadFromDisk(): List<FlashcardDeck>? = runCatching {
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<FlashcardDeck>>() {}.type
        gson.fromJson<List<FlashcardDeck>>(file.readText(), type)
    }.getOrElse {
        Log.e(TAG, "failed to load flashcards", it)
        null
    }
}