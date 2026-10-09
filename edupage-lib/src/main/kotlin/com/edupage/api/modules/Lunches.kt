package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.Meal
import com.edupage.api.model.MealOrderInfo
import com.edupage.api.model.MealRating
import com.edupage.api.model.Meals
import com.edupage.api.model.MenuChoice
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

internal class Lunches(private val session: EdupageSession) {

    private val urlDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val jsonDateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val log = Logger.getLogger("EduLunches")

    private fun JsonElement?.safeStr(): String? =
        if (this == null || isJsonNull) null else try { asString } catch (_: Exception) { null }

    private fun JsonElement?.safeObj(): JsonObject? =
        if (this == null || isJsonNull) null else try { asJsonObject } catch (_: Exception) { null }

    suspend fun getMeals(date: LocalDate): Meals? {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return withContext(Dispatchers.IO) {
            val url = mealPageUrl(date)
            val response = session.httpClient.newCall(Request.Builder().url(url).get().build()).execute()
            val html = response.body?.string() ?: return@withContext null
            // EduPage occasionally sends an empty edupageData list -> no menu.
            if (isEdupageDataEmpty(html)) return@withContext null
            val json = extractJsonViaMarker(html)
            if (json != null) return@withContext parseFullMealsJson(json, date)
            log.info("edupageData not found, HTML fallback")
            parseMealsFromHtml(html, date)
        }
    }

    suspend fun orderMeal(date: LocalDate, mealTypeIndex: String, choice: String, boarderId: String): Boolean {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return postMealChoice(date, mealTypeIndex, choice, boarderId)
    }

    suspend fun cancelMeal(date: LocalDate, mealTypeIndex: String, boarderId: String): Boolean {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return postMealChoice(date, mealTypeIndex, "AX", boarderId)
    }

    /**
     * Rates the meal of [mealTypeIndex] (quality/quantity, 1..5). Mirrors EduPage's
     * `menu/?akcia=ulozHodnotenia` request. Returns true on success.
     */
    suspend fun rateMeal(
        date: LocalDate,
        mealTypeIndex: String,
        boarderId: String,
        quality: Int,
        quantity: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        try {
            val formBody = FormBody.Builder()
                .add("akcia", "ulozHodnotenia")
                .add("stravnikid", boarderId)
                .add("mysqlDate", date.format(jsonDateFmt))
                .add("jedlo_dna", mealTypeIndex)
                .add("kvalita", quality.toString())
                .add("mnozstvo", quantity.toString())
                .build()
            val request = Request.Builder().url(mealPageUrl(date)).post(formBody).build()
            val body = session.httpClient.newCall(request).execute().body?.string()
                ?: return@withContext false
            val json = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
                ?: return@withContext false
            val error = json.get("error")?.safeStr()
            val ok = error.isNullOrEmpty()
            log.info("rate type $mealTypeIndex $date: ${if (ok) "ok" else "err: $error"}")
            ok
        } catch (e: Exception) {
            log.warning("rateMeal failed: ${e.message}")
            false
        }
    }

    private fun isEdupageDataEmpty(html: String): Boolean {
        for (marker in listOf("edupageData: ", "edupageData = ")) {
            val idx = html.indexOf(marker)
            if (idx == -1) continue
            val rest = html.substring(idx + marker.length).trimStart()
            if (rest.startsWith("[]") || rest.startsWith("null")) return true
        }
        return false
    }

    private fun mealPageUrl(date: LocalDate): String =
        "https://${session.subdomain}.edupage.org/menu/?date=${date.format(urlDateFmt)}"

    private fun extractJsonViaMarker(html: String): String? {
        for (marker in listOf("edupageData: ", "edupageData = ")) {
            val start = html.indexOf(marker)
            if (start == -1) continue
            val braceStart = html.indexOf('{', start + marker.length)
            if (braceStart == -1) continue
            var depth = 0
            var i = braceStart
            while (i < html.length) {
                when (html[i]) {
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) return html.substring(braceStart, i + 1) }
                }
                i++
            }
        }
        return null
    }

    private fun parseFullMealsJson(jsonStr: String, date: LocalDate): Meals {
        return try {
            val root = JsonParser.parseString(jsonStr).asJsonObject
            val subdomain = session.subdomain ?: return htmlFallbackMeals(date)
            val lunchesData = root.getAsJsonObject(subdomain) ?: return htmlFallbackMeals(date)
            val novyListok = lunchesData.getAsJsonObject("novyListok") ?: return htmlFallbackMeals(date)
            val dayData = novyListok.getAsJsonObject(date.format(jsonDateFmt)) ?: return emptyMeals(date)

            val addInfo = novyListok.getAsJsonObject("addInfo")
            val boarderId = addInfo?.get("stravnikid")?.safeStr()
            val credit = extractCredit(addInfo)

            val flatMeals = mutableListOf<Meal>()
            val orderInfoList = mutableListOf<MealOrderInfo>()

            for (type in listOf("1", "2", "3")) {
                val typeObj = dayData.getAsJsonObject(type) ?: continue
                if (typeObj.get("isCooking")?.asBoolean == false) continue

                val rows = typeObj.getAsJsonArray("rows") ?: continue
                val title = typeObj.get("nazov")?.safeStr() ?: ""
                val orderedChoice = parseOrderedChoice(typeObj.get("evidencia"))
                val canChangeUntil = typeObj.get("zmen_do")?.safeStr()
                val servedFrom = typeObj.get("vydaj_od")?.safeStr()
                val servedTo = typeObj.get("vydaj_do")?.safeStr()

                val menuChoices = mutableListOf<MenuChoice>()
                val ratingsObj = typeObj.get("hodnotenia")?.takeIf { !it.isJsonNull }?.let {
                    runCatching { it.asJsonObject }.getOrNull()
                }
                for (row in rows) {
                    if (row.isJsonNull) continue
                    val obj = row.asJsonObject
                    val meal = parseRow(obj)
                    if (meal != null) flatMeals.add(meal)

                    val letter = obj.get("menusStr")?.safeStr()?.replace(": ", "")?.trim()
                    val name = obj.get("nazov")?.safeStr() ?: ""
                    val allergens = parseAllergens(obj)
                    val weight = obj.get("hmotnostiStr")?.safeStr() ?: obj.get("weight")?.safeStr()
                    if (!letter.isNullOrBlank()) {
                        val rating = ratingsObj?.get(letter)?.let { parseRating(it) }
                        menuChoices.add(MenuChoice(letter, name, allergens, weight, rating))
                    }
                }

                if (menuChoices.isNotEmpty()) {
                    orderInfoList.add(
                        MealOrderInfo(
                            mealTypeIndex = type,
                            title = title,
                            orderedChoice = orderedChoice,
                            availableChoices = menuChoices,
                            canChangeUntil = canChangeUntil,
                            servedFrom = servedFrom,
                            servedTo = servedTo,
                            amountOfFoods = typeObj.get("druhov_jedal")?.let {
                                runCatching { it.asInt }.getOrNull()
                            },
                        )
                    )
                }
            }

            Meals(date, flatMeals, orderInfoList.ifEmpty { null }, credit, boarderId)
        } catch (e: Exception) {
            log.warning("parseFullMealsJson error: ${e.message}")
            htmlFallbackMeals(date)
        }
    }

    private suspend fun postMealChoice(date: LocalDate, mealTypeIndex: String, choice: String, boarderId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val gson = com.google.gson.Gson()
                val boarderMenu = mapOf(
                    "stravnikid" to boarderId,
                    "mysqlDate" to date.format(jsonDateFmt),
                    "jids" to mapOf(mealTypeIndex to choice),
                    "view" to "pc_listok",
                    "pravo" to "Student",
                )
                val formBody = FormBody.Builder()
                    .add("akcia", "ulozJedlaStravnika")
                    .add("jedlaStravnika", gson.toJson(boarderMenu))
                    .build()
                val request = Request.Builder().url(mealPageUrl(date)).post(formBody).build()
                val body = session.httpClient.newCall(request).execute().body?.string() ?: return@withContext false
                val json = JsonParser.parseString(body).asJsonObject
                val error = json.get("error")?.safeStr()
                val ok = error.isNullOrEmpty()
                log.info("order $choice type $mealTypeIndex $date: ${if (ok) "ok" else "err: $error"}")
                ok
            } catch (e: Exception) {
                log.warning("postMealChoice failed: ${e.message}")
                false
            }
        }
    }

    private fun parseOrderedChoice(evidencia: JsonElement?): String? {
        if (evidencia == null || evidencia.isJsonNull) return null
        return try {
            val ev = evidencia.asJsonObject
            if (ev.get("stav")?.safeStr() == "V") ev.get("obj")?.safeStr() else null
        } catch (_: Exception) { null }
    }

    private fun extractCredit(addInfo: JsonObject?): String? {
        if (addInfo == null) return null
        for (field in listOf("kredit", "aktualny_kredit", "zostatok", "stav_konta", "konto")) {
            val v = addInfo.get(field)?.safeStr()
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    private fun parseRow(obj: JsonObject): Meal? {
        val name = obj.get("nazov")?.safeStr() ?: obj.get("name")?.safeStr() ?: return null
        if (name.isBlank()) return null
        return Meal(
            mealId = obj.get("id")?.safeStr(),
            name = name,
            canOrder = obj.get("canorder")?.asBoolean ?: obj.get("can_order")?.asBoolean ?: false,
            isOrdered = false,
            allergens = parseAllergens(obj),
            weight = obj.get("hmotnostiStr")?.safeStr() ?: obj.get("weight")?.safeStr(),
        )
    }

    private fun parseAllergens(obj: JsonObject?): List<String>? {
        if (obj == null) return null
        val arr = obj.get("allergens")?.takeIf { !it.isJsonNull && it.isJsonArray }?.asJsonArray
        if (arr != null && arr.size() > 0) return arr.map { it.asString }
        val str = obj.get("alergenyStr")?.safeStr()
        if (!str.isNullOrBlank()) return str.split(Regex(",\\s*")).map { it.trim() }.filter { it.isNotBlank() }
        return null
    }

    /**
     * EduPage stores ratings per menu letter as `hodnotenia[letter] = [quality, quantity]`,
     * each `{ "priemer": Double, "pocet": Int }`.
     */
    private fun parseRating(el: JsonElement?): MealRating? {
        val arr = el?.takeIf { !it.isJsonNull && it.isJsonArray }?.asJsonArray ?: return null
        if (arr.size() < 2) return null
        val quality = arr[0].takeIf { it.isJsonObject }?.asJsonObject
        val quantity = arr[1].takeIf { it.isJsonObject }?.asJsonObject
        val rating = MealRating(
            qualityAverage = quality?.get("priemer")?.safeStr()?.toDoubleOrNull()
                ?: quality?.get("priemer")?.let { runCatching { it.asDouble }.getOrNull() },
            qualityCount = quality?.get("pocet")?.let { runCatching { it.asInt }.getOrNull() },
            quantityAverage = quantity?.get("priemer")?.safeStr()?.toDoubleOrNull()
                ?: quantity?.get("priemer")?.let { runCatching { it.asDouble }.getOrNull() },
            quantityCount = quantity?.get("pocet")?.let { runCatching { it.asInt }.getOrNull() },
        )
        return rating.takeIf { it.hasRatings }
    }

    private fun htmlFallbackMeals(date: LocalDate): Meals {
        log.warning("JSON parse failed, empty meals for $date")
        return emptyMeals(date)
    }

    private fun emptyMeals(date: LocalDate): Meals = Meals(date, emptyList())

    private fun parseMealsFromHtml(html: String, date: LocalDate): Meals {
        val dateStr = date.format(jsonDateFmt)
        val meals = mutableListOf<Meal>()
        for (type in listOf("1", "2", "3")) {
            val itemId = "$dateStr-$type"
            val liStart = html.indexOf("data-listItemId=\"$itemId\"")
            if (liStart == -1) continue
            val liOpen = html.lastIndexOf("<li", liStart)
            if (liOpen == -1) continue
            val liEnd = findClosingTag(html, liOpen, "li")
            if (liEnd == -1) continue
            val liContent = html.substring(liOpen, liEnd)
            val mealLines = extractSpanText(liContent, "menu_DFText_4")
            val allergenLines = extractSpanText(liContent, "menu_DFText_5")
            for ((i, line) in mealLines.withIndex()) {
                val nameHtml = line.trim()
                if (nameHtml.isEmpty()) continue
                val name = stripHtmlTags(nameHtml)
                if (name.isBlank()) continue
                val weight = parseWeight(nameHtml)
                val allergens = if (i < allergenLines.size) {
                    val raw = stripHtmlTags(allergenLines[i]).trim()
                    if (raw.isBlank()) null else raw.split(Regex(",\\s*")).map { it.trim() }.filter { it.isNotBlank() }
                } else null
                meals.add(Meal(null, name, false, false, allergens, weight))
            }
        }
        return Meals(date, meals)
    }

    private fun findClosingTag(html: String, openIdx: Int, tag: String): Int {
        val openTag = "<$tag"; val closeTag = "</$tag>"
        var depth = 0; var i = openIdx
        while (i < html.length) {
            if (html.startsWith(openTag, i) && (i + openTag.length >= html.length || !html[i + openTag.length].isLetterOrDigit())) { depth++; i += openTag.length }
            else if (html.startsWith(closeTag, i)) { depth--; if (depth == 0) return i + closeTag.length; i += closeTag.length }
            else i++
        }
        return -1
    }

    private fun extractSpanText(html: String, classSubstring: String): List<String> {
        val spanStart = html.indexOf(classSubstring)
        if (spanStart == -1) return emptyList()
        val openBracket = html.indexOf('>', spanStart)
        if (openBracket == -1) return emptyList()
        val contentStart = openBracket + 1
        var depth = 0; var contentEnd = contentStart; var i = contentStart
        while (i < html.length) {
            if (html.startsWith("<span", i) && (i + 5 >= html.length || !html[i + 5].isLetterOrDigit())) { depth++; i += 5 }
            else if (html.startsWith("</span>", i)) { if (depth == 0) { contentEnd = i; break }; depth--; i += 7 }
            else i++
        }
        return html.substring(contentStart, contentEnd).split(Regex("<br\\s*/?>"))
    }

    private fun stripHtmlTags(html: String): String = html.replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()

    private fun parseWeight(html: String): String? {
        Regex("""<span[^>]*>\((\d+(?:/\d+)+)\)</span>""").find(html)?.let { return it.groupValues[1] }
        val stripped = stripHtmlTags(html)
        Regex("""\((\d+(?:/\d+)+)\)\s*$""").find(stripped)?.let { return it.groupValues[1] }
        return null
    }
}

