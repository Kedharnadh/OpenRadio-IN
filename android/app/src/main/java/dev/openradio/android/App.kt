package dev.openradio.android

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
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
        // Firebase (Crashlytics) is optional: it's only available when a
        // google-services.json config is present at build time.
        crashReportingEnabled = runCatching { FirebaseApp.getInstance() }.isSuccess
    }

    companion object {
        private const val TAG = "OpenRadio"

        /** Whether a Firebase app (Crashlytics) is configured for this build. */
        private var crashReportingEnabled = false

        /** Log a non-fatal error to Crashlytics and logcat. */
        fun reportError(
            throwable: Throwable,
            message: String = "",
        ) {
            Log.e(TAG, message, throwable)
            if (!crashReportingEnabled) return
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
            if (crashReportingEnabled) {
                Firebase.crashlytics.log(message)
            }
        }
    }
}
