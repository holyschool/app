package com.enderplusbayzuiship.edupage2.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AiCredentialsStore
import com.enderplusbayzuiship.edupage2.data.ChatMessage
import com.enderplusbayzuiship.edupage2.data.ChatStore
import com.enderplusbayzuiship.edupage2.data.LocalHomeworkStore
import com.enderplusbayzuiship.edupage2.network.AiMessage
import com.enderplusbayzuiship.edupage2.network.AiService
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiService: AiService,
    private val aiCredentialsStore: AiCredentialsStore,
    private val chatStore: ChatStore,
    private val homeworkStore: LocalHomeworkStore,
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatStore.flow

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private val basePrompt = """
        You are a friendly, patient study assistant built into a school app.
        Help students understand their schoolwork: explain concepts step by step,
        give examples, summarise, and create practice questions on request.
        Prefer clear, age-appropriate language. If a question is unrelated to learning,
        gently steer back to studying. Be concise but genuinely educational.
        If you are unsure, say so rather than inventing facts.
    """.trimIndent()

    /** Adds a short, local-only summary of pending homework so answers are more relevant. */
    private fun systemPrompt(): String {
        val pending = homeworkStore.getAll()
            .filter { !it.done && it.title.isNotBlank() }
            .take(8)
            .joinToString("; ") { item ->
                if (item.subject.isNotBlank()) "${item.title} (${item.subject})" else item.title
            }
        return buildString {
            append(basePrompt)
            if (pending.isNotBlank()) {
                append("\n\nThe student currently has these homework items they could ask about: ")
                append(pending)
                append('.')
            }
        }
    }

    fun hasApiKey(): Boolean = aiCredentialsStore.current().apiKey.isNotBlank()

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isBlank() || _thinking.value) return
        if (!hasApiKey()) {
            chatStore.add(ChatMessage(role = "assistant", content = context.getString(R.string.chat_no_key), error = true))
            return
        }
        chatStore.add(ChatMessage(role = "user", content = prompt))
        viewModelScope.launch {
            _thinking.value = true
            val history = chatStore.all()
                .filter { !it.error }
                .takeLast(20)
                .map { AiMessage(it.role, it.content) }
            val payload = listOf(AiMessage("system", systemPrompt())) + history
            try {
                val reply = aiService.complete(aiCredentialsStore.current(), payload, jsonMode = false)
                chatStore.add(ChatMessage(role = "assistant", content = reply.trim()))
            } catch (e: Exception) {
                val message = if (e.isNetworkError()) context.getString(R.string.network_error)
                else e.message ?: context.getString(R.string.cloud_error_loading)
                chatStore.add(ChatMessage(role = "assistant", content = message, error = true))
            } finally {
                _thinking.value = false
            }
        }
    }

    fun clear() = chatStore.clear()
}