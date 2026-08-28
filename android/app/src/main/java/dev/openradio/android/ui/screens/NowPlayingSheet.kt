package dev.openradio.android.ui.screens

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import android.widget.Toast
import coil.compose.AsyncImage
import dev.openradio.android.LocaleManager
import dev.openradio.android.R
import dev.openradio.android.alarm.AlarmReceiver
import dev.openradio.android.data.Station
import dev.openradio.android.ui.MarqueeText
import dev.openradio.android.ui.PlayerViewModel
import dev.openradio.android.ui.theme.Sky
import dev.openradio.android.ui.theme.Violet
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    val playback by viewModel.playback.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val epg by viewModel.epg.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val station = stations.firstOrNull { it.id == playback.currentStationId }
    val context = LocalContext.current
    val uiLang = remember { LocaleManager.currentLanguage() }

    LaunchedEffect(playback.currentStationId) {
        playback.currentStationId?.let { viewModel.loadEpg(it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val artwork = playback.nowPlayingArt ?: station?.logo
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Sky, Violet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!artwork.isNullOrBlank()) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = station?.localizedName(uiLang) ?: playback.currentStationName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            MarqueeText(
                text = station?.localizedName(uiLang) ?: playback.currentStationName ?: stringResource(R.string.no_station),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            val subtitle = when {
                playback.loading -> stringResource(R.string.buffering)
                playback.paused -> stringResource(R.string.paused)
                else -> playback.nowPlayingTrack?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.live_radio)
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            playback.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            playback.retryStatus?.let {
                if (playback.error.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = viewModel::skipPrevious) {
                    Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous))
                }
                FilledIconButton(
                    onClick = { if (playback.playing) viewModel.pause() else viewModel.resume() },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (playback.playing) R.string.pause else R.string.play),
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = viewModel::skipNext) {
                    Icon(Icons.Filled.SkipNext, stringResource(R.string.next))
                }
            }

            if (!playback.castActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::toggleMute) {
                        Icon(
                            imageVector = if (playback.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.mute)
                        )
                    }
                    Slider(
                        value = playback.volume,
                        onValueChange = viewModel::setVolume,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (playback.castAvailable) {
                    IconButton(onClick = viewModel::toggleCast) {
                        Icon(
                            Icons.Filled.Cast,
                            stringResource(R.string.cast),
                            tint = if (playback.castActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SleepTimerAction(viewModel)
                AlarmAction(viewModel)
                IconButton(onClick = { shareStation(context, station) }) {
                    Icon(Icons.Filled.Share, stringResource(R.string.share))
                }
                station?.let {
                    IconButton(onClick = { viewModel.toggleFavorite(it.id) }) {
                        Icon(
                            imageVector = if (it.id in favorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite),
                            tint = if (it.id in favorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            epg?.let { schedule ->
                if (schedule.programs.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.epg_schedule),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(schedule.programs) { program ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(
                                    "${program.start} – ${program.end}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(program.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerAction(viewModel: PlayerViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    IconButton(onClick = { showDialog = true }) {
        Icon(Icons.Filled.Timer, stringResource(R.string.sleep_timer))
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.sleep_timer)) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        TextButton(
                            onClick = {
                                viewModel.startSleepTimer(minutes)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.sleep_minutes, minutes))
                        }
                    }
                    if (viewModel.isSleepTimerActive()) {
                        TextButton(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.cancel_sleep_timer),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun AlarmAction(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    IconButton(
        onClick = {
            val stationId = viewModel.playback.value.currentStationId ?: return@IconButton
            val now = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    AlarmReceiver.schedule(context, hour, minute, stationId)
                    Toast.makeText(context, context.getString(R.string.alarm_set), Toast.LENGTH_SHORT).show()
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(context)
            ).show()
        }
    ) {
        Icon(Icons.Filled.Alarm, stringResource(R.string.alarm))
    }
}

private fun shareStation(context: Context, station: Station?) {
    if (station == null) return
    val name = station.name
    val text = "Listen to $name on OpenRadio-IN: ${station.homepage.ifBlank { "https://kedharnadh.github.io/OpenRadio-IN/" }}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
