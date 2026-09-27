package dev.openradio.android.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.openradio.android.App
import dev.openradio.android.BuildConfig
import dev.openradio.android.Prefs
import dev.openradio.android.R
import dev.openradio.android.data.ArtworkRepository
import dev.openradio.android.data.MetadataRepository
import dev.openradio.android.data.Station
import dev.openradio.android.data.StationsStore
import dev.openradio.android.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.media.AudioAttributes as FrameworkAudioAttributes

/** Snapshot of playback state surfaced to the UI. */
data class PlaybackUiState(
    val playing: Boolean = false,
    val paused: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val retryStatus: String? = null,
    val currentStationId: String? = null,
    val currentStationName: String? = null,
    val nowPlayingTrack: String? = null,
    val nowPlayingArt: String? = null,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val castActive: Boolean = false,
    val castAvailable: Boolean = false,
)

/**
 * Returns a proxy URL for Cast playback via the HLS proxy worker.
 *
 * HLS streams go through the worker's HLS pipeline (`?url=`) which resolves
 * the playlist and streams continuous MP3 via ffmpeg.
 * Non-HLS (direct HTTP) streams go through the relay (`?relay=1&url=`) which
 * fetches the upstream with a browser User-Agent and re-serves it over HTTPS,
 * avoiding mixed-content blocks on the Cast receiver.
 */
fun castStreamUrl(
    originalUrl: String,
    isHls: Boolean,
): String {
    val base = BuildConfig.HLS_PROXY_URL
    val encoded = Uri.encode(originalUrl)
    return if (isHls) "$base?url=$encoded" else "$base?relay=1&url=$encoded"
}

/**
 * Process-wide playback owner. A single [CastPlayer] (backed by an internal
 * ExoPlayer for local playback) is wrapped in one [androidx.media3.session.MediaSession],
 * so local playback, Chromecast and Android Auto all share the same player and
 * timeline. Falls back to a plain ExoPlayer when Google Play services (Cast) is
 * unavailable.
 */
object AppPlayer {
    const val ROOT_MEDIA_ID = "openradio_root"

    /** Browsable folders exposed to Android Auto / Android TV. */
    const val ALL_MEDIA_ID = "openradio_all"
    const val FAVORITES_MEDIA_ID = "openradio_favorites"
    const val LANGUAGES_MEDIA_ID = "openradio_languages"

    /** Root of the "suggested" list (favorites) that Android Auto surfaces on the home screen. */
    const val SUGGESTED_MEDIA_ID = "openradio_suggested"

    private const val LANGUAGE_FOLDER_PREFIX = "openradio_lang:"

    /** Media id for the browsable folder holding stations in [tag]. */
    fun languageFolderMediaId(tag: String): String = "$LANGUAGE_FOLDER_PREFIX$tag"

    /**
     * MIME type that media3/ExoPlayer recognizes as HLS (MimeTypes.APPLICATION_M3U8).
     * NOTE: must NOT be "application/vnd.apple.mpegurl" — media3's Util.inferContentType()
     * only treats "application/x-mpegURL" as HLS, otherwise the .m3u8 is played as a
     * generic binary stream and local HLS playback breaks.
     */
    const val HLS_MIME_TYPE = "application/x-mpegURL"

    /** First reclaim delay after a permanent audio-focus loss; doubles per attempt. */
    private const val FOCUS_RECLAIM_BASE_DELAY_MS = 3_000L

    /** Max re-request attempts before giving up on auto-resuming. */
    private const val MAX_FOCUS_RECLAIM_ATTEMPTS = 15

    /** How often the now-playing track title / album art is refreshed. */
    private const val METADATA_POLL_INTERVAL_MS = 15_000L

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var _player: Player? = null
    private var _librarySession: MediaLibraryService.MediaLibrarySession? = null
    private var isCastPlayer = false
    private var volumeBeforeMute = 1f
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var duckVolumeBeforePause = 1f
    private var ducked = false
    private var audioFocusGranted = false

    /** True when playback was paused by audio-focus loss; auto-resumes on GAIN. */
    private var autoResumeOnGain = false

    /** In-flight bounded focus re-request loop after a permanent focus loss. */
    private var focusReclaimJob: Job? = null

    /** In-flight now-playing metadata poll for the station that is playing. */
    private var metadataJob: Job? = null
    private var metadataStationId: String? = null

    private val metadataRepository = MetadataRepository()
    private var artworkRepository: ArtworkRepository? = null

    /** Cover-art resolver, created on first use so it always has a Context. */
    private val artwork: ArtworkRepository?
        get() = artworkRepository ?: appContext?.let { ArtworkRepository(it) }?.also { artworkRepository = it }

    val player: Player? get() = _player
    val librarySession: MediaLibraryService.MediaLibrarySession? get() = _librarySession

    /**
     * Requests audio focus when playback begins so that another app starting to
     * play (or a phone call / navigation prompt) pauses or ducks this station.
     * Focus is released when playback stops. Uses a Media-audio-focus request
     * alongside media3's own handling so the radio reliably yields to other audio.
     */
    private fun requestAudioFocusIfNeeded(): Int {
        val ctx = appContext ?: return AudioManager.AUDIOFOCUS_REQUEST_FAILED
        if (audioFocusGranted) return AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (isRemotePlayback()) return AudioManager.AUDIOFOCUS_REQUEST_FAILED
        val am =
            audioManager
                ?: (ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
                ?: return AudioManager.AUDIOFOCUS_REQUEST_FAILED
        audioManager = am
        val request =
            audioFocusRequest
                ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        FrameworkAudioAttributes.Builder()
                            .setUsage(FrameworkAudioAttributes.USAGE_MEDIA)
                            .setContentType(FrameworkAudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    // Never steal focus from an active app: when someone else is
                    // playing, queue the request and get audio back only once the
                    // other app stops (receiving then a normal AUDIOFOCUS_GAIN).
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
                    .also { audioFocusRequest = it }
        val result = am.requestAudioFocus(request)
        audioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return result
    }

    /**
     * True while another app is actively playing music/media. Used to gate focus
     * reclaim so the radio only takes audio back once the interrupting app has
     * truly stopped, never over it.
     */
    private fun isOtherAudioActive(): Boolean {
        val am = audioManager ?: return false
        @Suppress("DEPRECATION") // Deprecated in API 35 but still the reliable cross-version signal.
        return am.isMusicActive
    }

    /**
     * A permanent focus loss removes this app from the focus stack, so Android
     * never delivers a GAIN when the other app finishes. Re-request focus on a
     * growing backoff and resume once granted.
     */
    private fun scheduleAudioFocusReclaim() {
        focusReclaimJob?.cancel()
        focusReclaimJob =
            mainScope.launch {
                var attempt = 0
                while (isActive && autoResumeOnGain) {
                    delay(FOCUS_RECLAIM_BASE_DELAY_MS * (1L shl attempt.coerceAtMost(3)))
                    if (!autoResumeOnGain) return@launch
                    // Never take focus away while the other app is still playing.
                    if (isOtherAudioActive()) {
                        attempt++
                        if (attempt >= MAX_FOCUS_RECLAIM_ATTEMPTS) return@launch
                        continue
                    }
                    when (requestAudioFocusIfNeeded()) {
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                            if (autoResumeOnGain) {
                                autoResumeOnGain = false
                                _player?.let { p ->
                                    if (ducked) {
                                        p.volume = duckVolumeBeforePause
                                        ducked = false
                                    }
                                    p.play()
                                }
                            }
                            return@launch
                        }
                        // Delayed: Android queues the request and our listener
                        // gets AUDIOFOCUS_GAIN once the active app releases,
                        // which resumes playback there. Keep looping so we
                        // eventually resume even without that event.
                        AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                            attempt++
                            if (attempt >= MAX_FOCUS_RECLAIM_ATTEMPTS) return@launch
                        }
                        else -> {
                            attempt++
                            if (attempt >= MAX_FOCUS_RECLAIM_ATTEMPTS) return@launch
                        }
                    }
                }
            }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        val request = audioFocusRequest ?: return
        am.abandonAudioFocusRequest(request)
        audioFocusGranted = false
        if (ducked) {
            _player?.volume = duckVolumeBeforePause
            ducked = false
        }
    }

    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            val player = _player ?: return@OnAudioFocusChangeListener
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    audioFocusGranted = false
                    // Remember that we were playing so we can auto-resume when
                    // the other app / call finishes and focus returns.
                    autoResumeOnGain = player.isPlaying
                    player.pause()
                    if (autoResumeOnGain) {
                        scheduleAudioFocusReclaim()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    autoResumeOnGain = player.isPlaying
                    player.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    ducked = true
                    duckVolumeBeforePause = player.volume
                    player.volume = (player.volume * player.volume * 0.25f).coerceAtLeast(0.02f)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusReclaimJob?.cancel()
                    audioFocusGranted = true
                    if (ducked) {
                        player.volume = duckVolumeBeforePause
                        ducked = false
                    }
                    if (autoResumeOnGain) {
                        autoResumeOnGain = false
                        player.play()
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                    audioFocusGranted = true
                    if (ducked) {
                        player.volume = duckVolumeBeforePause
                        ducked = false
                    }
                }
            }
        }

    /** Whether the current playback path is a Cast receiver (no phone audio focus). */
    private fun isRemotePlayback(): Boolean = _state.value.castActive

    fun initialize(context: Context) {
        if (_player != null) return
        appContext = context.applicationContext
        val ctx = appContext ?: return

        val sessionActivity =
            PendingIntent.getActivity(
                ctx,
                0,
                Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val localPlayer = ExoPlayer.Builder(ctx).build()
        // Keep a media usage for the audio sink but disable media3's built-in focus
        // handling: this app owns audio focus explicitly (see audioFocusListener) so
        // another app starting to play pauses or ducks this station without a fight
        // between two focus requesters on the same attributes.
        localPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            false,
        )
        val castPlayer: CastPlayer? =
            runCatching {
                // CastPlayer plays locally via this ExoPlayer and automatically transfers
                // to a Cast receiver when a Cast session becomes available. The converter
                // supplies explicit live audio/HLS metadata for the receiver.
                val remotePlayer =
                    RemoteCastPlayer.Builder(ctx)
                        .setMediaItemConverter(OpenRadioMediaItemConverter())
                        .build()
                CastPlayer.Builder(ctx)
                    .setLocalPlayer(localPlayer)
                    .setRemotePlayer(remotePlayer)
                    .build()
            }.getOrNull()

        val player: Player = castPlayer ?: localPlayer
        isCastPlayer = castPlayer != null
        _player = player
        player.addListener(playerListener)

        _librarySession =
            MediaLibraryService.MediaLibrarySession.Builder(ctx, player, libraryCallback)
                .setId("openradio")
                .setSessionActivity(sessionActivity)
                .build()

        _state.update { it.copy(castAvailable = isCastPlayer, volume = Prefs.volume()) }
    }

    // ---- Playback control -------------------------------------------------

    fun playStation(
        station: Station,
        queue: List<Station>,
    ) {
        val p = _player ?: return
        val items = buildQueue(queue)
        if (items.isEmpty()) return
        val index = items.indexOfFirst { it.mediaId == station.id }
        if (index < 0) return
        ensureForegroundService()
        requestAudioFocusIfNeeded()
        _state.update {
            it.copy(
                currentStationId = station.id,
                currentStationName = station.name,
                loading = true,
                error = null,
                paused = false,
            )
        }
        p.setMediaItems(items, index, 0)
        p.prepare()
        p.play()

        App.log("Playing station ${station.id} (${station.name})")
    }

    /**
     * Starts [PlaybackService] as a foreground service so media3 posts a
     * now-playing notification with transport controls (and keeps playback alive
     * while the app is minimized / screen is locked).
     */
    private fun ensureForegroundService() {
        val ctx = appContext ?: return
        runCatching {
            ctx.startForegroundService(Intent(ctx, PlaybackService::class.java))
        }
    }

    /**
     * Brings up the media stack for a playback start.
     *
     * Playback does not only begin in [playStation]: Android Auto, the lock
     * screen, Bluetooth buttons, Wear OS and the alarm receiver all drive the
     * session directly, so the foreground service and audio focus have to be
     * claimed here as well. Without this, a start that originates outside the
     * app runs with no foreground service (so nothing keeps playback alive and
     * no media notification is posted) and without audio focus.
     */
    private fun onPlaybackStarting() {
        ensureForegroundService()
        requestAudioFocusIfNeeded()
        startMetadataPolling()
    }

    fun pause() {
        autoResumeOnGain = false
        focusReclaimJob?.cancel()
        _player?.pause()
        abandonAudioFocus()
        App.log("Paused station ${_state.value.currentStationId}")
    }

    fun resume() {
        focusReclaimJob?.cancel()
        requestAudioFocusIfNeeded()
        _player?.play()
        App.log("Resumed station ${_state.value.currentStationId}")
    }

    fun stop() {
        autoResumeOnGain = false
        focusReclaimJob?.cancel()
        _player?.let { p ->
            p.stop()
            p.clearMediaItems()
        }
        abandonAudioFocus()
        _state.update {
            it.copy(
                playing = false,
                paused = false,
                loading = false,
                currentStationId = null,
                currentStationName = null,
                nowPlayingTrack = null,
                nowPlayingArt = null,
            )
        }
        App.log("Stopped playback")
    }

    fun skipNext() {
        _player?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        _player?.seekToPreviousMediaItem()
    }

    fun setQueue(stations: List<Station>) {
        val p = _player ?: return
        val items = buildQueue(stations)
        if (items.isNotEmpty()) p.setMediaItems(items)
    }

    fun setVolume(volume: Float) {
        val p = _player ?: return
        p.volume = volume.coerceIn(0f, 1f)
        _state.update { it.copy(volume = p.volume, muted = p.volume == 0f) }
    }

    fun toggleMute() {
        val p = _player ?: return
        if (_state.value.muted) {
            p.volume = volumeBeforeMute.coerceIn(0f, 1f)
            _state.update { it.copy(volume = p.volume, muted = false) }
            Prefs.setVolume(p.volume)
        } else {
            volumeBeforeMute = p.volume
            p.volume = 0f
            _state.update { it.copy(volume = 0f, muted = true) }
            Prefs.setVolume(volumeBeforeMute)
        }
    }

    // ---- Now playing metadata (polled from the metadata endpoint) ---------

    /**
     * Keeps the now-playing track title and album art on the current media item
     * up to date.
     *
     * This deliberately lives here rather than in the UI view model: Android
     * Auto, the lock screen and the notification all read this metadata from the
     * session, and none of them ever create the activity that hosts the view
     * model. A view-model-owned poller therefore left every external surface
     * stuck on the station logo with no track information at all.
     */
    fun updateNowPlaying(
        display: String,
        artUrl: String?,
        stationLogo: String? = null,
    ) {
        val p = _player ?: return
        val item = p.currentMediaItem ?: return
        val index = p.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        // Fall back to the station logo so a track with no cover art shows the
        // channel logo instead of keeping the previous track's artwork.
        val art = artUrl?.takeIf { it.isNotBlank() } ?: stationLogo?.takeIf { it.isNotBlank() }
        if (display == _state.value.nowPlayingTrack && art == _state.value.nowPlayingArt) return
        val updatedMetadata =
            item.mediaMetadata.buildUpon()
                .setArtist(display.ifBlank { item.mediaMetadata.artist })
                .setArtworkUri(art?.let { Uri.parse(it) })
                .build()
        p.replaceMediaItem(index, item.buildUpon().setMediaMetadata(updatedMetadata).build())
        _state.update { it.copy(nowPlayingTrack = display, nowPlayingArt = art) }
        App.log("Now playing on ${_state.value.currentStationId}: ${display.ifBlank { "(track)" }}")
    }

    /**
     * True while playback has been requested, including the buffering window
     * where `isPlaying` is still false. Used as the liveness signal for the
     * metadata poll, which has to start before the first track can appear.
     */
    private fun isPlaybackRequested(): Boolean = _player?.playWhenReady == true

    private fun startMetadataPolling() {
        val stationId = _state.value.currentStationId ?: return
        if (metadataJob?.isActive == true && metadataStationId == stationId) return
        metadataJob?.cancel()
        metadataStationId = stationId
        metadataJob =
            mainScope.launch {
                while (isActive && isPlaybackRequested() && _state.value.currentStationId == stationId) {
                    val station = StationsStore.stations.value.firstOrNull { it.id == stationId }
                    val stream = station?.primaryStream
                    if (stream != null) {
                        val nowPlaying = metadataRepository.fetchNowPlaying(stream.url, station.metadataUrl)
                        if (nowPlaying != null) {
                            val art = artwork?.resolve(nowPlaying, station.name).orEmpty()
                            updateNowPlaying(nowPlaying.display, art, station.logo)
                        }
                    }
                    delay(METADATA_POLL_INTERVAL_MS)
                }
                metadataStationId = null
            }
    }

    private fun stopMetadataPolling() {
        metadataJob?.cancel()
        metadataJob = null
        metadataStationId = null
    }

    // ---- Chromecast -------------------------------------------------------

    // Note: Casting is driven by the system UI Output Switcher through
    // androidx.media3.cast.MediaRouteButton. The CastPlayer registers itself as a
    // media route provider, so selecting a device in the route chooser dialog
    // automatically transfers playback from the local ExoPlayer to the receiver.
    // castActive/castAvailable are surfaced to the UI via the player listener.

    // ---- Helpers ----------------------------------------------------------

    fun buildQueue(stations: List<Station>): List<MediaItem> {
        return stations.mapNotNull { station ->
            val stream = station.primaryStream ?: return@mapNotNull null
            stationToMediaItem(station, stream.url, stream.isHls)
        }
    }

    /** All language tags present in the station list, sorted, one browsable folder each. */
    fun languageTags(stations: List<Station>): List<String> = stations.flatMap { it.languageTags }.distinct().sorted()

    /** Stations whose language tags include [tag]. */
    fun stationsInLanguage(
        stations: List<Station>,
        tag: String,
    ): List<Station> = stations.filter { it.languageTags.contains(tag) }

    /** Stations whose ids are in [favoriteIds] (see [Prefs.favorites]). */
    fun favoriteStations(
        stations: List<Station>,
        favoriteIds: Set<String>,
    ): List<Station> = stations.filter { favoriteIds.contains(it.id) }

    private fun stationToMediaItem(
        station: Station,
        url: String,
        isHls: Boolean,
    ): MediaItem {
        val subtitle = listOf(station.city, station.language).filter { it.isNotBlank() }.joinToString(" • ")
        val metadata =
            MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist(subtitle.ifBlank { station.name })
                .setArtworkUri(station.logo.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        val builder =
            MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(url)
                .setMediaMetadata(metadata)
                .setLiveConfiguration(liveConfig())
        if (isHls) {
            // Use the media3 HLS mime type so local ExoPlayer routes this to the HLS
            // source (the .m3u8 URL alone would work, but an explicit mime is robust).
            // The Cast converter changes this to the receiver's HLS MIME type.
            builder.setMimeType(HLS_MIME_TYPE)
        }
        return builder.build()
    }

    private fun liveConfig(): MediaItem.LiveConfiguration =
        MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(20_000)
            .build()

    private fun retryWithFallbackStream() {
        // Cast owns loading and retry behavior on the receiver. Replacing the
        // item here on every remote error causes the receiver to restart the
        // same station in a loop, and can also advance the Cast queue.
        if (isRemotePlayback()) return
        val stationId = _state.value.currentStationId ?: return
        val station = StationsStore.stations.value.firstOrNull { it.id == stationId } ?: return
        val ctx = appContext
        val currentUrl = _player?.currentMediaItem?.localConfiguration?.uri?.toString()
        val fallback = station.preferredStreams().firstOrNull { it.url != currentUrl }
        if (fallback == null) {
            val reason =
                ctx?.getString(R.string.status_unreachable)
                    ?: "This station is not reachable right now."
            val noStream =
                ctx?.getString(R.string.status_no_stream)
                    ?: "No stream available"
            _state.update {
                it.copy(
                    error = reason,
                    retryStatus = if (station.streams.isEmpty()) noStream else null,
                )
            }
            return
        }
        val p = _player ?: return
        val trying =
            ctx?.getString(R.string.status_trying_backup)
                ?: "Main stream failed — trying backup…"
        _state.update { it.copy(loading = true, error = null, retryStatus = trying) }
        p.setMediaItem(stationToMediaItem(station, fallback.url, fallback.isHls))
        p.prepare()
        p.play()
    }

    private val playerListener =
        object : Player.Listener {
            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                // Fires for every play()/pause(), whoever asked for it, which is
                // what makes Android Auto and lock-screen starts behave like an
                // in-app start.
                if (playWhenReady) onPlaybackStarting() else stopMetadataPolling()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val p = _player ?: return
                if (isPlaying) onPlaybackStarting()
                val paused = !isPlaying && !p.playWhenReady && p.playbackState == Player.STATE_READY
                _state.update {
                    it.copy(
                        playing = isPlaying,
                        paused = paused,
                        loading = if (isPlaying) false else it.loading,
                        error = if (isPlaying) null else it.error,
                        retryStatus = if (isPlaying) null else it.retryStatus,
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        loading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE,
                        error = if (playbackState == Player.STATE_READY) null else it.error,
                    )
                }
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                val id = mediaItem?.mediaId
                val name = mediaItem?.mediaMetadata?.title?.toString()
                _state.update {
                    it.copy(
                        currentStationId = id,
                        currentStationName = name,
                        nowPlayingTrack = mediaItem?.mediaMetadata?.artist?.toString(),
                        nowPlayingArt = mediaItem?.mediaMetadata?.artworkUri?.toString(),
                    )
                }
                // A new station has no track metadata yet; drop any polling left
                // over from the previous station.
                if (isPlaybackRequested()) {
                    startMetadataPolling()
                } else {
                    stopMetadataPolling()
                }
            }

            override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
                _state.update {
                    it.copy(castActive = deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE)
                }
            }

            override fun onVolumeChanged(volume: Float) {
                if (!_state.value.muted) {
                    _state.update { it.copy(volume = volume) }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                App.reportError(error, "Playback error for station ${_state.value.currentStationId}")
                _state.update { it.copy(error = error.message, playing = false, loading = false, retryStatus = null) }
                retryWithFallbackStream()
            }
        }

    // ---- Android Auto / MediaBrowser library ------------------------------

    private fun folderItem(
        mediaId: String,
        title: String,
    ): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    /**
     * Runs [supplier] once the station database is available and hands the result
     * back to the browser.
     *
     * Media browser callbacks arrive on the app thread, so the wait happens on a
     * background scope rather than blocking it. Without it a cold start from
     * Android Auto answered every browse and play request from a still-loading
     * store, which is what left the car with an empty tree and no playable
     * station.
     */
    private fun <T> whenStationsLoaded(supplier: () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        ioScope.launch {
            try {
                StationsStore.awaitReady()
                future.set(supplier())
            } catch (e: Exception) {
                App.reportError(e, "Media browser callback failed")
                future.setException(e)
            }
        }
        return future
    }

    private val libraryCallback =
        object : MediaLibraryService.MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                // Android Auto requests the "suggested" list separately and shows
                // it prominently on the home screen; serve favorites there.
                val suggested = params?.isSuggested == true
                val root =
                    MediaItem.Builder()
                        .setMediaId(if (suggested) SUGGESTED_MEDIA_ID else ROOT_MEDIA_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(appContext?.getString(R.string.app_name) ?: "OpenRadio-IN")
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .build(),
                        )
                        .build()
                return Futures.immediateFuture(LibraryResult.ofItem(root, params))
            }

            override fun onGetChildren(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return whenStationsLoaded { childrenFor(parentId, params) }
            }

            override fun onGetItem(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val folderTitle =
                    when (mediaId) {
                        ROOT_MEDIA_ID -> appContext?.getString(R.string.app_name) ?: "OpenRadio-IN"
                        ALL_MEDIA_ID -> appContext?.getString(R.string.all_stations) ?: "All stations"
                        FAVORITES_MEDIA_ID -> appContext?.getString(R.string.favorites) ?: "Favorites"
                        LANGUAGES_MEDIA_ID -> appContext?.getString(R.string.language) ?: "Language"
                        SUGGESTED_MEDIA_ID -> appContext?.getString(R.string.favorite_stations) ?: "Favorite Stations"
                        else -> null
                    }
                if (folderTitle != null) {
                    return Futures.immediateFuture(
                        LibraryResult.ofItem(folderItem(mediaId, folderTitle), null),
                    )
                }
                if (mediaId.startsWith(LANGUAGE_FOLDER_PREFIX)) {
                    return Futures.immediateFuture(
                        LibraryResult.ofItem(folderItem(mediaId, mediaId.removePrefix(LANGUAGE_FOLDER_PREFIX)), null),
                    )
                }
                return whenStationsLoaded {
                    val station = StationsStore.stations.value.firstOrNull { it.id == mediaId }
                    val stream = station?.primaryStream
                    if (station != null && stream != null) {
                        LibraryResult.ofItem(stationToMediaItem(station, stream.url, stream.isHls), null)
                    } else {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    }
                }
            }

            override fun onSearch(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<Void>> {
                return Futures.immediateFuture(LibraryResult.ofVoid(params))
            }

            override fun onGetSearchResult(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val q = query.trim()
                return whenStationsLoaded {
                    val stations =
                        StationsStore.stations.value.filter {
                            it.name.contains(q, ignoreCase = true) ||
                                it.language.contains(q, ignoreCase = true) ||
                                it.city.contains(q, ignoreCase = true) ||
                                it.categories.any { c -> c.contains(q, ignoreCase = true) }
                        }
                    LibraryResult.ofItemList(buildQueue(stations), params)
                }
            }

            override fun onAddMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>,
            ): ListenableFuture<List<MediaItem>> {
                // Pure resolver: one playable item back per requested item, in the
                // same order. Growing the list here is what used to make Android
                // Auto start on the wrong station, because the session then applies
                // the caller's start index to a playlist that no longer lines up
                // with the request.
                return whenStationsLoaded {
                    resolveStations(mediaItems.map { it.mediaId })
                        ?: mediaItems.map { it.buildUpon().setLiveConfiguration(liveConfig()).build() }
                }
            }

            @androidx.annotation.OptIn(UnstableApi::class) // MediaSession.MediaItemsWithStartPosition
            override fun onSetMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>,
                startIndex: Int,
                startPositionMs: Long,
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return whenStationsLoaded {
                    val requestedIds = mediaItems.mapNotNull { it.mediaId }
                    val targetId = mediaItems.getOrNull(startIndex)?.mediaId ?: requestedIds.firstOrNull()
                    val queue = contextQueue(requestedIds)
                    val targetIndex = queue.indexOfFirst { it.mediaId == targetId }
                    if (queue.isEmpty() || targetId == null || targetIndex < 0) {
                        val items = mediaItems.map { it.buildUpon().setLiveConfiguration(liveConfig()).build() }
                        val start = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                        MediaSession.MediaItemsWithStartPosition(items, start, startPositionMs)
                    } else {
                        // Rotate the context queue so the tapped station sits at the
                        // index the controller asked to start from. Without this the
                        // queue is installed verbatim and playback begins on its first
                        // entry, which is how Auto kept playing the wrong station.
                        val ordered = rotateTo(queue, targetIndex, startIndex)
                        MediaSession.MediaItemsWithStartPosition(ordered, startIndex, startPositionMs)
                    }
                }
            }
        }

    /**
     * The browsable children of [parentId]: the top-level folders, a station
     * list, the language folders, or the stations of one language.
     */
    private fun childrenFor(
        parentId: String,
        params: MediaLibraryService.LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
        val stations = StationsStore.stations.value
        val items: List<MediaItem> =
            when (parentId) {
                ROOT_MEDIA_ID ->
                    listOf(
                        folderItem(ALL_MEDIA_ID, string(R.string.all_stations, "All stations")),
                        folderItem(FAVORITES_MEDIA_ID, string(R.string.favorites, "Favorites")),
                        folderItem(LANGUAGES_MEDIA_ID, string(R.string.language, "Language")),
                    )
                ALL_MEDIA_ID -> buildQueue(stations)
                FAVORITES_MEDIA_ID,
                SUGGESTED_MEDIA_ID,
                ->
                    buildQueue(favoriteStations(stations, Prefs.favorites()))
                LANGUAGES_MEDIA_ID ->
                    languageTags(stations).map { tag ->
                        folderItem(languageFolderMediaId(tag), tag)
                    }
                else ->
                    if (parentId.startsWith(LANGUAGE_FOLDER_PREFIX)) {
                        val tag = parentId.removePrefix(LANGUAGE_FOLDER_PREFIX)
                        buildQueue(stationsInLanguage(stations, tag))
                    } else {
                        return LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    }
            }
        return LibraryResult.ofItemList(items, params)
    }

    private fun string(
        resId: Int,
        fallback: String,
    ): String = appContext?.getString(resId) ?: fallback

    /**
     * Playable [MediaItem]s for [mediaIds], or null when any id is unknown or has
     * no playable stream.
     */
    internal fun resolveStations(mediaIds: List<String?>): List<MediaItem>? {
        if (mediaIds.isEmpty() || mediaIds.any { it.isNullOrBlank() }) return null
        val stations = StationsStore.stations.value
        return mediaIds.map { id ->
            val station = stations.firstOrNull { it.id == id } ?: return null
            val stream = station.primaryStream ?: return null
            stationToMediaItem(station, stream.url, stream.isHls)
        }
    }

    /**
     * The queue a request came from, so next/previous walk the same list the
     * user was browsing: the favorites list when every requested station is a
     * favorite, otherwise the full station list.
     */
    internal fun contextQueue(requestedIds: List<String>): List<MediaItem> {
        val stations = StationsStore.stations.value
        val allFavorites = requestedIds.isNotEmpty() && requestedIds.all { Prefs.favorites().contains(it) }
        val source = if (allFavorites) favoriteStations(stations, Prefs.favorites()) else stations
        return buildQueue(source)
    }

    /**
     * Reorders [queue] so the item at [targetIndex] ends up at [fromIndex].
     *
     * The queue is rotated as a ring, so every station still appears exactly once
     * and the tapped station's immediate neighbours stay next to it. That matters
     * because the caller installs this as the playlist and then starts at
     * [fromIndex]: installing the queue verbatim is what made Android Auto begin
     * playback on the first station of the list instead of the selected one.
     */
    internal fun rotateTo(
        queue: List<MediaItem>,
        targetIndex: Int,
        fromIndex: Int,
    ): List<MediaItem> {
        if (queue.isEmpty() || fromIndex < 0 || fromIndex >= queue.size) return queue
        val target = targetIndex.coerceIn(0, queue.size - 1)
        if (target == fromIndex) return queue
        val size = queue.size
        // Walk backwards from the target for the slots that precede it, wrapping
        // around the end of the queue when the queue is shorter than the start index.
        val head = (1..fromIndex).map { queue[Math.floorMod(target - it, size)] }
        val tail = (0 until size - 1 - fromIndex).map { queue[(target + 1 + it) % size] }
        return head + queue[target] + tail
    }
}
