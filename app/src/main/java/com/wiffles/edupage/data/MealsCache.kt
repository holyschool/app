package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.edupage.api.model.Meal
import com.edupage.api.model.MealOrderInfo
import com.edupage.api.model.Meals
import com.edupage.api.model.MenuChoice
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private data class CachedMeal(
    val mealId: String?,
    val name: String,
    val canOrder: Boolean,
    val isOrdered: Boolean,
    val allergens: List<String>?,
    val weight: String?,
)

private data class CachedMealRating(
    val qualityAverage: Double? = null,
    val qualityCount: Int? = null,
    val quantityAverage: Double? = null,
    val quantityCount: Int? = null,
)

private data class CachedMenuChoice(
    val letter: String,
    val name: String,
    val allergens: List<String>?,
    val weight: String?,
    val rating: CachedMealRating? = null,
)

private data class CachedMealOrderInfo(
    val mealTypeIndex: String,
    val title: String,
    val orderedChoice: String?,
    val availableChoices: List<CachedMenuChoice>,
    val canChangeUntil: String?,
    val servedFrom: String?,
    val servedTo: String?,
    val amountOfFoods: Int? = null,
)

private data class MealsCacheEntry(
    val fetchedAtMs: Long,
    val meals: List<CachedMeal>,
    val credit: String? = null,
    val boarderId: String? = null,
    val mealOrderInfo: List<CachedMealOrderInfo>? = null,
)

private data class MealsCacheFile(
    val entries: Map<String, MealsCacheEntry> = emptyMap(),
)

/** Pre-multi-day cache format: a single date stored at the root of the file. */
private data class LegacyMealsCacheFile(
    val date: String,
    val fetchedAtMs: Long,
    val meals: List<CachedMeal>,
    val credit: String? = null,
    val boarderId: String? = null,
    val mealOrderInfo: List<CachedMealOrderInfo>? = null,
)

/**
 * Persistent, multi-day meals cache so the canteen can be browsed (and orders reviewed) offline.
 * Kept in memory to make repeated reads across date navigation cheap.
 */
@Singleton
class MealsCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "meals_cache.json"
        private const val TAG = "MealsCache"
        private const val STALE_AFTER_HOURS = 2L
        private const val KEEP_PAST_DAYS = 21L
        private const val KEEP_FUTURE_DAYS = 42L
    }

    private val gson = Gson()
    private val lock = Any()

    @Volatile
    private var memory: Map<String, MealsCacheEntry>? = null

    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun save(meals: Meals) {
        Log.i(TAG, "saving ${meals.meals.size} meals for ${meals.date}, credit=${meals.credit}")
        val cached = meals.meals.map { m ->
            CachedMeal(
                mealId    = m.mealId,
                name      = m.name,
                canOrder  = m.canOrder,
                isOrdered = m.isOrdered,
                allergens = m.allergens,
                weight    = m.weight,
            )
        }
        val cachedOrderInfo = meals.mealOrderInfo?.map { oi ->
            CachedMealOrderInfo(
                mealTypeIndex = oi.mealTypeIndex,
                title = oi.title,
                orderedChoice = oi.orderedChoice,
                availableChoices = oi.availableChoices.map { c ->
                    CachedMenuChoice(
                        letter = c.letter,
                        name = c.name,
                        allergens = c.allergens,
                        weight = c.weight,
                        rating = c.rating?.let {
                            CachedMealRating(
                                qualityAverage = it.qualityAverage,
                                qualityCount = it.qualityCount,
                                quantityAverage = it.quantityAverage,
                                quantityCount = it.quantityCount,
                            )
                        },
                    )
                },
                canChangeUntil = oi.canChangeUntil,
                servedFrom = oi.servedFrom,
                servedTo = oi.servedTo,
                amountOfFoods = oi.amountOfFoods,
            )
        }
        val entry = MealsCacheEntry(
            fetchedAtMs   = System.currentTimeMillis(),
            meals         = cached,
            credit        = meals.credit,
            boarderId     = meals.boarderId,
            mealOrderInfo = cachedOrderInfo,
        )
        synchronized(lock) {
            val current = memory ?: readFromDisk().also { memory = it }
            val updated = prune(current + (meals.date.toString() to entry))
            memory = updated
            write(updated)
        }
    }

    /** Fresh cache for [date] (used as the normal cache-first render). */
    fun load(date: String = LocalDate.now().toString()): Meals? = read(date) { entry ->
        val ageHours = (System.currentTimeMillis() - entry.fetchedAtMs) / 3_600_000L
        ageHours < STALE_AFTER_HOURS
    }

    /** Any cache for [date], even expired — this is what keeps meals usable offline. */
    fun loadLenient(date: String = LocalDate.now().toString()): Meals? = read(date) { true }

    fun isFresh(date: LocalDate): Boolean = load(date.toString()) != null

    fun clear() {
        synchronized(lock) {
            memory = emptyMap()
            file.delete()
        }
    }

    // ------------------------------------------------------------------ internals

    private inline fun read(date: String, accept: (MealsCacheEntry) -> Boolean): Meals? {
        val entry = snapshot()[date] ?: return null
        if (!accept(entry)) return null
        return try {
            Meals(
                date = LocalDate.parse(date),
                meals = entry.meals.map { it.toMeal() },
                mealOrderInfo = entry.mealOrderInfo?.map { it.toMealOrderInfo() },
                credit = entry.credit,
                boarderId = entry.boarderId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "failed to read cache for $date: ${e.message}", e)
            null
        }
    }

    private fun snapshot(): Map<String, MealsCacheEntry> {
        memory?.let { return it }
        synchronized(lock) {
            memory?.let { return it }
            val loaded = readFromDisk()
            memory = loaded
            return loaded
        }
    }

    private fun readFromDisk(): Map<String, MealsCacheEntry> {
        if (!file.exists()) return emptyMap()
        return try {
            val text = file.readText()
            val root = JsonParser.parseString(text).asJsonObject
            when {
                root.has("entries") -> {
                    val type = object : TypeToken<MealsCacheFile>() {}.type
                    val parsed: MealsCacheFile = gson.fromJson(text, type)
                    prune(parsed.entries)
                }
                root.has("date") -> {
                    val type = object : TypeToken<LegacyMealsCacheFile>() {}.type
                    val legacy: LegacyMealsCacheFile = gson.fromJson(text, type)
                    val entry = MealsCacheEntry(
                        fetchedAtMs   = legacy.fetchedAtMs,
                        meals         = legacy.meals,
                        credit        = legacy.credit,
                        boarderId     = legacy.boarderId,
                        mealOrderInfo = legacy.mealOrderInfo,
                    )
                    prune(mapOf(legacy.date to entry))
                }
                else -> emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to read cache: ${e.message}", e)
            emptyMap()
        }
    }

    private fun write(entries: Map<String, MealsCacheEntry>) {
        try {
            file.writeText(gson.toJson(MealsCacheFile(entries)))
        } catch (e: Exception) {
            Log.e(TAG, "failed to write cache: ${e.message}", e)
        }
    }

    private fun prune(entries: Map<String, MealsCacheEntry>): Map<String, MealsCacheEntry> {
        val today = LocalDate.now()
        return entries.filterKeys { key ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@filterKeys false
            val delta = ChronoUnit.DAYS.between(today, date)
            delta in (-KEEP_PAST_DAYS)..KEEP_FUTURE_DAYS
        }
    }

    private fun CachedMeal.toMeal(): Meal = Meal(
        mealId    = mealId,
        name      = name,
        canOrder  = canOrder,
        isOrdered = isOrdered,
        allergens = allergens,
        weight    = weight,
    )

    private fun CachedMealOrderInfo.toMealOrderInfo(): MealOrderInfo = MealOrderInfo(
        mealTypeIndex = mealTypeIndex,
        title = title,
        orderedChoice = orderedChoice,
        availableChoices = availableChoices.map { c ->
            MenuChoice(
                letter = c.letter,
                name = c.name,
                allergens = c.allergens,
                weight = c.weight,
                rating = c.rating?.let {
                    com.edupage.api.model.MealRating(
                        qualityAverage = it.qualityAverage,
                        qualityCount = it.qualityCount,
                        quantityAverage = it.quantityAverage,
                        quantityCount = it.quantityCount,
                    )
                },
            )
        },
        canChangeUntil = canChangeUntil,
        servedFrom = servedFrom,
        servedTo = servedTo,
        amountOfFoods = amountOfFoods,
    )
}
