package dev.openradio.android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import dev.openradio.android.data.StationsStore
import dev.openradio.android.playback.AppPlayer
import java.util.Locale

class App : Application() {

    override fun attachBaseContext(base: Context) {
        Prefs.init(base)
        super.attachBaseContext(
            LocaleManager.apply(base, Prefs.uiLanguage())
        )
    }

    override fun onCreate() {
        super.onCreate()
        AppPlayer.initialize(this)
        AppPlayer.setVolume(Prefs.volume())
        StationsStore.ensureLoaded(this)
    }
}
