package dev.openradio.android.playback

import android.content.Intent
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

/**
 * Exposes the [AppPlayer] session to Android Auto / other MediaBrowser
 * controllers. Auto browses the station list via [AppPlayer]'s
 * [MediaLibraryService.MediaLibrarySession.Callback]; playback commands are
 * handled by the shared player. The session is created once per process, so
 * this service is a thin shell that just returns it.
 */
class PlaybackService : MediaLibraryService() {

    override fun onCreate() {
        super.onCreate()
        AppPlayer.initialize(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession? {
        return AppPlayer.librarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = AppPlayer.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
