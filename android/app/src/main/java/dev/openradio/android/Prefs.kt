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
    private const val KEY_FILTER_LANG = "filter_lang"
    private const val KEY_ALARM_TIME = "alarm_time_millis"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun favorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

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

    /** Last language selected in the station language filter (null = "All"). */
    fun filterLanguage(): String? = prefs.getString(KEY_FILTER_LANG, null)

    fun setFilterLanguage(lang: String?) {
        val edit = prefs.edit()
        if (lang == null) edit.remove(KEY_FILTER_LANG) else edit.putString(KEY_FILTER_LANG, lang)
        edit.apply()
    }

    /** Scheduled alarm fire time in epoch millis, or null when no alarm is set. */
    fun alarmTimeMillis(): Long? {
        val value = prefs.getLong(KEY_ALARM_TIME, -1L)
        return if (value < 0) null else value
    }

    fun setAlarmTimeMillis(millis: Long?) {
        val edit = prefs.edit()
        if (millis == null) edit.remove(KEY_ALARM_TIME) else edit.putLong(KEY_ALARM_TIME, millis)
        edit.apply()
    }
}
