package dev.openradio.android.data

import dev.openradio.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NowPlaying(
    val streamTitle: String,
    val artUrl: String
)

data class EpgProgram(
    val start: String,
    val end: String,
    val title: String
)

data class EpgSchedule(
    val date: String,
    val programs: List<EpgProgram>
)

/**
 * Uses the same HLS proxy worker the PWA uses to fetch now-playing metadata and
 * Prasar Bharati EPG cuesheets for AIR stations.
 */
class MetadataRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNowPlaying(streamUrl: String, metadataUrl: String?): NowPlaying? =
        withContext(Dispatchers.IO) {
            var params = "?meta=1&url=${encode(streamUrl)}"
            if (!metadataUrl.isNullOrBlank()) params += "&metaUrl=${encode(metadataUrl)}"
            fetchJson("${BuildConfig.HLS_PROXY_URL}$params")?.let { obj ->
                val title = obj.optString("streamTitle", "")
                if (title.isBlank()) {
                    null
                } else {
                    NowPlaying(title, obj.optString("art", ""))
                }
            }
        }

    suspend fun fetchEpg(epgId: Long): EpgSchedule? = withContext(Dispatchers.IO) {
        if (epgId <= 0) return@withContext null
        fetchJson("${BuildConfig.HLS_PROXY_URL}?epg=$epgId")?.let { obj ->
            val programsArray = obj.optJSONArray("programs")
            val programs = if (programsArray == null) {
                emptyList()
            } else {
                (0 until programsArray.length()).mapNotNull { index ->
                    val item = programsArray.optJSONObject(index) ?: return@mapNotNull null
                    val title = item.optString("title", "")
                    if (title.isBlank()) return@mapNotNull null
                    EpgProgram(
                        start = item.optString("start", ""),
                        end = item.optString("end", ""),
                        title = title
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
                    if (!response.isSuccessful) return@use null
                    response.body?.string()?.let { text -> runCatching { JSONObject(text) }.getOrNull() }
                }
            } catch (_: IOException) {
                null
            }
        }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
}
