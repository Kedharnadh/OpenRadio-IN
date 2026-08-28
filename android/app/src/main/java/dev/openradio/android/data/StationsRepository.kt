package dev.openradio.android.data

import android.content.Context
import dev.openradio.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads the station database straight from the OpenRadio-IN git repo (GitHub Pages),
 * so stations added to the repo appear automatically after the next refresh.
 */
class StationsRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cacheFile: File get() = File(context.filesDir, "stations.json")

    suspend fun refresh(): List<Station> = withContext(Dispatchers.IO) {
        val text = runCatching {
            fetch(BuildConfig.STATIONS_URL)
                ?: fetch(BuildConfig.STATIONS_URL_FALLBACK)
        }.getOrNull()
        if (text != null) {
            saveCache(text)
            runCatching { StationParser.parse(text) }.getOrElse { loadCache() }
        } else {
            loadCache()
        }
    }

    suspend fun cached(): List<Station> = withContext(Dispatchers.IO) { loadCache() }

    private fun fetch(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    null
                } else {
                    response.body?.string()
                }
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun saveCache(text: String) {
        runCatching { cacheFile.writeText(text) }
    }

    private fun loadCache(): List<Station> {
        val text = runCatching { cacheFile.readText() }.getOrNull() ?: return emptyList()
        return runCatching { StationParser.parse(text) }.getOrElse { emptyList() }
    }
}

/** Process-wide holder so both the UI and the media service share one station list. */
object StationsStore {

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var loadJobStarted = false

    fun ensureLoaded(context: Context, force: Boolean = false) {
        if (loadJobStarted && !force) return
        loadJobStarted = true
        kotlinx.coroutines.MainScope().launch {
            val repo = StationsRepository(context)
            _loading.value = true
            if (_stations.value.isEmpty()) {
                _stations.value = repo.cached()
            }
            _stations.value = repo.refresh()
            _loading.value = false
        }
    }

    fun setStations(stations: List<Station>) {
        _stations.value = stations
    }
}
