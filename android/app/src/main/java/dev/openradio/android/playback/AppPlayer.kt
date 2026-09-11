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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.openradio.android.App
import dev.openradio.android.BuildConfig
import dev.openradio.android.Prefs
import dev.openradio.android.R
import dev.openradio.android.data.HttpClient
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
import okhttp3.Request
import org.json.JSONObject
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
 * Process-wide playback owner. A single [CastPlayer] (backed by an internal
 * ExoPlayer for local playback) is wrapped in one [androidx.media3.session.MediaSession],
 * so local playback, Chromecast and Android Auto all share the same player and
 * timeline. Falls back to a plain ExoPlayer when Google Play services (Cast) is
 * unavailable.
 */
object AppPlayer {
    const val ROOT_MEDIA_ID = "openradio_root"

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

    /**
     * Cache of proxy-resolved details for the current HLS stream, populated
     * asynchronously (see [probeHlsCastStream]) and consumed by the Cast converter
     * so it can route HLS through the worker — exactly like the PWA.
     */
    object HlsCastProxy {
        val base: String = BuildConfig.HLS_PROXY_URL

        @Volatile var probedForUrl: String? = null

        @Volatile var resolvedUrl: String? = null

        @Volatile var contentType: String? = null

        /** Continuous proxy stream URL for a given HLS source. */
        fun streamUrl(original: String): String {
            // Only reuse the probe result for the exact URL it was fetched for, so a
            // slow/stale probe from another station never leaks the wrong stream into
            // this one. Falls back to the original URL + worker-inferred content type.
            val sameSource = probedForUrl == original
            val target = if (sameSource) (resolvedUrl ?: original) else original
            val params =
                buildString {
                    append(urlParam("url", target))
                    if (sameSource) {
                        contentType?.takeIf { it.isNotBlank() }?.let {
                            append("&").append(urlParam("contentType", it))
                        }
                    }
                }
            return "$base?$params"
        }

        private fun urlParam(
            key: String,
            value: String,
        ): String = "${Uri.encode(key)}=${Uri.encode(value)}"
    }

    /** Fires off a background probe of the HLS proxy for the given stream. */
    fun probeHlsCastStream(
        stationId: String,
        hlsUrl: String,
    ) {
        ioScope.launch {
            runCatching {
                val probeUrl = "${HlsCastProxy.base}?probe=1&url=${Uri.encode(hlsUrl)}"
                val request = Request.Builder().url(probeUrl).build()
                val body =
                    HttpClient.client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        response.body?.string()
                    } ?: return@launch
                val obj = JSONObject(body)
                HlsCastProxy.probedForUrl = hlsUrl
                HlsCastProxy.resolvedUrl = obj.optString("url").takeIf { it.isNotBlank() }
                HlsCastProxy.contentType = obj.optString("contentType").takeIf { it.isNotBlank() }
            }
        }
    }

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
                // to a Cast receiver when a Cast session becomes available. HLS items are converted for the receiver.
                // sent with the HLS mime type (application/x-mpegURL) so the default Cast
                // receiver plays them using its native HLS pipeline — no proxy involved.
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

        // Resolve the HLS stream through the proxy worker (like the PWA) so the
        // Cast converter can hand the receiver a flattened continuous stream.
        station.primaryStream?.takeIf { it.isHls }?.let { stream ->
            probeHlsCastStream(station.id, stream.url)
        }
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

    fun updateNowPlaying(
        title: String?,
        artUrl: String?,
    ) {
        val p = _player ?: return
        val item = p.currentMediaItem ?: return
        val index = p.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        val updatedMetadata =
            item.mediaMetadata.buildUpon()
                .setArtist(title ?: item.mediaMetadata.artist)
                .setArtworkUri(
                    artUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) } ?: item.mediaMetadata.artworkUri,
                )
                .build()
        p.replaceMediaItem(index, item.buildUpon().setMediaMetadata(updatedMetadata).build())
        _state.update { it.copy(nowPlayingTrack = title, nowPlayingArt = artUrl) }
        App.log("Now playing on ${_state.value.currentStationId}: ${title ?: "(track)"}")
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
            // The cast converter rewrites HLS items to the proxy/audio-mpeg for the
            // default Cast receiver.
            builder.setMimeType(HLS_MIME_TYPE)
        }
        return builder.build()
    }

    private fun liveConfig(): MediaItem.LiveConfiguration =
        MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(20_000)
            .build()

    private fun retryWithFallbackStream() {
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
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val p = _player ?: return
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

    private val libraryCallback =
        object : MediaLibraryService.MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val root =
                    MediaItem.Builder()
                        .setMediaId(ROOT_MEDIA_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(appContext?.getString(R.string.app_name) ?: "OpenRadio-IN")
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
                if (parentId != ROOT_MEDIA_ID) {
                    return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
                val items = AppPlayer.buildQueue(StationsStore.stations.value)
                return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
            }

            override fun onGetItem(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val station = StationsStore.stations.value.firstOrNull { it.id == mediaId }
                val stream = station?.primaryStream
                return if (station != null && stream != null) {
                    Futures.immediateFuture(
                        LibraryResult.ofItem(AppPlayer.stationToMediaItem(station, stream.url, stream.isHls), null),
                    )
                } else {
                    Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
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
                val stations =
                    StationsStore.stations.value.filter {
                        it.name.contains(q, ignoreCase = true) ||
                            it.language.contains(q, ignoreCase = true) ||
                            it.city.contains(q, ignoreCase = true) ||
                            it.categories.any { c -> c.contains(q, ignoreCase = true) }
                    }
                return Futures.immediateFuture(LibraryResult.ofItemList(AppPlayer.buildQueue(stations), params))
            }

            override fun onAddMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>,
            ): ListenableFuture<List<MediaItem>> {
                val queue = AppPlayer.buildQueue(StationsStore.stations.value)
                val requested = mediaItems.firstOrNull()?.mediaId
                return if (requested != null && queue.any { it.mediaId == requested }) {
                    // Return the full queue so Auto's next/previous walks the station list.
                    Futures.immediateFuture(queue)
                } else {
                    val items = mediaItems.map { it.buildUpon().setLiveConfiguration(liveConfig()).build() }
                    Futures.immediateFuture(items)
                }
            }
        }
}
