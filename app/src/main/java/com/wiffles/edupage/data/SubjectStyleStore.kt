package com.wiffles.edupage.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-subject visual customization (icon + color). Both fields are optional:
 * when [iconKey] is null the subject's initials are used, when [colorArgb] is
 * null a pastel color derived from the subject name is used.
 */
data class SubjectStyle(
    val iconKey: String? = null,
    val colorArgb: Int? = null,
) {
    val isEmpty: Boolean get() = iconKey == null && colorArgb == null
}

@Singleton
class SubjectStyleStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "edupage_subject_styles"
        private const val KEY_STYLES = "styles_json"
    }

    private val prefs by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    private val _styles = MutableStateFlow(load())
    val styles: StateFlow<Map<String, SubjectStyle>> = _styles.asStateFlow()

    fun get(subject: String): SubjectStyle? = _styles.value[subject]

    fun set(subject: String, style: SubjectStyle) {
        val name = subject.trim()
        if (name.isBlank()) return
        val updated = _styles.value.toMutableMap()
        if (style.isEmpty) updated.remove(name) else updated[name] = style
        _styles.value = updated
        save(updated)
    }

    fun clear(subject: String) {
        val updated = _styles.value.toMutableMap()
        updated.remove(subject)
        _styles.value = updated
        save(updated)
    }

    private fun load(): Map<String, SubjectStyle> {
        val raw = prefs.getString(KEY_STYLES, null) ?: return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, SubjectStyle>>() {}.type
            gson.fromJson<Map<String, SubjectStyle>>(raw, type) ?: emptyMap()
        }.getOrDefault(emptyMap())
    }

    private fun save(map: Map<String, SubjectStyle>) {
        runCatching { prefs.edit().putString(KEY_STYLES, gson.toJson(map)).apply() }
    }
}
