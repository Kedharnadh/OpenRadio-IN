package dev.openradio.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Renders now-playing artwork with a graceful fallback chain:
 * track album art → station logo → radio placeholder icon.
 * The [primary] artwork usually comes from a live metadata endpoint and can be
 * broken/absent for many stations, so a failed load silently switches to the
 * station's channel logo instead of showing an empty box.
 */
@Composable
fun StationArtwork(
    primary: String?,
    fallback: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 28.dp,
    iconTint: Color = Color.White,
    contentDescription: String? = null,
) {
    val primaryUrl = primary?.takeIf { it.isNotBlank() }
    val fallbackUrl = fallback?.takeIf { it.isNotBlank() }
    var url by remember(primaryUrl, fallbackUrl) { mutableStateOf(primaryUrl ?: fallbackUrl) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    url = if (url == primaryUrl) fallbackUrl else null
                },
            )
        } else {
            Icon(
                Icons.Filled.Radio,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
