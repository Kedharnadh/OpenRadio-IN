package dev.openradio.android.playback

import android.net.Uri
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import dev.openradio.android.BuildConfig
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem

/** Adapts station items for the Default Media Receiver and tolerates partial queue updates. */
class OpenRadioMediaItemConverter : MediaItemConverter {

    private val defaultConverter = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val localConfiguration = mediaItem.localConfiguration
        if (localConfiguration?.mimeType != AppPlayer.HLS_MIME_TYPE) {
            return defaultConverter.toMediaQueueItem(mediaItem)
        }

        val proxiedUrl = Uri.parse(BuildConfig.HLS_PROXY_URL).buildUpon()
            .appendQueryParameter("url", localConfiguration.uri.toString())
            .build()
        val receiverItem = mediaItem.buildUpon()
            .setUri(proxiedUrl)
            .setMimeType("audio/mpeg")
            .build()
        return defaultConverter.toMediaQueueItem(receiverItem)
    }

    override fun toMediaItem(queueItem: MediaQueueItem): MediaItem {
        return try {
            defaultConverter.toMediaItem(queueItem)
        } catch (_: NullPointerException) {
            // Cast can publish a queue item before its MediaInfo is available.
            MediaItem.Builder()
                .setMediaId("cast-item-${queueItem.itemId}")
                .setUri("data:audio/mpeg;base64,")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("OpenRadio-IN")
                        .setArtist("Cast item unavailable")
                        .build()
                )
                .build()
        }
    }
}