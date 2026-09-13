package dev.openradio.android.playback

import android.net.Uri
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata
import com.google.android.gms.cast.MediaQueueItem

private const val CAST_AUDIO_MIME_TYPE = "audio/mpeg"
private const val CAST_HLS_MIME_TYPE = "application/x-mpegURL"

/**
 * Converts local station items to Cast receiver-safe queue items.
 *
 * - HLS streams: The Cast receiver handles HLS natively. We send the original
 *   HTTPS URL with `application/x-mpegURL` so the receiver fetches the manifest
 *   and segments itself (the worker's segment-by-segment pipeline produces a raw
 *   TS stream the receiver cannot play).
 * - Non-HLS (direct HTTP) streams: Routed through the worker relay (`?relay=1`)
 *   which fetches upstream with a browser User-Agent and re-serves over HTTPS,
 *   avoiding mixed-content blocks on the receiver.
 */
class OpenRadioMediaItemConverter : MediaItemConverter {

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val localConfiguration =
            mediaItem.localConfiguration ?: return fallbackQueueItem(mediaItem)

        val isHls = localConfiguration.mimeType == AppPlayer.HLS_MIME_TYPE
        val originalUrl = localConfiguration.uri.toString()

        // HLS: let the Cast receiver handle playback natively.
        // Non-HLS: route through the worker relay for HTTPS + browser UA.
        val castUrl = if (isHls) originalUrl else castStreamUrl(originalUrl, false)
        val contentType = if (isHls) CAST_HLS_MIME_TYPE else CAST_AUDIO_MIME_TYPE

        val castMetadata = CastMediaMetadata().apply {
            putString(CastMediaMetadata.KEY_TITLE, mediaItem.mediaMetadata.title?.toString() ?: "")
            putString(CastMediaMetadata.KEY_ARTIST, mediaItem.mediaMetadata.artist?.toString() ?: "")
            // Station logo for the Cast receiver's "Now Playing" screen.
            val artUri = mediaItem.mediaMetadata.artworkUri?.toString()
            if (!artUri.isNullOrBlank()) {
                addImage(com.google.android.gms.common.images.WebImage(Uri.parse(artUri)))
            }
        }

        val mediaInfo =
            MediaInfo.Builder(castUrl)
                .setContentType(contentType)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setMetadata(castMetadata)
                .build()

        return MediaQueueItem.Builder(mediaInfo)
            .setAutoplay(true)
            .build()
    }

    private fun fallbackQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val url = mediaItem.localConfiguration?.uri?.toString() ?: mediaItem.mediaId
        val castMetadata = CastMediaMetadata().apply {
            putString(CastMediaMetadata.KEY_TITLE, mediaItem.mediaMetadata.title?.toString() ?: "")
            putString(CastMediaMetadata.KEY_ARTIST, mediaItem.mediaMetadata.artist?.toString() ?: "")
            val artUri = mediaItem.mediaMetadata.artworkUri?.toString()
            if (!artUri.isNullOrBlank()) {
                addImage(com.google.android.gms.common.images.WebImage(Uri.parse(artUri)))
            }
        }
        val mediaInfo =
            MediaInfo.Builder(url)
                .setContentType(CAST_AUDIO_MIME_TYPE)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setMetadata(castMetadata)
                .build()
        return MediaQueueItem.Builder(mediaInfo)
            .setAutoplay(true)
            .build()
    }

    override fun toMediaItem(queueItem: MediaQueueItem): MediaItem {
        val mediaInfo = queueItem.media ?: return emptyMediaItem(queueItem)
        val uri = mediaInfo.contentId ?: return emptyMediaItem(queueItem)
        val contentType = mediaInfo.contentType
        val isHls =
            contentType?.contains("mpegurl", ignoreCase = true) == true ||
                contentType?.contains("m3u8", ignoreCase = true) == true
        val mimeType = if (isHls) AppPlayer.HLS_MIME_TYPE else contentType ?: CAST_AUDIO_MIME_TYPE
        val title = mediaInfo.metadata?.getString(CastMediaMetadata.KEY_TITLE) ?: ""
        val artist = mediaInfo.metadata?.getString(CastMediaMetadata.KEY_ARTIST) ?: ""
        // Preserve artwork from Cast metadata so the app's now-playing card keeps the logo.
        val artUri = mediaInfo.metadata?.images?.firstOrNull()?.url?.toString()
        return MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artUri?.let { Uri.parse(it) })
                    .build(),
            )
            .build()
    }

    private fun emptyMediaItem(queueItem: MediaQueueItem): MediaItem =
        MediaItem.Builder()
            .setMediaId("cast-item-${queueItem.itemId}")
            .setUri("data:audio/mpeg;base64,")
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle("OpenRadio-IN")
                    .setArtist("Cast item unavailable")
                    .build(),
            )
            .build()
}
