package dev.openradio.android

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/** Tiny persistence for favorites, recent stations and volume. */
object Prefs {

    private const val FILE = "openradio_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENTS = "recents"
    private const val KEY_VOLUME = "volume"
    private const val KEY_UI_LANG = "ui_lang"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun favorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun setFavorites(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, ids).apply()
    }

    fun recents(): List<String> {
        val json = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    fun saveRecents(ids: List<String>) {
        val array = JSONArray()
        ids.take(20).forEach { array.put(it) }
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }

    fun volume(): Float = prefs.getFloat(KEY_VOLUME, 1f)

    fun setVolume(volume: Float) {
        prefs.edit().putFloat(KEY_VOLUME, volume).apply()
    }

    fun uiLanguage(): String = prefs.getString(KEY_UI_LANG, "en") ?: "en"

    fun setUiLanguage(lang: String) {
        prefs.edit().putString(KEY_UI_LANG, lang).apply()
    }
}
