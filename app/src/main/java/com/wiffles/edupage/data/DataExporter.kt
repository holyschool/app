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
    private val quizAttemptStore: QuizAttemptStore,
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
        root.addProperty("schemaVersion", 3)

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
            q.addProperty("subjectTag", quiz.subjectTag)
            q.addProperty("isGrammar", quiz.isGrammar)
            val types = JsonArray()
            quiz.questionTypes.forEach { types.add(it.name.lowercase()) }
            q.add("questionTypes", types)
            val questions = JsonArray()
            quiz.questions.forEach { question ->
                val qo = JsonObject()
                qo.addProperty("question", question.question)
                qo.addProperty("type", question.type.name.lowercase())
                val options = JsonArray()
                question.options.forEach { options.add(it) }
                qo.add("options", options)
                qo.addProperty("correctIndex", question.correctIndex)
                qo.addProperty("answer", question.answer)
                qo.addProperty("explanation", question.explanation)
                questions.add(qo)
            }
            q.add("questions", questions)
            quizzes.add(q)
        }
        root.add("quizzes", quizzes)

        val sessions = JsonArray()
        quizAttemptStore.all.value.forEach { (quizId, attempts) ->
            attempts.forEach { attempt ->
                val s = JsonObject()
                s.addProperty("id", attempt.id)
                s.addProperty("quizId", quizId)
                s.addProperty("timestampMs", attempt.timestampMs)
                s.addProperty("subjectTag", attempt.subjectTag)
                s.addProperty("quizTopic", attempt.quizTopic)
                s.addProperty("score", attempt.score)
                s.addProperty("total", attempt.total)
                s.addProperty("durationMs", attempt.durationMs)
                val wrong = JsonArray()
                attempt.wrongQuestions.forEach { wrong.add(it) }
                s.add("wrongQuestions", wrong)
                val answers = JsonArray()
                attempt.answers.forEach { answer ->
                    val a = JsonObject()
                    a.addProperty("question", answer.question)
                    a.addProperty("given", answer.given)
                    a.addProperty("correctAnswer", answer.correctAnswer)
                    a.addProperty("correct", answer.correct)
                    a.addProperty("feedback", answer.feedback)
                    answers.add(a)
                }
                s.add("answers", answers)
                sessions.add(s)
            }
        }
        root.add("quizSessions", sessions)

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

