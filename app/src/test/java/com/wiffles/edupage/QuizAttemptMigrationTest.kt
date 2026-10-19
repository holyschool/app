package com.wiffles.edupage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wiffles.edupage.data.QuizAttempt
import com.wiffles.edupage.data.sanitizedForStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test for the crash when opening the quiz screen or the session history sheet.
 *
 * Sessions written before the per-session `id` field existed are loaded by Gson with a null
 * `id` (Gson skips Kotlin defaults). The next `copy()` then threw
 * "Parameter specified as non-null is null: ... QuizAttempt.copy, parameter id".
 */
class QuizAttemptMigrationTest {

    @Test
    fun legacyAttemptWithoutIdGetsANonNullId() {
        // Exactly the on-disk shape: keyed by quiz id, no "id" field on the attempt itself.
        val legacy = """
            {"c2c50044-3a5d-4dd4-8e48-4dd0204cf8af":[
              {"score":5,"timestampMs":1791645775100,"total":10,"wrongQuestions":["a","b"]}
            ]}
        """.trimIndent()

        val type = object : TypeToken<Map<String, List<QuizAttempt>>>() {}.type
        val loaded: Map<String, List<QuizAttempt>> = Gson().fromJson(legacy, type)

        val attempt = loaded.values.single().single().sanitizedForStorage()

        assertTrue("a missing id must be filled in", attempt.id.isNotBlank())
        assertEquals(5, attempt.score)
        assertEquals(10, attempt.total)
        assertEquals(2, attempt.wrongQuestions.size)
    }

    @Test
    fun sanitizingIsIdempotentSoIdsSurviveResaves() {
        val json = """[{"score":1,"total":2,"timestampMs":1,"wrongQuestions":[]}]"""
        val type = object : TypeToken<List<QuizAttempt>>() {}.type
        val loaded: List<QuizAttempt> = Gson().fromJson(json, type)

        val once = loaded.single().sanitizedForStorage()
        assertEquals(once.id, once.sanitizedForStorage().id)
    }

    @Test
    fun anExistingIdIsKept() {
        val json = """[{"id":"keep-me","score":3,"total":3,"timestampMs":1,"wrongQuestions":[]}]"""
        val type = object : TypeToken<List<QuizAttempt>>() {}.type
        val loaded: List<QuizAttempt> = Gson().fromJson(json, type)
        assertEquals("keep-me", loaded.single().sanitizedForStorage().id)
    }
}