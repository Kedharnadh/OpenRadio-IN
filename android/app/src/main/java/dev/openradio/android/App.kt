package dev.openradio.android

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dev.openradio.android.data.StationsStore
import dev.openradio.android.playback.AppPlayer

class App : Application() {
    override fun attachBaseContext(base: Context) {
        Prefs.init(base)
        super.attachBaseContext(
            LocaleManager.apply(base, Prefs.uiLanguage()),
        )
    }

    override fun onCreate() {
        super.onCreate()
        AppPlayer.initialize(this)
        AppPlayer.setVolume(Prefs.volume())
        StationsStore.ensureLoaded(this)
    }

    companion object {
        private const val TAG = "OpenRadio"

        /** Log a non-fatal error to Crashlytics and logcat. */
        fun reportError(
            throwable: Throwable,
            message: String = "",
        ) {
            Log.e(TAG, message, throwable)
            Firebase.crashlytics.apply {
                if (message.isNotBlank()) {
                    setCustomKey("error_message", message)
                }
                recordException(throwable)
            }
        }

        /** Log a non-fatal message to Crashlytics (breadcrumb) and logcat. */
        fun log(message: String) {
            Log.d(TAG, message)
            Firebase.crashlytics.log(message)
        }
    }
}
