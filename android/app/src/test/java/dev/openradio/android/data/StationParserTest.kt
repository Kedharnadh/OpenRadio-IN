package dev.openradio.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationParserTest {
    @Test
    fun `parse returns empty list for blank input`() {
        assertEquals(emptyList<Station>(), StationParser.parse(""))
        assertEquals(emptyList<Station>(), StationParser.parse("   "))
        assertEquals(emptyList<Station>(), StationParser.parse("[]"))
    }

    @Test
    fun `parse returns empty list for invalid JSON`() {
        assertEquals(emptyList<Station>(), StationParser.parse("not json"))
    }

    @Test
    fun `parse correctly parses a single station`() {
        val json =
            """
            [
                {
                    "id": "air_hyderabad",
                    "name": "AIR Hyderabad",
                    "name_te": "ఏఐఆర్ హైదరాబాద్",
                    "name_hi": "एआईआर हैदराबाद",
                    "language": "Telugu",
                    "country": "India",
                    "state": "Andhra Pradesh",
                    "city": "Hyderabad",
                    "categories": ["AIR", "News"],
                    "genre": ["Talk"],
                    "homepage": "https://example.com",
                    "logo": "https://example.com/logo.png",
                    "streams": [
                        {
                            "url": "https://example.com/live.m3u8",
                            "codec": "HLS",
                            "priority": 1
                        }
                    ],
                    "verified": true,
                    "status": "online",
                    "epg_id": 411,
                    "metadata_url": "https://example.com/api",
                    "song_first": false
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(1, stations.size)

        val station = stations[0]
        assertEquals("air_hyderabad", station.id)
        assertEquals("AIR Hyderabad", station.name)
        assertEquals("ఏఐఆర్ హైదరాబాద్", station.nameTe)
        assertEquals("एआईआर हैदराबाद", station.nameHi)
        assertEquals("Telugu", station.language)
        assertEquals("India", station.country)
        assertEquals("Andhra Pradesh", station.state)
        assertEquals("Hyderabad", station.city)
        assertEquals(listOf("AIR", "News"), station.categories)
        assertEquals(listOf("Talk"), station.genre)
        assertEquals("https://example.com", station.homepage)
        assertEquals("https://example.com/logo.png", station.logo)
        assertEquals(1, station.streams.size)
        assertEquals("https://example.com/live.m3u8", station.streams[0].url)
        assertEquals("HLS", station.streams[0].codec)
        assertEquals(1, station.streams[0].priority)
        assertTrue(station.verified)
        assertEquals("online", station.status)
        assertEquals(411L, station.epgId)
        assertEquals("https://example.com/api", station.metadataUrl)
        assertEquals(false, station.songFirst)
    }

    @Test
    fun `parse correctly parses multiple stations`() {
        val json =
            """
            [
                {
                    "id": "station_1",
                    "name": "Station One",
                    "language": "Hindi"
                },
                {
                    "id": "station_2",
                    "name": "Station Two",
                    "language": "Tamil",
                    "categories": ["FM"]
                },
                {
                    "id": "station_3",
                    "name": "Station Three"
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(3, stations.size)
        assertEquals("station_1", stations[0].id)
        assertEquals("station_2", stations[1].id)
        assertEquals("Hindi", stations[0].language)
        assertEquals("Tamil", stations[1].language)
        assertEquals(listOf("FM"), stations[1].categories)
    }

    @Test
    fun `parse handles missing optional fields gracefully`() {
        val json =
            """
            [
                {
                    "id": "minimal_station",
                    "name": "Minimal Station"
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(1, stations.size)

        val station = stations[0]
        assertEquals("minimal_station", station.id)
        assertEquals("Minimal Station", station.name)
        assertEquals("", station.nameTe)
        assertEquals("", station.nameHi)
        assertEquals("", station.language)
        assertEquals("", station.country)
        assertEquals(emptyList<String>(), station.categories)
        assertEquals(emptyList<String>(), station.genre)
        assertEquals("", station.logo)
        assertEquals(emptyList<Station.Stream>(), station.streams)
        assertEquals(false, station.verified)
        assertEquals("unknown", station.status)
        assertEquals(-1L, station.epgId)
    }

    @Test
    fun `parse skips stations with invalid JSON objects`() {
        val json =
            """
            [
                {
                    "id": "valid_station",
                    "name": "Valid Station"
                },
                null,
                123,
                "not an object"
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(1, stations.size)
        assertEquals("valid_station", stations[0].id)
    }

    @Test
    fun `parse skips streams with blank URLs`() {
        val json =
            """
            [
                {
                    "id": "test",
                    "name": "Test",
                    "streams": [
                        { "url": "", "codec": "HLS", "priority": 1 },
                        { "url": "https://example.com/stream.mp3", "codec": "MP3", "priority": 2 }
                    ]
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(1, stations.size)
        assertEquals(1, stations[0].streams.size)
        assertEquals("https://example.com/stream.mp3", stations[0].streams[0].url)
    }

    @Test
    fun `parse assigns max priority when priority is missing`() {
        val json =
            """
            [
                {
                    "id": "test",
                    "name": "Test",
                    "streams": [
                        { "url": "https://example.com/stream.mp3" }
                    ]
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(Int.MAX_VALUE, stations[0].streams[0].priority)
    }

    @Test
    fun `parse handles empty streams array`() {
        val json =
            """
            [
                {
                    "id": "test",
                    "name": "Test",
                    "streams": []
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(0, stations[0].streams.size)
    }

    @Test
    fun `parse handles missing streams key`() {
        val json =
            """
            [
                {
                    "id": "test",
                    "name": "Test"
                }
            ]
            """.trimIndent()

        val stations = StationParser.parse(json)
        assertEquals(0, stations[0].streams.size)
    }
}
