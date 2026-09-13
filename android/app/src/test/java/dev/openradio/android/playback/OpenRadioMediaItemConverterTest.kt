package dev.openradio.android.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.android.gms.cast.MediaInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OpenRadioMediaItemConverterTest {
    @Test
    fun `HLS uses original URL with native HLS content type`() {
        val url = "https://example.com/live.m3u8"
        val item =
            MediaItem.Builder()
                .setUri(url)
                .setMimeType(AppPlayer.HLS_MIME_TYPE)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("AIR Test")
                        .setArtist("Telugu")
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        .build(),
                )
                .build()

        val queueItem = OpenRadioMediaItemConverter().toMediaQueueItem(item)

        val media = requireNotNull(queueItem.media)
        assertNotNull(media.contentId)
        assertEquals("HLS should use original URL", url, media.contentId)
        assertEquals("application/x-mpegURL", media.contentType)
        assertEquals(MediaInfo.STREAM_TYPE_LIVE, media.streamType)
        assertEquals(true, queueItem.autoplay)
        assertEquals("AIR Test", media.metadata?.getString("com.google.android.gms.cast.metadata.TITLE"))
        assertEquals("Telugu", media.metadata?.getString("com.google.android.gms.cast.metadata.ARTIST"))
    }

    @Test
    fun `converts direct audio to a live Cast audio item via relay`() {
        val item =
            MediaItem.Builder()
                .setMediaId("direct-audio")
                .setUri("https://example.com/live.mp3")
                .build()

        val queueItem = OpenRadioMediaItemConverter().toMediaQueueItem(item)
        val media = requireNotNull(queueItem.media)

        assertNotNull("contentId is null", media.contentId)
        assertTrue("Expected relay URL but got: ${media.contentId}", media.contentId.contains("relay=1"))
        assertTrue("Expected url param but got: ${media.contentId}", media.contentId.contains("url="))
        assertEquals("audio/mpeg", media.contentType)
        assertEquals(MediaInfo.STREAM_TYPE_LIVE, media.streamType)
        assertEquals(true, queueItem.autoplay)
    }

    @Test
    fun `roundtrip toMediaItem preserves URI and metadata`() {
        val item =
            MediaItem.Builder()
                .setUri("https://example.com/live.m3u8")
                .setMimeType(AppPlayer.HLS_MIME_TYPE)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("AIR Test")
                        .setArtist("Telugu")
                        .build(),
                )
                .build()

        val queueItem = OpenRadioMediaItemConverter().toMediaQueueItem(item)
        val restoredItem = OpenRadioMediaItemConverter().toMediaItem(queueItem)

        assertNotNull("restored URI should not be null", restoredItem.localConfiguration?.uri)
        assertEquals(
            "HLS roundtrip preserves original URL",
            "https://example.com/live.m3u8",
            restoredItem.localConfiguration?.uri.toString(),
        )
        assertEquals("AIR Test", restoredItem.mediaMetadata.title)
        assertEquals("Telugu", restoredItem.mediaMetadata.artist)
    }
}
