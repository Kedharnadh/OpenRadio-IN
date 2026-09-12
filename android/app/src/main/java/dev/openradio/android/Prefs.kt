package dev.openradio.android

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

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

    /**
     * Serialize the user-editable preferences (favorites, recents, UI/station
     * language, volume and alarm) into a portable JSON string for backup/restore.
     */
    fun exportJson(): String =
        runCatching {
            JSONObject()
                .put("format", EXPORT_FORMAT_VERSION)
                .put("favorites", JSONArray(favorites()))
                .put("recents", JSONArray(recents()))
                .put("volume", volume().toDouble())
                .put("ui_lang", uiLanguage())
                .apply {
                    filterLanguage()?.let { put("filter_lang", it) }
                    alarmTimeMillis()?.let { put("alarm_time_millis", it) }
                }
                .toString()
        }.getOrDefault("")

    /** Restore preferences from a JSON string produced by [exportJson]. */
    fun importJson(json: String): Boolean {
        if (json.isBlank()) return false
        return try {
            val root = JSONObject(json)
            if (root.optInt("format", -1) != EXPORT_FORMAT_VERSION) return false
            val edit = prefs.edit()

            root.optJSONArray("favorites")?.let { array ->
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) set.add(array.getString(i))
                edit.putStringSet(KEY_FAVORITES, set)
            }

            root.optJSONArray("recents")?.let { array ->
                val recents = JSONArray()
                for (i in 0 until array.length().coerceAtMost(20)) {
                    val id = array.optString(i)
                    if (id.isNotBlank()) recents.put(id)
                }
                edit.putString(KEY_RECENTS, recents.toString())
            }

            if (root.has("volume")) edit.putFloat(KEY_VOLUME, root.optDouble("volume", 1.0).toFloat())

            val uiLang = root.optString("ui_lang", "")
            if (uiLang.isNotBlank() && LocaleManager.supportsLanguage(uiLang)) {
                edit.putString(KEY_UI_LANG, uiLang)
            }

            root.optString("filter_lang", "").takeIf { it.isNotBlank() }?.let {
                edit.putString(KEY_FILTER_LANG, it)
            }

            if (root.has("alarm_time_millis")) {
                val millis = root.optLong("alarm_time_millis", -1L)
                if (millis < 0) edit.remove(KEY_ALARM_TIME) else edit.putLong(KEY_ALARM_TIME, millis)
            }

            edit.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    private const val EXPORT_FORMAT_VERSION = 1
}
