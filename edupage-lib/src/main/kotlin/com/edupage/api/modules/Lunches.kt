package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.Meal
import com.edupage.api.model.Meals
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class Lunches(private val session: EdupageSession) {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun getMeals(date: LocalDate): Meals? {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/menu/?date=${date.format(dateFmt)}"
            val request = Request.Builder().url(url).get().build()
            val response = session.httpClient.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext null

            val marker = "edupageData = "
            val start = html.indexOf(marker)
            if (start == -1) return@withContext Meals(date, emptyList())

            val jsonStart = start + marker.length
            val jsonEnd = html.indexOf('\n', jsonStart)
            if (jsonEnd == -1) return@withContext Meals(date, emptyList())

            val jsonStr = html.substring(jsonStart, jsonEnd).trim().trimEnd(';')
            val data = try {
                JsonParser.parseString(jsonStr).asJsonObject
            } catch (e: Exception) {
                return@withContext Meals(date, emptyList())
            }

            val mealsArray = data.getAsJsonArray("menus") ?: return@withContext Meals(date, emptyList())
            val meals = mealsArray.mapNotNull { elem ->
                val menu = elem.asJsonObject
                val name = menu.get("name")?.asString ?: return@mapNotNull null
                val mealId = menu.get("id")?.asString
                val canOrder = menu.get("canOrder")?.asBoolean ?: false
                val isOrdered = menu.get("ordered")?.asBoolean ?: false
                val allergensArray = menu.getAsJsonArray("allergens")
                val allergens = allergensArray?.map { it.asString }
                val weight = menu.get("weight")?.asString
                Meal(mealId, name, canOrder, isOrdered, allergens, weight)
            }

            Meals(date, meals)
        }
    }
}
