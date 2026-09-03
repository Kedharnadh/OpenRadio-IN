package dev.openradio.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Renders single-line [text] and, when it is wider than its container, auto-scrolls it
 * back and forth like a classic marquee. Uses an unbounded [TextMeasurer] so the text is
 * measured at its intrinsic width (not clipped to the container) before deciding to scroll.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
) {
    val textMeasurer = rememberTextMeasurer()
    val textWidthPx =
        remember(text, style) {
            textMeasurer.measure(
                text = text,
                style = style,
                maxLines = maxLines,
                overflow = TextOverflow.Clip,
            ).size.width
        }

    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val viewWidthPx = with(density) { maxWidth.roundToPx() }
        val scrollDistance = (textWidthPx - viewWidthPx).coerceAtLeast(0)
        val shouldScroll = scrollDistance > 0

        // When there is nothing to scroll, keep the text centered like a plain Text.
        if (!shouldScroll) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = style,
                    color = color,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            return@BoxWithConstraints
        }

        val transition = rememberInfiniteTransition(label = "marquee")
        val offsetPx by transition.animateFloat(
            initialValue = 0f,
            targetValue = -scrollDistance.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = (scrollDistance * 5 + 2000).coerceIn(2000, 30000),
                            easing = LinearEasing,
                        ),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "marqueeOffset",
        )

        Box(
            modifier =
                Modifier
                    .requiredWidth(with(density) { textWidthPx.toDp() })
                    .offset { IntOffset(offsetPx.roundToInt(), 0) },
        ) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
