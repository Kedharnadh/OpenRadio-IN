package dev.openradio.android.playback

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Cast options provider referenced from the manifest so the Cast framework can
 * create [com.google.android.gms.cast.framework.CastContext] before the first
 * [com.google.android.gms.cast.framework.CastContext.getSharedInstance] call.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        // Disable session resume / reconnection so CastPlayer never attaches to a
        // stale or foreign Cast session (e.g. one started by the web PWA). Media3's
        // RemoteCastPlayer crashes with a NullPointerException when it tries to read
        // a session media item that does not carry media3 custom data.
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setResumeSavedSession(false)
            .setEnableReconnectionService(false)
            .setStopReceiverApplicationWhenEndingSession(false)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}
