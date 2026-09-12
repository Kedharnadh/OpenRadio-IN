package dev.openradio.android.data

import android.util.Log
import dev.openradio.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI

data class NowPlaying(
    val streamTitle: String,
    val artUrl: String,
)

data class EpgProgram(
    val start: String,
    val end: String,
    val title: String,
)

data class EpgSchedule(
    val date: String,
    val programs: List<EpgProgram>,
)

/**
 * Uses the same HLS proxy worker the PWA uses to fetch now-playing metadata and
 * Prasar Bharati EPG cuesheets for AIR stations.
 */
class MetadataRepository {
    private val client: OkHttpClient = HttpClient.client

    suspend fun fetchNowPlaying(
        streamUrl: String,
        metadataUrl: String?,
    ): NowPlaying? =
        withContext(Dispatchers.IO) {
            var params = "?meta=1&url=${encode(streamUrl)}"
            if (!metadataUrl.isNullOrBlank()) params += "&metaUrl=${encode(metadataUrl)}"
            fetchJson("${BuildConfig.HLS_PROXY_URL}$params")
                ?.let { obj ->
                    val title = obj.optString("streamTitle", "")
                    if (title.isBlank()) {
                        null
                    } else {
                        NowPlaying(title, obj.optString("art", ""))
                    }
                }
                ?: fetchIcecastStatus(streamUrl)
        }

    /**
     * Fallback for Icecast streams whose ICY metadata is unreachable through the
     * proxy worker: query the server's status-json.xsl endpoint directly, which
     * reports the currently playing song.
     */
    private suspend fun fetchIcecastStatus(streamUrl: String): NowPlaying? =
        withContext(Dispatchers.IO) {
            val statusUrl = deriveIcecastStatusUrl(streamUrl) ?: return@withContext null
            val request = Request.Builder().url(statusUrl).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val text = response.body?.string() ?: return@use null
                    parseIcecastStatus(text)?.let { NowPlaying(it, "") }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network error fetching Icecast status $statusUrl", e)
                null
            }
        }

    /**
     * Parses an Icecast [status-json.xsl payload][json] and returns the title of
     * the currently playing song (or null if none is reported).
     */
    internal fun parseIcecastStatus(json: String): String? =
        runCatching {
            val obj = JSONObject(json).getJSONObject("icestats")
            when (val source = obj.opt("source")) {
                is JSONArray ->
                    (0 until source.length()).mapNotNull { index ->
                        source.optJSONObject(index)?.let { icecastTitle(it) }
                    }.firstOrNull()
                is JSONObject -> icecastTitle(source)
                else -> null
            }
        }.getOrNull()

    private fun icecastTitle(mount: JSONObject): String? {
        val song = mount.optString("song", "").trim()
        if (song.isNotEmpty()) return song
        val title = mount.optString("title", "").trim()
        return title.takeIf { it.isNotEmpty() }
    }

    internal fun deriveIcecastStatusUrl(streamUrl: String): String? =
        runCatching {
            val uri = URI(streamUrl)
            if (uri.scheme != "http" && uri.scheme != "https") return@runCatching null
            "${uri.scheme}://${uri.authority}/status-json.xsl"
        }.getOrNull()

    suspend fun fetchEpg(epgId: Long): EpgSchedule? =
        withContext(Dispatchers.IO) {
            if (epgId <= 0) return@withContext null
            fetchJson("${BuildConfig.HLS_PROXY_URL}?epg=$epgId")?.let { obj ->
                val programsArray = obj.optJSONArray("programs")
                val programs =
                    if (programsArray == null) {
                        emptyList()
                    } else {
                        (0 until programsArray.length()).mapNotNull { index ->
                            val item = programsArray.optJSONObject(index) ?: return@mapNotNull null
                            val title = item.optString("title", "")
                            if (title.isBlank()) return@mapNotNull null
                            EpgProgram(
                                start = item.optString("start", ""),
                                end = item.optString("end", ""),
                                title = title,
                            )
                        }
                    }
                EpgSchedule(date = obj.optString("date", ""), programs = programs)
            }
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
            } catch (e: IOException) {
                Log.w(TAG, "Network error fetching $url", e)
                null
            }
        }

    companion object {
        private const val TAG = "MetadataRepo"
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
}
