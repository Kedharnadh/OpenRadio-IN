package dev.openradio.android.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.cast.MediaRouteButton
import coil.compose.AsyncImage
import dev.openradio.android.LocaleManager
import dev.openradio.android.R
import dev.openradio.android.data.Station
import dev.openradio.android.playback.PlaybackUiState
import dev.openradio.android.ui.FilterState
import dev.openradio.android.ui.MarqueeText
import dev.openradio.android.ui.PlayerViewModel
import dev.openradio.android.ui.theme.Sky
import dev.openradio.android.ui.theme.Violet
import dev.openradio.android.ui.theme.stationStatusColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onLanguageChanged: (String) -> Unit = {},
) {
    val stations by viewModel.filteredStations.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val languages by viewModel.allLanguages.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val stationsLoading by viewModel.stationsLoading.collectAsState()
    val recentsStations = recents.mapNotNull { id -> viewModel.stations.value.firstOrNull { it.id == id } }
    val favoriteStations = stations.filter { it.id in favorites }
    val showFavorites = !filter.onlyFavorites
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var recentsExpanded by rememberSaveable { mutableStateOf(true) }
    var favoritesExpanded by rememberSaveable { mutableStateOf(true) }
    var allExpanded by rememberSaveable { mutableStateOf(true) }
    val searchFocus = remember { FocusRequester() }
    val config = LocalContext.current.resources.configuration
    val isDpadDevice =
        remember(config) {
            config.navigation == android.content.res.Configuration.NAVIGATION_DPAD ||
                config.navigation == android.content.res.Configuration.NAVIGATION_TRACKBALL
        }
    LaunchedEffect(isDpadDevice) {
        if (isDpadDevice) searchFocus.requestFocus()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    UiLanguageMenu(onLanguageChanged = onLanguageChanged)
                    if (playback.castAvailable) {
                        MediaRouteButton()
                    }
                },
            )
        },
        bottomBar = {
            NowPlayingBar(
                playback = playback,
                viewModel = viewModel,
                onClick = { showNowPlaying = true },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 840.dp
            val horizontalPadding = if (wideLayout) 32.dp else 16.dp
            val gridColumns =
                if (maxWidth >= 1200.dp) {
                    4
                } else if (maxWidth >= 880.dp) {
                    3
                } else if (maxWidth >= 600.dp) {
                    2
                } else {
                    1
                }

            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
            ) {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = viewModel::setQuery,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 8.dp)
                            .focusRequester(searchFocus),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filter.query.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.setQuery("") },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
                FilterControls(
                    filter = filter,
                    languages = languages,
                    categories = categories,
                    favorites = favorites,
                    onLanguage = viewModel::setLanguage,
                    onCategory = viewModel::setCategory,
                    onFavorites = viewModel::setOnlyFavorites,
                    horizontalPadding = horizontalPadding,
                    wideLayout = wideLayout,
                )

                val currentStationHidden =
                    playback.currentStationId != null &&
                        stations.none { it.id == playback.currentStationId }
                if (currentStationHidden) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding, vertical = 4.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showNowPlaying = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Sky, Violet))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.now_playing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    playback.currentStationName
                                        ?: stringResource(R.string.select_station),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusTarget(),
                    contentPadding =
                        PaddingValues(
                            start = if (wideLayout) 16.dp else 0.dp,
                            end = if (wideLayout) 16.dp else 0.dp,
                            bottom = 16.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (filter.query.isBlank() && !filter.onlyFavorites && recentsStations.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(
                                title = stringResource(R.string.recently_played),
                                expanded = recentsExpanded,
                                onToggle = { recentsExpanded = !recentsExpanded },
                            )
                        }
                        if (recentsExpanded) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(recentsStations, key = { it.id }) { station ->
                                        FeaturedCard(
                                            station = station,
                                            isFavorite = station.id in favorites,
                                            isPlaying = playback.currentStationId == station.id && playback.playing,
                                            onPlay = { viewModel.togglePlay(station) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showFavorites && favoriteStations.isNotEmpty() &&
                        filter.query.isBlank() && filter.category == null && filter.language == null
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(
                                title = stringResource(R.string.favorite_stations),
                                expanded = favoritesExpanded,
                                onToggle = { favoritesExpanded = !favoritesExpanded },
                            )
                        }
                        if (favoritesExpanded) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(favoriteStations, key = { it.id }) { station ->
                                        FeaturedCard(
                                            station = station,
                                            isFavorite = true,
                                            isPlaying = playback.currentStationId == station.id && playback.playing,
                                            onPlay = { viewModel.togglePlay(station) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filter.query.isBlank() && !filter.onlyFavorites) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(
                                title = stringResource(R.string.all_stations),
                                expanded = allExpanded,
                                onToggle = { allExpanded = !allExpanded },
                            )
                        }
                    }

                    if (showFavorites && favorites.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(R.string.no_favorites_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
                            )
                        }
                    }

                    if (allExpanded) {
                        if (stationsLoading && stations.isEmpty()) {
                            items(6) {
                                StationCardSkeleton(wideLayout = wideLayout)
                            }
                        } else if (stations.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        Icons.Filled.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Text(
                                        stringResource(R.string.empty_list),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        items(stations, key = { it.id }) { station ->
                            StationCard(
                                station = station,
                                isFavorite = station.id in favorites,
                                isCurrent =
                                    playback.currentStationId == station.id &&
                                        (playback.playing || playback.paused || playback.loading),
                                isPlaying = playback.currentStationId == station.id && playback.playing,
                                wideLayout = wideLayout,
                                onPlay = { viewModel.togglePlay(station) },
                                onStop = { viewModel.stop() },
                                onFavorite = { viewModel.toggleFavorite(station.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNowPlaying) {
        NowPlayingSheet(
            viewModel = viewModel,
            onDismiss = { showNowPlaying = false },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        animationSpec = tween(180),
        label = "headerScale",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .then(
                    if (onToggle != null) {
                        Modifier
                            .focusable()
                            .onFocusChanged { focused = it.isFocused }
                            .clickable(onClick = onToggle)
                            .border(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) Violet else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                    } else {
                        Modifier
                    },
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (onToggle != null) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription =
                    if (expanded) {
                        stringResource(R.string.collapse)
                    } else {
                        stringResource(R.string.expand)
                    },
                tint = if (focused) Violet else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UiLanguageMenu(onLanguageChanged: (String) -> Unit) {
    val currentLang = remember { LocaleManager.currentLanguage() }
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.Language,
                contentDescription = stringResource(R.string.ui_language),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "en" to stringResource(R.string.lang_en),
                "te" to stringResource(R.string.lang_te),
                "hi" to stringResource(R.string.lang_hi),
            )
                .forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            if (code != currentLang) onLanguageChanged(code)
                        },
                    )
                }
        }
    }
}

@Composable
private fun FilterControls(
    filter: FilterState,
    languages: List<String>,
    categories: List<String>,
    favorites: Set<String>,
    onLanguage: (String?) -> Unit,
    onCategory: (String?) -> Unit,
    onFavorites: (Boolean) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    wideLayout: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 2.dp)
                .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageDropdown(
            filter = filter,
            languages = languages,
            onLanguage = onLanguage,
            modifier = Modifier.weight(1f),
        )
        // Favorites pill is kept at the same height as the language selector so the
        // two controls line up on the same baseline across all screen sizes.
        FavoritePill(
            filter = filter,
            onFavorites = onFavorites,
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = filter.category == null,
                onClick = { onCategory(null) },
                label = { Text(stringResource(R.string.all_categories)) },
            )
        }
        items(categories, key = { it }) { category ->
            FilterChip(
                selected = filter.category == category,
                onClick = { onCategory(if (filter.category == category) null else category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
private fun FavoritePill(
    filter: FilterState,
    onFavorites: (Boolean) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(56.dp)
                .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        FilterChip(
            selected = filter.onlyFavorites,
            onClick = { onFavorites(!filter.onlyFavorites) },
            label = { Text(stringResource(R.string.favorites)) },
            leadingIcon =
                if (filter.onlyFavorites) {
                    { Icon(Icons.Filled.Favorite, contentDescription = null) }
                } else {
                    { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) }
                },
        )
    }
}

@Composable
private fun LanguageDropdown(
    filter: FilterState,
    languages: List<String>,
    onLanguage: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterChip(
            selected = filter.language != null,
            onClick = { expanded = true },
            label = {
                Text(
                    filter.language ?: stringResource(R.string.all_languages),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusable(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_languages)) },
                onClick = {
                    onLanguage(null)
                    expanded = false
                },
            )
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onLanguage(language)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StationAvatar(
    station: Station,
    displayName: String,
    size: Int = 48,
) {
    val shape = CircleShape
    val initials =
        displayName
            .split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first() }
            .joinToString("")
            .uppercase()
            .ifBlank { displayName.firstOrNull()?.toString() ?: "?" }
    Box(
        modifier =
            Modifier
                .size(size.dp)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Sky, Violet),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (station.logo.isNotBlank()) {
            AsyncImage(
                model = station.logo,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                initials,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusDot(status: String) {
    val color = stationStatusColor(status)
    Box(
        modifier =
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
    )
}

@Composable
private fun StationCardSkeleton(wideLayout: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )
    val shape = RoundedCornerShape(16.dp)
    val bone = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.4f)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (wideLayout) Modifier else Modifier.padding(horizontal = 16.dp)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bone),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bone),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bone),
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun StationCard(
    station: Station,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    wideLayout: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onFavorite: () -> Unit,
) {
    val uiLang = remember { LocaleManager.currentLanguage() }
    val displayName = station.localizedName(uiLang)
    val shape = RoundedCornerShape(18.dp)
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border =
            androidx.compose.foundation.BorderStroke(
                2.dp,
                when {
                    focused -> Violet
                    isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                },
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (wideLayout) Modifier else Modifier.padding(horizontal = 16.dp))
                .focusable()
                .onFocusChanged { focused = it.isFocused }
                .clip(shape)
                .clickable(onClick = onPlay),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StationAvatar(station, displayName)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        StatusDot(station.status)
                    }
                    val tags = listOf(station.language) + station.categories
                    Text(
                        tags.filter { it.isNotBlank() }.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint =
                            if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }

            if (station.categories.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    station.categories.take(3).forEach { category ->
                        AssistChip(
                            onClick = { },
                            label = { Text(category, fontSize = 11.sp) },
                            colors =
                                androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = Violet.copy(alpha = 0.16f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            border = null,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val footerText =
                    if (station.verified) {
                        stringResource(R.string.verified)
                    } else {
                        stringResource(R.string.community)
                    }
                Column {
                    Text(
                        footerText,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (station.verified) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    if (station.city.isNotBlank() || station.country.isNotBlank()) {
                        Text(
                            listOf(station.city, station.country).filter { it.isNotBlank() }.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onPlay),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(
                                    when {
                                        isPlaying -> R.string.pause
                                        else -> R.string.play
                                    },
                                ),
                                color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.error,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(onClick = onStop),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    stringResource(R.string.stop),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    station: Station,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
) {
    val uiLang = remember { LocaleManager.currentLanguage() }
    val displayName = station.localizedName(uiLang)
    val shape = RoundedCornerShape(16.dp)
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border =
            androidx.compose.foundation.BorderStroke(
                2.dp,
                if (focused) Violet else MaterialTheme.colorScheme.outline.copy(alpha = 0f),
            ),
        modifier =
            Modifier
                .width(180.dp)
                .focusable()
                .onFocusChanged { focused = it.isFocused }
                .clip(shape)
                .clickable(onClick = onPlay),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StationAvatar(station, displayName, 40)
                Spacer(Modifier.width(8.dp))
                StatusDot(station.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle =
                if (station.verified) {
                    stringResource(R.string.verified)
                } else {
                    stringResource(R.string.community)
                }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NowPlayingBar(
    playback: PlaybackUiState,
    viewModel: PlayerViewModel,
    onClick: () -> Unit,
) {
    val hasStation = playback.currentStationId != null
    val currentStation =
        playback.currentStationId?.let { id ->
            viewModel.stations.value.firstOrNull { it.id == id }
        }
    Surface(
        tonalElevation = 10.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClick)
                        .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationAvatarBox(
                    nowPlayingArt = playback.nowPlayingArt,
                    fallbackLogo = currentStation?.logo,
                    playing = playback.playing,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    MarqueeText(
                        text = playback.currentStationName ?: stringResource(R.string.select_station),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    val subtitle =
                        when {
                            playback.retryStatus != null -> playback.retryStatus
                            playback.loading -> stringResource(R.string.buffering)
                            playback.paused -> stringResource(R.string.paused)
                            !playback.nowPlayingTrack.isNullOrBlank() -> playback.nowPlayingTrack
                            else -> stringResource(R.string.live_radio)
                        }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (playback.retryStatus != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FocusableIconButton(onClick = viewModel::skipPrevious, enabled = hasStation) {
                    Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous))
                }
                FocusablePlayPause(
                    playing = playback.playing,
                    onClick = { if (playback.playing) viewModel.pause() else viewModel.resume() },
                )
                FocusableIconButton(onClick = viewModel::stop, enabled = hasStation) {
                    Icon(Icons.Filled.Stop, stringResource(R.string.stop))
                }
                FocusableIconButton(onClick = viewModel::skipNext, enabled = hasStation) {
                    Icon(Icons.Filled.SkipNext, stringResource(R.string.next))
                }
            }
        }
    }
}

@Composable
private fun FocusableIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.15f else 1f,
        animationSpec = tween(160),
        label = "iconBtnScale",
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .onFocusChanged { focused = it.isFocused },
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .border(
                        width = if (focused) 2.dp else 0.dp,
                        color = Violet,
                        shape = CircleShape,
                    )
                    .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun FocusablePlayPause(
    playing: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.12f else 1f,
        animationSpec = tween(160),
        label = "npPlayScale",
    )
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .focusable()
                .onFocusChanged { focused = it.isFocused }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(
                    width = if (focused) 3.dp else 0.dp,
                    color = Color.White,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick)
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Sky, Violet))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
            tint = Color.White,
            modifier = Modifier.padding(13.dp),
        )
    }
}

@Composable
private fun StationAvatarBox(
    nowPlayingArt: String?,
    fallbackLogo: String?,
    playing: Boolean = false,
) {
    val artwork = nowPlayingArt?.takeIf { it.isNotBlank() } ?: fallbackLogo?.takeIf { it.isNotBlank() }
    Box(
        modifier =
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Sky, Violet)),
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Filled.Radio,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        if (playing) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
