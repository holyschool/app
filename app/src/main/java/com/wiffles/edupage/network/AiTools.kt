package com.wiffles.edupage.network

import android.util.Log
import com.google.gson.JsonParser
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * The web-facing half of the chat tools: searching the web and looking up pictures.
 *
 * DuckDuckGo is the primary source, but its HTML endpoints are rate limited from some
 * networks, so every lookup is layered with Wikipedia as a dependable fallback. The chat
 * never fails because of a search: a thin result simply comes back as an empty list.
 */
@Singleton
class AiTools @Inject constructor(
    private val httpClient: OkHttpClient,
) {

    companion object {
        private const val TAG = "AiTools"
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
        private const val MAX_HITS = 5
        private const val MAX_IMAGES = 4
        private const val MAX_IMAGE_BYTES = 6L * 1024 * 1024
    }

    data class WebHit(val title: String, val url: String, val snippet: String)

    data class ImageHit(val title: String, val url: String, val source: String)

    data class Download(val bytes: ByteArray, val mimeType: String, val name: String)

    // ------------------------------------------------------------------ web search

    suspend fun webSearch(query: String): List<WebHit> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val hits = LinkedHashMap<String, WebHit>()

        runCatching { instantAnswers(trimmed) }
            .onFailure { Log.d(TAG, "instant answers failed: ${it.message}") }
            .getOrDefault(emptyList())
            .forEach { hits.putIfAbsent(it.url, it) }

        runCatching { duckHtml(trimmed) }
            .onFailure { Log.d(TAG, "duckduckgo html failed: ${it.message}") }
            .getOrDefault(emptyList())
            .forEach { hits.putIfAbsent(it.url, it) }

        runCatching { wikipedia(trimmed) }
            .onFailure { Log.d(TAG, "wikipedia search failed: ${it.message}") }
            .getOrDefault(emptyList())
            .forEach { hits.putIfAbsent(it.url, it) }

        Log.i(TAG, "web search '$trimmed' -> ${hits.size} hits")
        hits.values.take(MAX_HITS)
    }

    /** DuckDuckGo's Instant Answer API: reliable, but only covers topics it has curated. */
    private fun instantAnswers(query: String): List<WebHit> {
        val url = "https://api.duckduckgo.com/?q=${enc(query)}&format=json&no_html=1" +
            "&no_redirect=1&skip_disambig=1"
        val body = get(url) ?: return emptyList()
        val root = JsonParser.parseString(body).asJsonObject
        val out = mutableListOf<WebHit>()

        val abstract = root.get("AbstractText")?.asString.orEmpty()
        val heading = root.get("Heading")?.asString.orEmpty()
        val link = root.get("AbstractURL")?.asString.orEmpty()
        if (abstract.isNotBlank() && link.isNotBlank()) {
            out += WebHit(heading.ifBlank { query }, link, decodeEntities(abstract))
        }

        root.getAsJsonArray("RelatedTopics")?.forEach { element ->
            collectTopic(element, out)
        }
        return out
    }

    private fun collectTopic(element: com.google.gson.JsonElement, out: MutableList<WebHit>) {
        val obj = runCatching { element.asJsonObject }.getOrNull() ?: return
        val nested = obj.getAsJsonArray("Topics")
        if (nested != null) {
            nested.forEach { collectTopic(it, out) }
            return
        }
        val text = obj.get("Text")?.asString.orEmpty()
        val link = obj.get("FirstURL")?.asString.orEmpty()
        if (text.isNotBlank() && link.isNotBlank() && !link.contains("duckduckgo.com/c/")) {
            val title = text.substringBefore(" - ").take(80)
            out += WebHit(title, link, decodeEntities(text))
        }
    }

    /** DuckDuckGo's no-JS HTML endpoint. Best effort: it is rate limited on some networks. */
    private fun duckHtml(query: String): List<WebHit> {
        val request = Request.Builder()
            .url("https://html.duckduckgo.com/html/")
            .header("User-Agent", BROWSER_UA)
            .post(FormBody.Builder().add("q", query).add("kl", "wt-wt").build())
            .build()
        val html = runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                response.body?.string().orEmpty()
            }
        }.getOrNull().orEmpty()
        if (html.isBlank()) return emptyList()

        val titles = RESULT_LINK.findAll(html).toList()
        val snippets = RESULT_SNIPPET.findAll(html).toList()
        return titles.mapIndexedNotNull { index, match ->
            val href = normalizeDuckLink(match.groupValues[1])
            val title = decodeEntities(stripTags(match.groupValues[2])).trim()
            val snippet = snippets.getOrNull(index)?.let { decodeEntities(stripTags(it.groupValues[1])).trim() }
            if (href.isBlank() || title.isBlank()) null else WebHit(title, href, snippet.orEmpty())
        }
    }

    private fun wikipedia(query: String): List<WebHit> {
        val lang = wikiLanguage()
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&format=json" +
            "&list=search&srsearch=${enc(query)}&srlimit=$MAX_HITS"
        val body = get(url) ?: return emptyList()
        val search = runCatching {
            JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("query")?.getAsJsonArray("search")
        }.getOrNull() ?: return emptyList()
        return search.mapNotNull { element ->
            val obj = element.asJsonObject
            val title = obj.get("title")?.asString.orEmpty()
            val snippet = decodeEntities(stripTags(obj.get("snippet")?.asString.orEmpty())).trim()
            if (title.isBlank()) null
            else WebHit(title, "https://$lang.wikipedia.org/wiki/${title.replace(' ', '_')}", snippet)
        }
    }

    // ----------------------------------------------------------------- image search

    suspend fun imageSearch(query: String, count: Int = MAX_IMAGES): List<ImageHit> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()
            val limit = count.coerceIn(1, MAX_IMAGES)

            val hits = runCatching { wikipediaImages(trimmed, limit) }
                .onFailure { Log.d(TAG, "wikipedia images failed: ${it.message}") }
                .getOrDefault(emptyList())
                .toMutableList()
            if (hits.size < limit) {
                runCatching { commonsImages(trimmed, limit) }
                    .onFailure { Log.d(TAG, "commons images failed: ${it.message}") }
                    .getOrDefault(emptyList())
                    .forEach { hit -> if (hits.none { it.url == hit.url }) hits += hit }
            }
            Log.i(TAG, "image search '$trimmed' -> ${hits.size} images")
            hits.take(limit)
        }

    /** Main picture of the top Wikipedia articles for the topic. */
    private fun wikipediaImages(query: String, limit: Int): List<ImageHit> {
        val lang = wikiLanguage()
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&format=json" +
            "&generator=search&gsrsearch=${enc(query)}&gsrlimit=${limit + 2}" +
            "&prop=pageimages&piprop=thumbnail&pithumbsize=900"
        val body = get(url) ?: return emptyList()
        val pages = runCatching {
            JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("query")?.getAsJsonObject("pages")
        }.getOrNull() ?: return emptyList()
        return pages.entrySet().mapNotNull { (_, element) ->
            val obj = element.asJsonObject
            val thumb = obj.getAsJsonObject("thumbnail")?.get("source")?.asString.orEmpty()
            val title = obj.get("title")?.asString.orEmpty()
            if (thumb.isBlank()) null
            else ImageHit(title, thumb.removeSuffix("\n"), "https://$lang.wikipedia.org/wiki/$title")
        }
    }

    /** Free-licensed pictures from Wikimedia Commons, used when no article has a lead image. */
    private fun commonsImages(query: String, limit: Int): List<ImageHit> {
        val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&generator=search&gsrsearch=${enc(query)}&gsrnamespace=6&gsrlimit=$limit" +
            "&prop=imageinfo&iiprop=url&iiurlwidth=900"
        val body = get(url) ?: return emptyList()
        val pages = runCatching {
            JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("query")?.getAsJsonObject("pages")
        }.getOrNull() ?: return emptyList()
        return pages.entrySet().mapNotNull { (_, element) ->
            val obj = element.asJsonObject
            val info = obj.getAsJsonArray("imageinfo")?.firstOrNull()?.asJsonObject
            val thumb = info?.get("thumburl")?.asString.orEmpty()
            val title = obj.get("title")?.asString.orEmpty().removePrefix("File:").substringBeforeLast('.')
            if (thumb.isBlank()) null else ImageHit(title, thumb, "https://commons.wikimedia.org/")
        }
    }

    /** Fetches a picture so it can be saved into the chat as an attachment. */
    suspend fun downloadImage(url: String): Download? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_UA)
                .header("Accept", "image/*")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val mime = response.header("Content-Type")?.substringBefore(';')?.trim().orEmpty()
                if (!mime.startsWith("image/")) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                if (bytes.isEmpty() || bytes.size > MAX_IMAGE_BYTES) return@withContext null
                Download(bytes, mime, imageName(url, mime))
            }
        }.onFailure { Log.d(TAG, "image download failed: ${it.message}") }.getOrNull()
    }

    // ------------------------------------------------------------------- helpers

    private fun get(url: String): String? = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", BROWSER_UA).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }.onFailure { Log.d(TAG, "GET failed for $url: ${it.message}") }.getOrNull()

    /** The Wikipedia language to prefer: the app's own language when we support it. */
    private fun wikiLanguage(): String =
        when (val language = Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "cs", "sk", "de", "fr", "es", "it", "pl", "uk", "hu" -> language
            else -> "en"
        }

    private fun imageName(url: String, mime: String): String {
        val fromUrl = url.substringBefore('?').substringAfterLast('/')
            .substringBeforeLast('?')
            .ifBlank { "image" }
        val cleaned = fromUrl.replace(Regex("[^A-Za-z0-9._-]"), "-").take(60)
        return if (cleaned.contains('.')) cleaned else "$cleaned.${mime.substringAfter('/', "jpg")}"
    }

    /** DuckDuckGo wraps result links in a redirect; pull the real target out of it. */
    private fun normalizeDuckLink(raw: String): String {
        val href = decodeEntities(raw).removePrefix("//")
        val wrapped = href.substringAfter("uddg=", "")
        if (wrapped.isNotBlank()) {
            val value = wrapped.substringBefore('&')
            return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
        }
        return if (href.startsWith("http")) href else "https://$href"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun stripTags(html: String): String =
        html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        var out = text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&hellip;", "…")
        out = NUMERIC_ENTITY.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code == null) match.value else String(Character.toChars(code))
        }
        return out
    }

    private val RESULT_LINK = Regex(
        "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val RESULT_SNIPPET = Regex(
        "class=\"result__snippet\"[^>]*>(.*?)</a>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val NUMERIC_ENTITY = Regex("&#(\\d+);")
}