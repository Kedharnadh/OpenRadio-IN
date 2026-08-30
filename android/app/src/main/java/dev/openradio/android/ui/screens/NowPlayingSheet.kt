package dev.openradio.android.ui.screens

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.media3.cast.MediaRouteButton
import dev.openradio.android.LocaleManager
import dev.openradio.android.Prefs
import dev.openradio.android.R
import dev.openradio.android.alarm.AlarmReceiver
import dev.openradio.android.data.EpgProgram
import dev.openradio.android.data.Station
import dev.openradio.android.ui.MarqueeText
import dev.openradio.android.ui.PlayerViewModel
import dev.openradio.android.ui.theme.Sky
import dev.openradio.android.ui.theme.Violet
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val sleepEndAt by viewModel.sleepEndAt.collectAsState()
    var alarmMillis by remember { mutableStateOf(Prefs.alarmTimeMillis()) }

    LaunchedEffect(playback.currentStationId) {
        playback.currentStationId?.let { viewModel.loadEpg(it) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Sky.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            val wide = maxWidth >= 760.dp || maxWidth > maxHeight
            val artSize = (if (wide) (maxHeight * 0.58f).coerceIn(240.dp, 480.dp)
            else (maxWidth - 48.dp).coerceIn(160.dp, 320.dp))
            val artwork = playback.nowPlayingArt ?: station?.logo

            @Composable
            fun Artwork() {
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(36.dp),
                            ambientColor = Violet.copy(alpha = 0.4f),
                            spotColor = Sky.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(36.dp))
                        .background(Brush.linearGradient(listOf(Sky, Violet))),
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
                            modifier = Modifier.size(96.dp),
                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            @Composable
            fun Details() {
                MarqueeText(
                    text = station?.localizedName(uiLang) ?: playback.currentStationName ?: stringResource(R.string.no_station),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(6.dp))
                val subtitle = playback.retryStatus ?: when {
                    playback.loading -> stringResource(R.string.buffering)
                    playback.paused -> stringResource(R.string.paused)
                    else -> playback.nowPlayingTrack?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.live_radio)
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (playback.retryStatus != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Transport controls
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    SheetIconButton(onClick = viewModel::skipPrevious) {
                        Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous), modifier = Modifier.size(32.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = Violet.copy(alpha = 0.5f),
                                spotColor = Sky.copy(alpha = 0.5f)
                            )
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Sky, Violet))),
                        contentAlignment = Alignment.Center
                    ) {
                        SheetIconButton(
                            onClick = { if (playback.playing) viewModel.pause() else viewModel.resume() },
                            outerSize = 76.dp,
                            showRing = false
                        ) {
                            Icon(
                                imageVector = if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (playback.playing) R.string.pause else R.string.play),
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    SheetIconButton(onClick = viewModel::skipNext) {
                        Icon(Icons.Filled.SkipNext, stringResource(R.string.next), modifier = Modifier.size(32.dp))
                    }
                }

                if (!playback.castActive) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SheetIconButton(onClick = viewModel::toggleMute) {
                            Icon(
                                imageVector = if (playback.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.mute)
                            )
                        }
                        Slider(
                            value = playback.volume,
                            onValueChange = viewModel::setVolume,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Violet,
                                activeTrackColor = Violet,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (playback.castAvailable) {
                        MediaRouteButton()
                    }
                    SleepTimerAction(viewModel)
                    AlarmAction(viewModel, onAlarmSet = { alarmMillis = it })
                    SheetIconButton(onClick = { shareStation(context, station) }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share))
                    }
                    station?.let {
                        SheetIconButton(onClick = { viewModel.toggleFavorite(it.id) }) {
                            Icon(
                                imageVector = if (it.id in favorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorite),
                                tint = if (it.id in favorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (sleepEndAt != null || alarmMillis != null) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (sleepEndAt != null) {
                            val remaining = rememberRemainingText(sleepEndAt)
                            StatusRow(
                                Icons.Filled.Timer,
                                stringResource(R.string.sleep_timer_remaining, remaining)
                            )
                        }
                        alarmMillis?.let { alarmTime ->
                            StatusRow(
                                Icons.Filled.Alarm,
                                stringResource(R.string.alarm_scheduled, formatTimeMillis(alarmTime, context))
                            )
                        }
                    }
                }

                epg?.let { schedule ->
                    if (schedule.programs.isNotEmpty()) {
                        val now = rememberEpgTime()
                        val currentIndex = schedule.programs.indexOfFirst { program ->
                            val s = parseEpgTimeToMinutes(program.start)
                            val e = parseEpgTimeToMinutes(program.end)
                            s != null && e != null && now >= s && now < e
                        }
                        val current = if (currentIndex >= 0) schedule.programs[currentIndex] else null
                        val nextIndex = if (currentIndex >= 0) {
                            currentIndex + 1
                        } else {
                            schedule.programs.indexOfFirst { (parseEpgTimeToMinutes(it.start) ?: Int.MAX_VALUE) > now }
                        }
                        val next = if (nextIndex >= 0 && nextIndex < schedule.programs.size) schedule.programs[nextIndex] else null
                        val restStart = if (nextIndex >= 0) nextIndex + 1 else schedule.programs.size
                        val rest = schedule.programs.drop(restStart)

                        Spacer(Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.epg_schedule),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            if (current != null) {
                                item {
                                    EpgNowRow(program = current)
                                }
                            }
                            if (next != null) {
                                item {
                                    EpgNextRow(program = next)
                                }
                            }
                            items(rest) { program ->
                                EpgRow(program = program)
                            }
                        }
                    }
                }
            }

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Artwork()
                    }
                    Column(
                        modifier = Modifier.weight(1.25f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Details()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 40.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(52.dp))

                    Artwork()
                    Spacer(Modifier.height(28.dp))
                    Details()
                }
            }
        }
    }
}

@Composable
private fun SleepTimerAction(viewModel: PlayerViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    val sleepEndAt by viewModel.sleepEndAt.collectAsState()
    val active = sleepEndAt != null
    SheetIconButton(onClick = { showDialog = true }) {
        Icon(
            Icons.Filled.Timer,
            stringResource(R.string.sleep_timer),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.sleep_timer)) },
            text = {
                Column {
                    if (active) {
                        val remaining = rememberRemainingText(sleepEndAt)
                        Text(
                            stringResource(R.string.sleep_timer_remaining, remaining),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                    }
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
                    if (active) {
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
private fun AlarmAction(viewModel: PlayerViewModel, onAlarmSet: (Long?) -> Unit) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val scheduled = Prefs.alarmTimeMillis()
    SheetIconButton(onClick = { showDialog = true }) {
        Icon(
            Icons.Filled.Alarm,
            stringResource(R.string.alarm),
            tint = if (scheduled != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.alarm)) },
            text = {
                Column {
                    if (scheduled != null) {
                        Text(
                            stringResource(R.string.alarm_scheduled, formatTimeMillis(scheduled, context)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                AlarmReceiver.cancel(context)
                                Prefs.setAlarmTimeMillis(null)
                                onAlarmSet(null)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.cancel_alarm), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text(stringResource(R.string.no_alarm))
                    }
                    TextButton(
                        onClick = {
                            val stationId = viewModel.playback.value.currentStationId ?: return@TextButton
                            showDialog = false
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                    AlarmReceiver.schedule(context, hour, minute, stationId)
                                    Prefs.setAlarmTimeMillis(cal.timeInMillis)
                                    onAlarmSet(cal.timeInMillis)
                                    Toast.makeText(context, context.getString(R.string.alarm_set), Toast.LENGTH_SHORT).show()
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                DateFormat.is24HourFormat(context)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.set_alarm))
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun rememberRemainingText(endAt: Long?): String {
    if (endAt == null) return ""
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endAt) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = ((endAt - now) / 1000).coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${secs}s"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

private fun formatTimeMillis(millis: Long, context: Context): String =
    DateFormat.getTimeFormat(context).format(Date(millis))

@Composable
private fun StatusRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/* ---------- EPG (Prasar Bharati schedule) helpers ---------- */

// IST (UTC+5:30) offset in milliseconds, used to align with AIR cuesheet times.
private const val IST_OFFSET_MS = 19_800_000L

/** Current minutes since midnight in IST (UTC+5:30), matching the AIR cuesheet times. */
private fun istMinutesNow(): Int {
    val ist = System.currentTimeMillis() + IST_OFFSET_MS
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = ist }
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/** Parse an "h:mm AM/PM" cuesheet time into minutes since midnight, or null. */
private fun parseEpgTimeToMinutes(timeStr: String): Int? {
    val match = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""", RegexOption.IGNORE_CASE)
        .find(timeStr.trim()) ?: return null
    var hours = match.groupValues[1].toInt()
    val minutes = match.groupValues[2].toInt()
    when (match.groupValues[3].uppercase()) {
        "AM" -> if (hours == 12) hours = 0
        "PM" -> if (hours != 12) hours += 12
    }
    return hours * 60 + minutes
}

@Composable
private fun rememberEpgTime(): Int {
    var now by remember { mutableStateOf(istMinutesNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            // Wait until the next minute boundary (IST) before re-evaluating, then
            // keep ticking so the highlighted "Now" program stays in sync.
            val elapsedInMinute = (System.currentTimeMillis() + IST_OFFSET_MS) % 60_000L
            delay((60_000L - elapsedInMinute + 500L).coerceAtLeast(1000L))
            now = istMinutesNow()
        }
    }
    return now
}

@Composable
private fun EpgNowRow(program: EpgProgram) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Sky.copy(alpha = 0.22f), Violet.copy(alpha = 0.16f))))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Violet)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.epg_now),
            style = MaterialTheme.typography.labelSmall,
            color = Violet,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                program.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${program.start} – ${program.end}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpgNextRow(program: EpgProgram) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.epg_next),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                program.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                program.start,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpgRow(program: EpgProgram) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            program.start,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        Text(
            program.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SheetIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outerSize: Dp = 52.dp,
    showRing: Boolean = true,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.12f else 1f,
        animationSpec = tween(160),
        label = "sheetBtnScale"
    )
    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .focusable()
                .onFocusChanged { focused = it.isFocused }
                .border(
                    width = if (focused && showRing) 2.dp else 0.dp,
                    color = Violet,
                    shape = CircleShape
                )
        ) {
            content()
        }
    }
}
