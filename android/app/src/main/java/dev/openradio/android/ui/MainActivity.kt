package dev.openradio.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.openradio.android.LocaleManager
import dev.openradio.android.Prefs
import dev.openradio.android.playback.AppPlayer
import dev.openradio.android.ui.screens.HomeScreen
import dev.openradio.android.ui.theme.OpenRadioTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.apply(newBase, Prefs.uiLanguage()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPlayer.initialize(this)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
        setContent {
            OpenRadioTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onLanguageChanged = {
                        Prefs.setUiLanguage(it)
                        recreate()
                    },
                )
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
    }
}
