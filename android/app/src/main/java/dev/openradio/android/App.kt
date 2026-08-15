package dev.openradio.android

import android.app.Application
import dev.openradio.android.data.StationsStore
import dev.openradio.android.playback.AppPlayer

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        AppPlayer.initialize(this)
        AppPlayer.setVolume(Prefs.volume())
        StationsStore.ensureLoaded(this)
    }
}
