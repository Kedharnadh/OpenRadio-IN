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
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setEnableReconnectionService(true)
            .setStopReceiverApplicationWhenEndingSession(false)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}
