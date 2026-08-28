package dev.openradio.android

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Applies the user-selected in-app UI language (en / te / hi) independently of
 * the device system locale, mirroring the PWA's language selector.
 */
object LocaleManager {

    fun apply(context: Context, language: String): Context {
        val locale = when (language) {
            "te" -> Locale("te")
            "hi" -> Locale("hi")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun supportsLanguage(language: String): Boolean =
        language == "en" || language == "te" || language == "hi"

    fun currentLanguage(): String = Prefs.uiLanguage().takeIf { supportsLanguage(it) } ?: "en"

    @Suppress("DEPRECATION")
    fun createLocaleOverrideForApiBelow24(context: Context, language: String): Context {
        val locale = when (language) {
            "te" -> Locale("te")
            "hi" -> Locale("hi")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }
}
