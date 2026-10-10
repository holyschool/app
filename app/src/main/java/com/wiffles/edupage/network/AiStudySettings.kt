package com.wiffles.edupage.network

enum class AiAnswerStyle(val key: String) {
    CONCISE("concise"),
    BALANCED("balanced"),
    DETAILED("detailed");

    val promptHint: String
        get() = when (this) {
            CONCISE -> "Keep every answer as short as possible: a few sentences or a compact list."
            BALANCED -> "Give clear, moderately detailed answers with the key steps."
            DETAILED -> "Give thorough, step-by-step explanations with worked examples."
        }

    companion object {
        fun fromKey(key: String?): AiAnswerStyle =
            entries.firstOrNull { it.key == key } ?: BALANCED
    }
}

enum class AiStudyTone(val key: String) {
    FRIENDLY("friendly"),
    FORMAL("formal"),
    ENCOURAGING("encouraging");

    val promptHint: String
        get() = when (this) {
            FRIENDLY -> "Use a warm, friendly and casual tone."
            FORMAL -> "Use a precise, formal and academic tone."
            ENCOURAGING -> "Be encouraging and motivating; praise effort and progress."
        }

    companion object {
        fun fromKey(key: String?): AiStudyTone =
            entries.firstOrNull { it.key == key } ?: FRIENDLY
    }
}

enum class AiStudyLanguage(val key: String) {
    AUTO("auto"),
    ENGLISH("en"),
    CZECH("cs"),
    SLOVAK("sk");

    /** Instruction appended to the prompt, or null to let the model mirror the question. */
    val promptHint: String?
        get() = when (this) {
            AUTO -> null
            ENGLISH -> "Always answer in English, regardless of the question's language."
            CZECH -> "Odpovídej vždy česky, bez ohledu na jazyk otázky."
            SLOVAK -> "Odpovedaj vždy po slovensky, bez ohľadu na jazyk otázky."
        }

    companion object {
        fun fromKey(key: String?): AiStudyLanguage =
            entries.firstOrNull { it.key == key } ?: AUTO
    }
}

data class AiStudySettings(
    val customInstructions: String = "",
    val answerStyle: AiAnswerStyle = AiAnswerStyle.BALANCED,
    val tone: AiStudyTone = AiStudyTone.FRIENDLY,
    val language: AiStudyLanguage = AiStudyLanguage.AUTO,
    val includeHomework: Boolean = true,
    val studentClass: String = "",
) {
    /** Level instruction based on the class the student says they are in, or null. */
    fun classHint(): String? = studentClass.trim().takeIf { it.isNotBlank() }?.let {
        "The student is in $it. Match the difficulty, vocabulary and depth of your " +
            "explanations and questions to that level."
    }

    /** Class level plus language instruction, for the quiz/flashcard generators. */
    fun contextHint(): String? =
        listOfNotNull(classHint(), language.promptHint).joinToString(" ").takeIf { it.isNotBlank() }

    fun promptBlock(): String = buildString {
        append(answerStyle.promptHint)
        append(' ').append(tone.promptHint)
        language.promptHint?.let { append(' ').append(it) }
        classHint()?.let { append(' ').append(it) }
        if (customInstructions.isNotBlank()) {
            append("\n\nAdditional instructions from the student (follow them):\n")
            append(customInstructions.trim())
        }
    }
}