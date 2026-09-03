package dev.openradio.android.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import dev.openradio.android.R
import dev.openradio.android.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Exposes the [AppPlayer] session to Android Auto / other MediaBrowser
 * controllers and owns the now-playing foreground notification.
 *
 * The session is created once per process ([AppPlayer.librarySession]), so this
 * service also posts the media notification itself. Because the player may
 * already be playing by the time the service starts, we call [startForeground]
 * deterministically from [onStartCommand] instead of relying on the automatic
 * foreground handling that media3 applies to sessions created inside the
 * service (that race otherwise throws
 * `ForegroundServiceDidNotStartInTimeException`).
 */
class PlaybackService : MediaLibraryService() {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "openradio_playback"
        private const val ACTION_PLAY_PAUSE = "dev.openradio.android.action.PLAY_PAUSE"
        private const val ACTION_SKIP_NEXT = "dev.openradio.android.action.SKIP_NEXT"
        private const val ACTION_SKIP_PREV = "dev.openradio.android.action.SKIP_PREV"
        private const val ACTION_STOP = "dev.openradio.android.action.STOP"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var cachedArtwork: Bitmap? = null
    private var loadedArtworkUrl: String? = null

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = updateNotification()

            override fun onPlaybackStateChanged(playbackState: Int) = updateNotification()

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                cachedArtwork = null
                loadedArtworkUrl = null
                updateNotification()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = updateNotification()
        }

    override fun onCreate() {
        super.onCreate()
        AppPlayer.initialize(this)
        createNotificationChannel()
        AppPlayer.player?.addListener(playerListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession? {
        return AppPlayer.librarySession
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        handleAction(intent?.action)
        val player = AppPlayer.player
        if (player != null) {
            startForeground(NOTIFICATION_ID, buildNotification(player))
        }
        return START_NOT_STICKY
    }

    private fun handleAction(action: String?) {
        val player = AppPlayer.player ?: return
        when (action) {
            ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_SKIP_NEXT -> player.seekToNextMediaItem()
            ACTION_SKIP_PREV -> player.seekToPreviousMediaItem()
            ACTION_STOP -> {
                AppPlayer.stop()
                stopSelf()
            }
        }
    }

    private fun updateNotification() {
        val player = AppPlayer.player ?: return
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(player))
        } catch (_: SecurityException) {
        }
    }

    private fun buildNotification(player: Player): android.app.Notification {
        val session = AppPlayer.librarySession
        val metadata = player.currentMediaItem?.mediaMetadata
        val title =
            metadata?.title?.toString()?.takeIf { it.isNotBlank() }
                ?: getString(R.string.app_name)
        val subtitle = metadata?.artist?.toString()?.takeIf { it.isNotBlank() }
        val artUrl = metadata?.artworkUri?.toString()

        if (!artUrl.isNullOrBlank() && artUrl != loadedArtworkUrl) {
            loadedArtworkUrl = artUrl
            cachedArtwork = null
            loadArtworkAsync(artUrl)
        }

        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_radio)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(player.isPlaying)
                .addAction(
                    R.drawable.ic_stat_radio,
                    ACTION_SKIP_PREV,
                    actionIntent(ACTION_SKIP_PREV, 2),
                )
                .addAction(
                    R.drawable.ic_stat_radio,
                    if (player.isPlaying) getString(R.string.pause) else getString(R.string.play),
                    actionIntent(ACTION_PLAY_PAUSE, 1),
                )
                .addAction(
                    R.drawable.ic_stat_radio,
                    ACTION_SKIP_NEXT,
                    actionIntent(ACTION_SKIP_NEXT, 3),
                )

        val largeIcon =
            cachedArtwork
                ?: BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_stat_radio,
                )
        largeIcon?.let { builder.setLargeIcon(it) }

        if (session != null) {
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2),
            )
        }

        return builder.build()
    }

    private fun actionIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun loadArtworkAsync(url: String) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { downloadBitmap(url) }
            if (bitmap != null) {
                cachedArtwork = bitmap
                updateNotification()
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? =
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.instanceFollowRedirects = true
            val input = connection.inputStream
            try {
                BitmapFactory.decodeStream(java.io.BufferedInputStream(input))
            } finally {
                input.close()
                connection.disconnect()
            }
        }.getOrNull()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Playback",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Controls for live radio playback"
                    setShowBadge(false)
                }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
