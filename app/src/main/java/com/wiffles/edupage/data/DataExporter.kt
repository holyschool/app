package com.wiffles.edupage.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExporter @Inject constructor(
    private val homeworkStore: LocalHomeworkStore,
    private val quizStore: AiQuizStore,
    private val flashcardStore: FlashcardStore,
) {

    fun exportAsJson(): String {
        val root = JsonObject()
        root.addProperty("app", "Edupage2")
        root.addProperty("exportedAt", System.currentTimeMillis())
        root.addProperty(
            "exportedAtIso",
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now())
        )
        root.addProperty("schemaVersion", 2)

        val homework = JsonArray()
        homeworkStore.getAll().forEach { item ->
            val o = JsonObject()
            o.addProperty("id", item.id)
            o.addProperty("title", item.title)
            o.addProperty("date", item.date)
            o.addProperty("subject", item.subject)
            o.addProperty("notes", item.notes)
            o.addProperty("done", item.done)
            o.addProperty("createdAtMs", item.createdAtMs)
            homework.add(o)
        }
        root.add("homework", homework)

        val quizzes = JsonArray()
        quizStore.quizzes.value.forEach { quiz ->
            val q = JsonObject()
            q.addProperty("id", quiz.id)
            q.addProperty("topic", quiz.topic)
            q.addProperty("difficulty", quiz.difficulty)
            q.addProperty("createdAtMs", quiz.createdAtMs)
            q.addProperty("bestScore", quiz.bestScore)
            q.addProperty("bestTotal", quiz.bestTotal)
            val questions = JsonArray()
            quiz.questions.forEach { question ->
                val qo = JsonObject()
                qo.addProperty("question", question.question)
                val options = JsonArray()
                question.options.forEach { options.add(it) }
                qo.add("options", options)
                qo.addProperty("correctIndex", question.correctIndex)
                qo.addProperty("explanation", question.explanation)
                questions.add(qo)
            }
            q.add("questions", questions)
            quizzes.add(q)
        }
        root.add("quizzes", quizzes)

        val decks = JsonArray()
        flashcardStore.all().forEach { deck ->
            val d = JsonObject()
            d.addProperty("id", deck.id)
            d.addProperty("topic", deck.topic)
            d.addProperty("createdAtMs", deck.createdAtMs)
            d.addProperty("lastStudiedMs", deck.lastStudiedMs)
            d.addProperty("reviewCount", deck.reviewCount)
            val cards = JsonArray()
            deck.cards.forEach { card ->
                val c = JsonObject()
                c.addProperty("front", card.front)
                c.addProperty("back", card.back)
                c.addProperty("hint", card.hint)
                cards.add(c)
            }
            d.add("cards", cards)
            val known = JsonArray()
            deck.knownIndices.forEach { known.add(it) }
            d.add("knownIndices", known)
            decks.add(d)
        }
        root.add("flashcards", decks)

        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }
}

