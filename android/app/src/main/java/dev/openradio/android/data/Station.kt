package dev.openradio.android.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Station(
    val id: String,
    val name: String,
    val nameTe: String,
    val nameHi: String,
    val language: String,
    val country: String,
    val state: String,
    val city: String,
    val categories: List<String>,
    val genre: List<String>,
    val homepage: String,
    val logo: String,
    val streams: List<Stream>,
    val verified: Boolean,
    val status: String,
    val epgId: Long,
    val metadataUrl: String,
    val songFirst: Boolean,
) {
    data class Stream(
        val url: String,
        val codec: String,
        val priority: Int,
    ) {
        val isHls: Boolean
            get() = codec.equals("hls", ignoreCase = true) || url.contains(".m3u8", ignoreCase = true)
    }

    val primaryStream: Stream?
        get() = preferredStreams().firstOrNull()

    /**
     * Streams ordered by preference: HLS streams first (they play natively in
     * ExoPlayer / the Cast HLS pipeline), then by ascending priority (1 = primary).
     */
    fun preferredStreams(): List<Stream> =
        streams.sortedWith(compareByDescending<Stream> { it.isHls }.thenBy { it.priority })

    val languageTags: List<String>
        get() = language.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun localizedName(uiLang: String): String =
        when (uiLang) {
            "te" -> nameTe.ifBlank { name }
            "hi" -> nameHi.ifBlank { name }
            else -> name
        }
}

object StationParser {
    fun parse(text: String): List<Station> {
        if (text.isBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            val result = ArrayList<Station>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                result.add(parseStation(obj))
            }
            result
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun parseStation(obj: JSONObject): Station {
        val streamsArray = obj.optJSONArray("streams")
        val streams = ArrayList<Station.Stream>()
        if (streamsArray != null) {
            for (j in 0 until streamsArray.length()) {
                val streamObj = streamsArray.optJSONObject(j) ?: continue
                val url = streamObj.optString("url", "")
                if (url.isBlank()) continue
                streams.add(
                    Station.Stream(
                        url = url,
                        codec = streamObj.optString("codec", ""),
                        priority = streamObj.optInt("priority", Int.MAX_VALUE),
                    ),
                )
            }
        }
        return Station(
            id = obj.optString("id", ""),
            name = obj.optString("name", "Unknown station"),
            nameTe = obj.optString("name_te", ""),
            nameHi = obj.optString("name_hi", ""),
            language = obj.optString("language", ""),
            country = obj.optString("country", ""),
            state = obj.optString("state", ""),
            city = obj.optString("city", ""),
            categories = stringArray(obj.optJSONArray("categories")),
            genre = stringArray(obj.optJSONArray("genre")),
            homepage = obj.optString("homepage", ""),
            logo = obj.optString("logo", ""),
            streams = streams,
            verified = obj.optBoolean("verified", false),
            status = obj.optString("status", "unknown"),
            epgId = obj.optLong("epg_id", -1L),
            metadataUrl = obj.optString("metadata_url", ""),
            songFirst = obj.optBoolean("song_first", false),
        )
    }

    private fun stringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val value = array.optString(index, "").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            value
        }
    }
}
