package dev.openradio.android.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.openradio.android.R
import dev.openradio.android.data.Station
import dev.openradio.android.data.StationsStore
import dev.openradio.android.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val castAvailable: Boolean = false
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

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var _player: Player? = null
    private var _librarySession: MediaLibraryService.MediaLibrarySession? = null
    private var isCastPlayer = false
    private var volumeBeforeMute = 1f

    val player: Player? get() = _player
    val librarySession: MediaLibraryService.MediaLibrarySession? get() = _librarySession

    fun initialize(context: Context) {
        if (_player != null) return
        appContext = context.applicationContext
        val ctx = appContext ?: return

        val sessionActivity = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val localPlayer = ExoPlayer.Builder(ctx).build()
        val castPlayer: CastPlayer? = runCatching {
            // CastPlayer plays locally via this ExoPlayer and automatically transfers
            // to a Cast receiver when a Cast session becomes available.
            val remotePlayer = RemoteCastPlayer.Builder(ctx)
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

        _librarySession = MediaLibraryService.MediaLibrarySession.Builder(ctx, player, libraryCallback)
            .setId("openradio")
            .setSessionActivity(sessionActivity)
            .build()

        _state.update { it.copy(castAvailable = isCastPlayer) }
    }

    // ---- Playback control -------------------------------------------------

    fun playStation(station: Station, queue: List<Station>) {
        val p = _player ?: return
        val items = buildQueue(queue)
        if (items.isEmpty()) return
        val index = items.indexOfFirst { it.mediaId == station.id }
        if (index < 0) return
        ensureForegroundService()
        _state.update {
            it.copy(
                currentStationId = station.id,
                currentStationName = station.name,
                loading = true,
                error = null,
                paused = false
            )
        }
        p.setMediaItems(items, index, 0)
        p.prepare()
        p.play()
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
        _player?.pause()
    }

    fun resume() {
        _player?.play()
    }

    fun stop() {
        _player?.let { p ->
            p.stop()
            p.clearMediaItems()
        }
        _state.update {
            it.copy(
                playing = false,
                paused = false,
                loading = false,
                currentStationId = null,
                currentStationName = null,
                nowPlayingTrack = null,
                nowPlayingArt = null
            )
        }
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
        } else {
            volumeBeforeMute = p.volume
            p.volume = 0f
            _state.update { it.copy(volume = 0f, muted = true) }
        }
    }

    // ---- Now playing metadata (polled from the metadata endpoint) ---------

    fun updateNowPlaying(title: String?, artUrl: String?) {        val p = _player ?: return
        val item = p.currentMediaItem ?: return
        val index = p.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        val updatedMetadata = item.mediaMetadata.buildUpon()
            .setArtist(title ?: item.mediaMetadata.artist)
            .setArtworkUri(artUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) } ?: item.mediaMetadata.artworkUri)
            .build()
        p.replaceMediaItem(index, item.buildUpon().setMediaMetadata(updatedMetadata).build())
        _state.update { it.copy(nowPlayingTrack = title, nowPlayingArt = artUrl) }
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

    private fun stationToMediaItem(station: Station, url: String, isHls: Boolean): MediaItem {
        val subtitle = listOf(station.city, station.language).filter { it.isNotBlank() }.joinToString(" • ")
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(subtitle.ifBlank { station.name })
            .setArtworkUri(station.logo.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build()
        val builder = MediaItem.Builder()
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
        val fallback = station.streams
            .sortedWith(compareByDescending<Station.Stream> { it.isHls }.thenBy { it.priority })
            .firstOrNull { it.url != currentUrl }
        if (fallback == null) {
            val reason = ctx?.getString(R.string.status_unreachable)
                ?: "This station is not reachable right now."
            val noStream = ctx?.getString(R.string.status_no_stream)
                ?: "No stream available"
            _state.update {
                it.copy(
                    error = reason,
                    retryStatus = if (station.streams.isEmpty()) noStream else null
                )
            }
            return
        }
        val p = _player ?: return
        val trying = ctx?.getString(R.string.status_trying_backup)
            ?: "Main stream failed — trying backup…"
        _state.update { it.copy(loading = true, error = null, retryStatus = trying) }
        p.setMediaItem(stationToMediaItem(station, fallback.url, fallback.isHls))
        p.prepare()
        p.play()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val p = _player ?: return
            val paused = !isPlaying && !p.playWhenReady && p.playbackState == Player.STATE_READY
            _state.update {
                it.copy(
                    playing = isPlaying,
                    paused = paused,
                    loading = if (isPlaying) false else it.loading,
                    error = if (isPlaying) null else it.error,
                    retryStatus = if (isPlaying) null else it.retryStatus
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    loading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE,
                    error = if (playbackState == Player.STATE_READY) null else it.error
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId
            val name = mediaItem?.mediaMetadata?.title?.toString()
            _state.update {
                it.copy(
                    currentStationId = id,
                    currentStationName = name,
                    nowPlayingTrack = mediaItem?.mediaMetadata?.artist?.toString(),
                    nowPlayingArt = mediaItem?.mediaMetadata?.artworkUri?.toString()
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
            _state.update { it.copy(error = error.message, playing = false, loading = false, retryStatus = null) }
            retryWithFallbackStream()
        }
    }

    // ---- Android Auto / MediaBrowser library ------------------------------

    private val libraryCallback = object : MediaLibraryService.MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_MEDIA_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(appContext?.getString(R.string.app_name) ?: "OpenRadio-IN")
                        .build()
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
            params: MediaLibraryService.LibraryParams?
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
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = StationsStore.stations.value.firstOrNull { it.id == mediaId }
            val stream = station?.primaryStream
            return if (station != null && stream != null) {
                Futures.immediateFuture(LibraryResult.ofItem(AppPlayer.stationToMediaItem(station, stream.url, stream.isHls), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onSearch(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val q = query.trim()
            val stations = StationsStore.stations.value.filter {
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
            mediaItems: List<MediaItem>
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
