package dev.openradio.android.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.openradio.android.data.StationsRepository
import dev.openradio.android.data.StationsStore
import dev.openradio.android.playback.AppPlayer
import dev.openradio.android.playback.PlaybackService
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/**
 * Plays the selected station at the scheduled time. Starts [PlaybackService] as
 * a foreground service so media3 can show the media notification, then starts
 * playback through the shared [AppPlayer].
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: return
        // The process may have been killed; load the station list from the cached copy.
        val stations =
            runBlocking {
                StationsRepository(context.applicationContext).cached()
            }
        if (stations.isEmpty()) return
        StationsStore.setStations(stations)
        val station = stations.firstOrNull { it.id == stationId } ?: return
        AppPlayer.initialize(context.applicationContext)
        runCatching {
            context.startForegroundService(Intent(context, PlaybackService::class.java))
        }
        AppPlayer.playStation(station, stations)
    }

    companion object {
        private const val EXTRA_STATION_ID = "station_id"
        private const val REQUEST_CODE = 1001
        const val ACTION_PLAY_STATION = "dev.openradio.android.alarm.PLAY"

        fun schedule(
            context: Context,
            hour: Int,
            minute: Int,
            stationId: String,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent =
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_PLAY_STATION
                    putExtra(EXTRA_STATION_ID, stationId)
                }
            val pending =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val calendar =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent =
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_PLAY_STATION
                }
            val pending =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    alarmIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }
}
