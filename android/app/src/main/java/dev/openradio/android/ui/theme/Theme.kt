package dev.openradio.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette mirrors the OpenRadio-IN PWA so the native app shares the
// same look and feel. Accent gradient is sky -> violet (#38bdf8 -> #7c3aed).
val Sky = Color(0xFF38BDF8)
val Violet = Color(0xFF7C3AED)
val Online = Color(0xFF22C55E)
val Offline = Color(0xFFEF4444)
val Unknown = Color(0xFF94A3B8)

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF7C3AED),
        onSecondary = Color.White,
        background = Color(0xFFF1F5F9),
        onBackground = Color(0xFF1E293B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF64748B),
        surfaceContainerHighest = Color(0xFFE2E8F0),
        surfaceContainer = Color(0xFFF8FAFC),
        surfaceContainerLow = Color(0xFFF1F5F9),
        tertiary = Color(0xFF7C3AED),
        onTertiary = Color.White,
        error = Color(0xFFEF4444),
        outline = Color(0xFF94A3B8),
    )

private val DarkColors =
    darkColorScheme(
        primary = Sky,
        onPrimary = Color(0xFF07111F),
        primaryContainer = Color(0xFF0C2A4A),
        onPrimaryContainer = Color(0xFFBAE6FD),
        secondary = Violet,
        onSecondary = Color.White,
        background = Color(0xFF07111F),
        onBackground = Color(0xFFE2E8F0),
        surface = Color(0xFF111C32),
        onSurface = Color(0xFFE2E8F0),
        surfaceVariant = Color(0xFF1E293B),
        onSurfaceVariant = Color(0xFF94A3B8),
        surfaceContainerHighest = Color(0xFF1E293B),
        surfaceContainer = Color(0xFF111C32),
        surfaceContainerLow = Color(0xFF0D1526),
        surfaceContainerHigh = Color(0xFF16223C),
        tertiary = Color(0xFFA78BFA),
        onTertiary = Color(0xFF1E1B4B),
        error = Color(0xFFF87171),
        outline = Color(0xFF64748B),
    )

/**
 * OpenRadio-IN brand theme. Uses the PWA's sky/violet accent palette instead
 * of Material You so the app looks identical across Android versions and
 * devices to the web app.
 */
@Composable
fun OpenRadioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
