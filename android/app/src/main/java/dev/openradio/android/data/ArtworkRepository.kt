package dev.openradio.android.data

import android.content.Context
import android.util.Log
import dev.openradio.android.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Resolves album art for the track currently on air.
 *
 * Almost every station reports no artwork in its stream metadata: only the
 * handful of AzuraCast stations answer with an `art` URL, and plain ICY /
 * Icecast streams report nothing but a title. So when the metadata response
 * carries no art, the song is looked up on iTunes and then Deezer, the same
 * way the web player does it. Results are cached on disk so each track is only
 * ever looked up once per [CACHE_TTL_MS].
 */
class ArtworkRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client: OkHttpClient = HttpClient.client
    private val prefs = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * Cover art URL for [nowPlaying], or an empty string when nothing could be
     * resolved. [stationName] is used as a last-resort search term, because
     * talk radio titles are frequently generic and match nothing on their own.
     */
    suspend fun resolve(
        nowPlaying: NowPlaying,
        stationName: String = "",
    ): String {
        // Art supplied by the station itself always wins: it is authoritative.
        nowPlaying.artUrl.takeIf { it.isNotBlank() }?.let { return it }

        val cacheKey = nowPlaying.display.lowercase().trim()
        if (cacheKey.isBlank()) return ""
        cached(cacheKey)?.let { return it }

        val terms =
            (nowPlaying.searchTerms + stationName.trim())
                .filter { it.isNotBlank() }
                .distinct()
        for (term in terms) {
            lookupItunes(term)?.let { art ->
                putCached(cacheKey, art)
                return art
            }
            lookupDeezer(term)?.let { art ->
                putCached(cacheKey, art)
                return art
            }
        }
        return ""
    }

    /** iTunes search API: returns the first matching song's cover art. */
    internal suspend fun lookupItunes(term: String): String? =
        fetchJson("https://itunes.apple.com/search?term=${encode(term)}&media=music&entity=song&limit=1")
            ?.optJSONArray("results")
            ?.optJSONObject(0)
            ?.optString("artworkUrl100", "")
            ?.takeIf { it.startsWith("http") }
            ?.let(::upgradeItunesArtwork)

    /** Deezer search API: returns the first match's large album cover. */
    internal suspend fun lookupDeezer(term: String): String? =
        fetchJson("https://api.deezer.com/search?q=${encode(term)}&limit=1")
            ?.optJSONArray("data")
            ?.optJSONObject(0)
            ?.optJSONObject("album")
            ?.optString("cover_big", "")
            ?.takeIf { it.startsWith("http") }

    /**
     * iTunes serves 100x100 covers by default, which look blurry on a
     * notification or a car screen, so request the 600x600 variant instead.
     */
    internal fun upgradeItunesArtwork(url: String): String =
        if (url.contains("100x100bb")) {
            url.replace("100x100bb", "600x600bb")
        } else {
            url
        }

    private suspend fun fetchJson(url: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "HTTP ${response.code} from $url")
                        return@use null
                    }
                    response.body?.string()?.let { text -> runCatching { JSONObject(text) }.getOrNull() }
                }
            } catch (e: Exception) {
                App.reportError(e, "Artwork lookup failed for $url")
                null
            }
        }

    private fun cached(key: String): String? {
        val entry = readStore()[key] ?: return null
        val (url, at) = entry
        if (System.currentTimeMillis() - at > CACHE_TTL_MS) return null
        return url.takeIf { it.isNotBlank() }
    }

    private fun putCached(
        key: String,
        url: String,
    ) {
        val store = readStore().toMutableMap()
        store[key] = url to System.currentTimeMillis()
        // Drop the oldest entries so the store cannot grow without bound.
        val trimmed =
            store.entries
                .sortedBy { it.value.second }
                .takeLast(CACHE_MAX_ENTRIES)
                .associate { it.key to it.value }
        val json = JSONObject()
        trimmed.forEach { (k, v) -> json.put(k, JSONArray().put(v.first).put(v.second)) }
        prefs.edit().putString(KEY_STORE, json.toString()).apply()
    }

    private fun readStore(): MutableMap<String, Pair<String, Long>> {
        val raw = prefs.getString(KEY_STORE, null) ?: return mutableMapOf()
        val result = mutableMapOf<String, Pair<String, Long>>()
        runCatching {
            val json = JSONObject(raw)
            for (key in json.keys()) {
                val value = json.optJSONArray(key) ?: continue
                val url = value.optString(0, "")
                val at = value.optLong(1, 0L)
                if (url.isNotBlank()) result[key] = url to at
            }
        }.onFailure { Log.w(TAG, "Corrupt artwork cache", it) }
        return result
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val TAG = "ArtworkRepo"
        private const val FILE = "openradio_artwork"
        private const val KEY_STORE = "store"
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
        private const val CACHE_MAX_ENTRIES = 200
    }
}
