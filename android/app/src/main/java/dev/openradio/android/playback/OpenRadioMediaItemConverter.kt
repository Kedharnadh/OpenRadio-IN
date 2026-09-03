package dev.openradio.android.playback

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem

/**
 * Converts local station items to Cast receiver-safe queue items.
 *
 * ExoPlayer needs [AppPlayer.HLS_MIME_TYPE] to recognize local HLS playback,
 * while the Default Media Receiver requires the standard IANA HLS content type.
 * This conversion happens only for the Cast queue item, so both players receive
 * the MIME type they understand. Non-HLS streams retain their original URL.
 */
class OpenRadioMediaItemConverter : MediaItemConverter {
    private val defaultConverter = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val localConfiguration = mediaItem.localConfiguration
        if (localConfiguration?.mimeType != AppPlayer.HLS_MIME_TYPE) {
            return defaultConverter.toMediaQueueItem(mediaItem)
        }

        // Route HLS through the worker (like the PWA) so the default Cast receiver
        // gets a flattened continuous stream instead of a raw .m3u8 it may mis-handle.
        // The worker resolves each HLS source to a concrete media content type (these
        // AIR/Doordarshan streams are MPEG-TS -> "video/MP2T"); use the async probe's
        // result only when it belongs to this exact source, defaulting to MPEG-TS.
        val proxyUrl = AppPlayer.HlsCastProxy.streamUrl(localConfiguration.uri.toString())
        val contentType =
            if (AppPlayer.HlsCastProxy.probedForUrl == localConfiguration.uri.toString()) {
                AppPlayer.HlsCastProxy.contentType ?: "video/mp2t"
            } else {
                "video/mp2t"
            }
        val receiverItem =
            mediaItem.buildUpon()
                .setUri(proxyUrl)
                .setMimeType(contentType)
                .build()
        return defaultConverter.toMediaQueueItem(receiverItem)
    }

    override fun toMediaItem(queueItem: MediaQueueItem): MediaItem =
        try {
            defaultConverter.toMediaItem(queueItem)
        } catch (_: NullPointerException) {
            // The Cast SDK can report a queue item before its MediaInfo is available.
            MediaItem.Builder()
                .setMediaId("cast-item-${queueItem.itemId}")
                .setUri("data:audio/mpeg;base64,")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("OpenRadio-IN")
                        .setArtist("Cast item unavailable")
                        .build(),
                )
                .build()
        }
}
