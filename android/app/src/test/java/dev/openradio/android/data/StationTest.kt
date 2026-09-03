package dev.openradio.android.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationTest {
    private fun buildStationJson(
        id: String = "test_station",
        name: String = "Test Station",
        nameTe: String = "",
        nameHi: String = "",
        language: String = "Hindi",
        categories: List<String> = listOf("AIR"),
        streams: List<JSONObject> = emptyList(),
        verified: Boolean = true,
        status: String = "online",
        epgId: Long = -1L,
        logo: String = "",
        metadataUrl: String = "",
        songFirst: Boolean = false,
    ): JSONObject {
        val streamsArray = JSONArray()
        streams.forEach { streamsArray.put(it) }

        val categoriesArray = JSONArray()
        categories.forEach { categoriesArray.put(it) }

        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("name_te", nameTe)
            put("name_hi", nameHi)
            put("language", language)
            put("categories", categoriesArray)
            put("verified", verified)
            put("status", status)
            put("epg_id", epgId)
            put("logo", logo)
            put("metadata_url", metadataUrl)
            put("song_first", songFirst)
            put("streams", streamsArray)
        }
    }

    private fun buildStreamJson(
        url: String = "https://example.com/stream.mp3",
        codec: String = "MP3",
        priority: Int = 1,
    ): JSONObject {
        return JSONObject().apply {
            put("url", url)
            put("codec", codec)
            put("priority", priority)
        }
    }

    @Test
    fun `primaryStream returns HLS stream with lowest priority`() {
        val station =
            Station(
                id = "test",
                name = "Test",
                nameTe = "",
                nameHi = "",
                language = "Hindi",
                country = "",
                state = "",
                city = "",
                categories = emptyList(),
                genre = emptyList(),
                homepage = "",
                logo = "",
                streams =
                    listOf(
                        Station.Stream("https://example.com/stream.mp3", "MP3", 2),
                        Station.Stream("https://example.com/live.m3u8", "HLS", 1),
                        Station.Stream("https://example.com/backup.mp3", "MP3", 1),
                    ),
                verified = true,
                status = "online",
                epgId = -1L,
                metadataUrl = "",
                songFirst = false,
            )
        val primary = station.primaryStream
        assertNotNull(primary)
        assertEquals("https://example.com/live.m3u8", primary!!.url)
        assertEquals("HLS", primary.codec)
        assertEquals(1, primary.priority)
    }

    @Test
    fun `primaryStream returns null when no streams`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertNull(station.primaryStream)
    }

    @Test
    fun `primaryStream prefers HLS over non-HLS even with higher priority`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "",
                streams =
                    listOf(
                        Station.Stream("https://example.com/stream.mp3", "MP3", 1),
                        Station.Stream("https://example.com/live.m3u8", "HLS", 3),
                    ),
                verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        val primary = station.primaryStream
        assertNotNull(primary)
        assertEquals("https://example.com/live.m3u8", primary!!.url)
    }

    @Test
    fun `languageTags splits comma-separated language string`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi, English", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals(listOf("Hindi", "English"), station.languageTags)
    }

    @Test
    fun `languageTags filters blank entries`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi, , English,  , Tamil", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals(listOf("Hindi", "English", "Tamil"), station.languageTags)
    }

    @Test
    fun `languageTags returns single language in a list`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Telugu", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals(listOf("Telugu"), station.languageTags)
    }

    @Test
    fun `localizedName returns Telugu name when uiLang is te`() {
        val station =
            Station(
                id = "test",
                name = "AIR Hyderabad",
                nameTe = "ఏఐఆర్ హైదరాబాద్",
                nameHi = "",
                language = "Telugu", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals("ఏఐఆర్ హైదరాబాద్", station.localizedName("te"))
    }

    @Test
    fun `localizedName falls back to default name when localizedName is blank`() {
        val station =
            Station(
                id = "test", name = "AIR Hyderabad", nameTe = "", nameHi = "",
                language = "Telugu", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals("AIR Hyderabad", station.localizedName("te"))
    }

    @Test
    fun `localizedName returns Hindi name when uiLang is hi`() {
        val station =
            Station(
                id = "test", name = "AIR Delhi", nameTe = "", nameHi = "एआईआर दिल्ली",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals("एआईआर दिल्ली", station.localizedName("hi"))
    }

    @Test
    fun `localizedName returns default name for unknown language`() {
        val station =
            Station(
                id = "test", name = "AIR Test", nameTe = "టెస్ట్", nameHi = "टेस्ट",
                language = "Telugu", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
            )
        assertEquals("AIR Test", station.localizedName("fr"))
    }

    @Test
    fun `Stream isHls returns true for HLS codec`() {
        val stream = Station.Stream("https://example.com/stream.mp3", "HLS", 1)
        assertTrue(stream.isHls)
    }

    @Test
    fun `Stream isHls returns true when URL contains m3u8`() {
        val stream = Station.Stream("https://example.com/live.m3u8", "MP3", 1)
        assertTrue(stream.isHls)
    }

    @Test
    fun `Stream isHls is case insensitive`() {
        val stream1 = Station.Stream("https://example.com/live.m3u8", "mp3", 1)
        val stream2 = Station.Stream("https://example.com/stream.mp3", "hls", 1)
        assertTrue(stream1.isHls)
        assertTrue(stream2.isHls)
    }

    @Test
    fun `Stream isHls returns false for plain HTTP streams`() {
        val stream = Station.Stream("https://example.com/stream.mp3", "MP3", 1)
        assertFalse(stream.isHls)
    }

    @Test
    fun `preferredStreams orders HLS before non-HLS regardless of priority`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
                streams =
                    listOf(
                        Station.Stream("https://example.com/a.mp3", "MP3", 1),
                        Station.Stream("https://example.com/b.m3u8", "HLS", 5),
                    ),
            )
        val ordered = station.preferredStreams()
        assertEquals("https://example.com/b.m3u8", ordered[0].url)
        assertEquals("https://example.com/a.mp3", ordered[1].url)
    }

    @Test
    fun `preferredStreams orders by priority ascending within same codec`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
                streams =
                    listOf(
                        Station.Stream("https://example.com/backup.mp3", "MP3", 9),
                        Station.Stream("https://example.com/primary.mp3", "MP3", 1),
                    ),
            )
        val ordered = station.preferredStreams()
        assertEquals("https://example.com/primary.mp3", ordered[0].url)
        assertEquals("https://example.com/backup.mp3", ordered[1].url)
    }

    @Test
    fun `primaryStream returns preferred first stream`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false,
                streams =
                    listOf(
                        Station.Stream("https://example.com/evening.m3u8", "HLS", 2),
                        Station.Stream("https://example.com/morning.m3u8", "HLS", 1),
                    ),
            )
        assertEquals("https://example.com/morning.m3u8", station.primaryStream?.url)
    }

    @Test
    fun `primaryStream is null when no streams`() {
        val station =
            Station(
                id = "test", name = "Test", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false, streams = emptyList(),
            )
        assertNull(station.primaryStream)
    }
}
