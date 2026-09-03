package dev.openradio.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.openradio.android.Prefs
import dev.openradio.android.data.EpgSchedule
import dev.openradio.android.data.MetadataRepository
import dev.openradio.android.data.Station
import dev.openradio.android.data.StationsStore
import dev.openradio.android.playback.AppPlayer
import dev.openradio.android.playback.PlaybackUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FilterState(
    val query: String = "",
    val language: String? = null,
    val category: String? = null,
    val onlyFavorites: Boolean = false,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val metadataRepository = MetadataRepository()

    val playback: StateFlow<PlaybackUiState> = AppPlayer.state

    private val _filter = MutableStateFlow(FilterState(language = Prefs.filterLanguage()))
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    private val _favorites = MutableStateFlow(Prefs.favorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow(Prefs.recents())
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    private val _epg = MutableStateFlow<EpgSchedule?>(null)
    val epg: StateFlow<EpgSchedule?> = _epg.asStateFlow()

    private val _sleepEndAt = MutableStateFlow<Long?>(null)
    val sleepEndAt: StateFlow<Long?> = _sleepEndAt.asStateFlow()

    val stations: StateFlow<List<Station>> = StationsStore.stations

    val stationsLoading: StateFlow<Boolean> = StationsStore.loading

    val allLanguages: StateFlow<List<String>> =
        stations
            .map { list -> list.flatMap { it.languageTags }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<String>> =
        stations
            .map { list ->
                list.flatMap { it.categories }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
                    .distinct()
                    .sorted()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredStations: StateFlow<List<Station>> =
        combine(stations, _filter, _favorites) { list, f, favs ->
            list.filter { station ->
                val matchesQuery =
                    f.query.isBlank() ||
                        station.name.contains(f.query, ignoreCase = true) ||
                        station.language.contains(f.query, ignoreCase = true) ||
                        station.city.contains(f.query, ignoreCase = true) ||
                        station.categories.any { it.contains(f.query, ignoreCase = true) }
                val matchesLanguage = f.language == null || station.languageTags.contains(f.language)
                val matchesCategory = f.category == null || station.categories.contains(f.category)
                val matchesFavorites = !f.onlyFavorites || favs.contains(station.id)
                matchesQuery && matchesLanguage && matchesCategory && matchesFavorites
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var metadataJob: Job? = null
    private var metadataStationId: String? = null
    private var sleepJob: Job? = null

    init {
        viewModelScope.launch {
            AppPlayer.state.collect { state -> handlePlaybackState(state) }
        }
        viewModelScope.launch {
            filteredStations.collect { list ->
                if (list.isNotEmpty() && !AppPlayer.state.value.playing) {
                    AppPlayer.setQueue(list)
                }
            }
        }
    }

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    fun setLanguage(language: String?) {
        Prefs.setFilterLanguage(language)
        _filter.update { it.copy(language = language) }
    }

    fun setCategory(category: String?) = _filter.update { it.copy(category = category) }

    fun setOnlyFavorites(enabled: Boolean) = _filter.update { it.copy(onlyFavorites = enabled) }

    fun toggleFavorite(stationId: String) {
        val current = _favorites.value
        val updated = if (stationId in current) current - stationId else current + stationId
        _favorites.value = updated
        Prefs.setFavorites(updated)
    }

    fun play(station: Station) {
        val queue = filteredStations.value.ifEmpty { StationsStore.stations.value }
        AppPlayer.playStation(station, queue)
    }

    /**
     * Toggles playback for a station card: pauses the station if it is currently
     * playing, resumes it if it is paused, otherwise starts it.
     */
    fun togglePlay(station: Station) {
        val state = AppPlayer.state.value
        val isCurrent = state.currentStationId == station.id
        when {
            isCurrent && state.playing -> AppPlayer.pause()
            isCurrent && state.paused -> AppPlayer.resume()
            else -> play(station)
        }
    }

    fun pause() = AppPlayer.pause()

    fun resume() = AppPlayer.resume()

    fun stop() = AppPlayer.stop()

    fun skipNext() = AppPlayer.skipNext()

    fun skipPrevious() = AppPlayer.skipPrevious()

    fun setVolume(volume: Float) {
        AppPlayer.setVolume(volume)
        Prefs.setVolume(volume)
    }

    fun toggleMute() = AppPlayer.toggleMute()

    fun loadEpg(stationId: String) {
        val station = StationsStore.stations.value.firstOrNull { it.id == stationId } ?: return
        if (station.epgId <= 0) {
            _epg.value = null
            return
        }
        viewModelScope.launch {
            _epg.value = metadataRepository.fetchEpg(station.epgId)
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        _sleepEndAt.value = endAt
        sleepJob =
            viewModelScope.launch {
                delay(minutes * 60_000L)
                AppPlayer.pause()
                _sleepEndAt.value = null
            }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepEndAt.value = null
    }

    fun isSleepTimerActive(): Boolean = sleepJob?.isActive == true

    private fun handlePlaybackState(state: PlaybackUiState) {
        val stationId = state.currentStationId ?: return
        if (state.playing) {
            updateRecents(stationId)
            startMetadataPolling(stationId)
        } else {
            stopMetadataPolling()
        }
    }

    private fun updateRecents(stationId: String) {
        val updated = (listOf(stationId) + _recents.value.filter { it != stationId }).take(20)
        _recents.value = updated
        Prefs.saveRecents(updated)
    }

    private fun startMetadataPolling(stationId: String) {
        if (metadataJob?.isActive == true && metadataStationId == stationId) return
        metadataStationId = stationId
        metadataJob?.cancel()
        metadataJob =
            viewModelScope.launch {
                while (isActive) {
                    if (AppPlayer.state.value.currentStationId != metadataStationId) break
                    val station = StationsStore.stations.value.firstOrNull { it.id == stationId }
                    val stream = station?.primaryStream
                    if (stream != null) {
                        // Pass the station's status endpoint (AzuraCast / Icecast) so the
                        // metadata proxy can also pull proper song + album art, not just ICY.
                        val nowPlaying = metadataRepository.fetchNowPlaying(stream.url, station.metadataUrl)
                        if (nowPlaying != null && AppPlayer.state.value.playing) {
                            AppPlayer.updateNowPlaying(nowPlaying.streamTitle, nowPlaying.artUrl)
                        }
                    }
                    delay(15_000)
                }
            }
    }

    private fun stopMetadataPolling() {
        metadataJob?.cancel()
        metadataJob = null
        metadataStationId = null
    }

    private fun MutableStateFlow<FilterState>.update(transform: (FilterState) -> FilterState) {
        value = transform(value)
    }
}
