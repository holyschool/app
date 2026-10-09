package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.edupage.api.model.Meal
import com.edupage.api.model.MealOrderInfo
import com.edupage.api.model.Meals
import com.edupage.api.model.MenuChoice
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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

private data class MealsCacheFile(
    val date: String,
    val fetchedAtMs: Long,
    val meals: List<CachedMeal>,
    val credit: String? = null,
    val boarderId: String? = null,
    val mealOrderInfo: List<CachedMealOrderInfo>? = null,
)

@Singleton
class MealsCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "meals_cache.json"
        private const val TAG = "MealsCache"
        private const val STALE_AFTER_HOURS = 2L
    }

    private val gson = Gson()
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
        val cacheFile = MealsCacheFile(
            date          = meals.date.toString(),
            fetchedAtMs   = System.currentTimeMillis(),
            meals         = cached,
            credit        = meals.credit,
            boarderId     = meals.boarderId,
            mealOrderInfo = cachedOrderInfo,
        )
        file.writeText(gson.toJson(cacheFile))
    }

    fun load(date: String = java.time.LocalDate.now().toString()): Meals? {        if (!file.exists()) {
            Log.i(TAG, "cache miss: no file")
            return null
        }
        return try {
            val type = object : TypeToken<MealsCacheFile>() {}.type
            val cf: MealsCacheFile = gson.fromJson(file.readText(), type)
            if (cf.date != date) {
                Log.i(TAG, "cache stale: cached date=${cf.date}, requested date=$date")
                return null
            }
            val ageHours = (System.currentTimeMillis() - cf.fetchedAtMs) / 3_600_000L
            if (ageHours >= STALE_AFTER_HOURS) {
                Log.i(TAG, "cache expired: $ageHours hours old")
                return null
            }
            val meals = cf.meals.map { it.toMeal() }
            val orderInfo = cf.mealOrderInfo?.map { it.toMealOrderInfo() }
            Log.i(TAG, "cache hit: ${meals.size} meals for $date")
            Meals(
                date = java.time.LocalDate.parse(date),
                meals = meals,
                mealOrderInfo = orderInfo,
                credit = cf.credit,
                boarderId = cf.boarderId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "failed to read cache: ${e.message}", e)
            null
        }
    }

    fun clear() {
        Log.i(TAG, "clearing meals cache")
        file.delete()
    }

    fun loadLenient(date: String = java.time.LocalDate.now().toString()): Meals? {
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<MealsCacheFile>() {}.type
            val cf: MealsCacheFile = gson.fromJson(file.readText(), type)
            if (cf.date != date) return null
            val meals = cf.meals.map { it.toMeal() }
            val orderInfo = cf.mealOrderInfo?.map { it.toMealOrderInfo() }
            Meals(
                date = java.time.LocalDate.parse(date),
                meals = meals,
                mealOrderInfo = orderInfo,
                credit = cf.credit,
                boarderId = cf.boarderId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "failed to read lenient cache: ${e.message}", e)
            null
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

