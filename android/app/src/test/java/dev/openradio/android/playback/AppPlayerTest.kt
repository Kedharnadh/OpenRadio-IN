package dev.openradio.android.playback

import dev.openradio.android.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPlayerTest {
    private fun createStation(
        id: String,
        name: String = "Station $id",
        logo: String = "",
        city: String = "Delhi",
        language: String = "Hindi",
        streams: List<Station.Stream> = listOf(Station.Stream("https://example.com/$id.mp3", "MP3", 1)),
    ): Station {
        return Station(
            id = id, name = name, nameTe = "", nameHi = "",
            language = language, country = "India", state = "Delhi", city = city,
            categories = listOf("AIR"), genre = emptyList(), homepage = "",
            logo = logo, streams = streams, verified = true, status = "online",
            epgId = -1L, metadataUrl = "", songFirst = false,
        )
    }

    @Test
    fun `buildQueue returns empty list for empty input`() {
        val items = AppPlayer.buildQueue(emptyList())
        assertTrue(items.isEmpty())
    }

    @Test
    fun `buildQueue filters out stations without streams`() {
        val stations =
            listOf(
                createStation("1"),
                Station(
                    id = "no_streams", name = "No Streams", nameTe = "", nameHi = "",
                    language = "Hindi", country = "", state = "", city = "",
                    categories = emptyList(), genre = emptyList(), homepage = "",
                    logo = "", streams = emptyList(), verified = false, status = "unknown",
                    epgId = -1L, metadataUrl = "", songFirst = false,
                ),
                createStation("3"),
            )
        val items = AppPlayer.buildQueue(stations)
        assertEquals(2, items.size)
        assertEquals("1", items[0].mediaId)
        assertEquals("3", items[1].mediaId)
    }

    @Test
    fun `buildQueue creates correct media items for stations`() {
        val stations =
            listOf(
                createStation("test_1", name = "AIR Delhi", city = "Delhi", language = "Hindi"),
            )
        val items = AppPlayer.buildQueue(stations)
        assertEquals(1, items.size)

        val item = items[0]
        assertEquals("test_1", item.mediaId)
        assertNotNull(item.localConfiguration)
        assertEquals("https://example.com/test_1.mp3", item.localConfiguration?.uri.toString())

        val metadata = item.mediaMetadata
        assertEquals("AIR Delhi", metadata.title?.toString())
        assertEquals("Delhi • Hindi", metadata.artist?.toString())
    }

    @Test
    fun `buildQueue sets HLS mime type for HLS streams`() {
        val stations =
            listOf(
                createStation(
                    "hls_1",
                    streams = listOf(Station.Stream("https://example.com/live.m3u8", "HLS", 1)),
                ),
            )
        val items = AppPlayer.buildQueue(stations)
        assertEquals(1, items.size)
        assertEquals(AppPlayer.HLS_MIME_TYPE, items[0].localConfiguration?.mimeType)
    }

    @Test
    fun `buildQueue does not set mime type for non-HLS streams`() {
        val stations =
            listOf(
                createStation(
                    "mp3_1",
                    streams = listOf(Station.Stream("https://example.com/stream.mp3", "MP3", 1)),
                ),
            )
        val items = AppPlayer.buildQueue(stations)
        assertEquals(1, items.size)
        assertNull(items[0].localConfiguration?.mimeType)
    }

    @Test
    fun `buildQueue sets artwork URI from station logo`() {
        val stations =
            listOf(
                createStation("test_1", logo = "https://example.com/logo.png"),
            )
        val items = AppPlayer.buildQueue(stations)
        assertEquals(
            "https://example.com/logo.png",
            items[0].mediaMetadata.artworkUri?.toString(),
        )
    }

    @Test
    fun `buildQueue sets radio station media type`() {
        val stations = listOf(createStation("test_1"))
        val items = AppPlayer.buildQueue(stations)
        assertEquals(
            androidx.media3.common.MediaMetadata.MEDIA_TYPE_RADIO_STATION,
            items[0].mediaMetadata.mediaType,
        )
    }

    @Test
    fun `buildQueue uses station name in subtitle when city and language are blank`() {
        val stations =
            listOf(
                createStation("test_1", name = "AIR Test", city = "", language = ""),
            )
        val items = AppPlayer.buildQueue(stations)
        assertEquals("AIR Test", items[0].mediaMetadata.artist?.toString())
    }

    @Test
    fun `HLS_MIME_TYPE is correct media3 value`() {
        assertEquals("application/x-mpegURL", AppPlayer.HLS_MIME_TYPE)
    }

    @Test
    fun `ROOT_MEDIA_ID is set`() {
        assertEquals("openradio_root", AppPlayer.ROOT_MEDIA_ID)
    }
}
