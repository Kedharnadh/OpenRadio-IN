package dev.openradio.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IcecastStatusTest {
    @Test
    fun `parses icecast status with object source title`() {
        val json =
            """
            {"icestats":{"source":{"bitrate":320,"listeners":6,
              "listenurl":"http://radio.mslivecdn.com:8124/stream",
              "server_name":"Online Radio","server_type":"audio/mpeg",
              "title":"VINNAPAMU","dummy":null}}}
            """.trimIndent()
        assertEquals("VINNAPAMU", MetadataRepository().parseIcecastStatus(json))
    }

    @Test
    fun `parses icecast status preferring song over title`() {
        val json =
            """
            {"icestats":{"source":{"song":"Artist - Track","title":"Track",
              "server_name":"Station"}}}
            """.trimIndent()
        assertEquals("Artist - Track", MetadataRepository().parseIcecastStatus(json))
    }

    @Test
    fun `parses icecast status with array source picking a titled mount`() {
        val json =
            """
            {"icestats":{"source":[
              {"server_name":"Mount A"},
              {"title":"  Song Two  "}
            ]}}
            """.trimIndent()
        assertEquals("Song Two", MetadataRepository().parseIcecastStatus(json))
    }

    @Test
    fun `returns null when no song or title is reported`() {
        val json =
            """
            {"icestats":{"source":{"server_name":"Online Radio","dummy":null}}}
            """.trimIndent()
        assertNull(MetadataRepository().parseIcecastStatus(json))
    }

    @Test
    fun `returns null for invalid json or missing icestats`() {
        assertNull(MetadataRepository().parseIcecastStatus("not json"))
        assertNull(MetadataRepository().parseIcecastStatus("""{"foo":123}"""))
    }

    @Test
    fun `derives icecast status url keeping scheme host and port`() {
        assertEquals(
            "https://radio.mslivecdn.com:6278/status-json.xsl",
            MetadataRepository().deriveIcecastStatusUrl("https://radio.mslivecdn.com:6278/stream"),
        )
    }

    @Test
    fun `derives icecast status url dropping an existing path`() {
        assertEquals(
            "https://example.com:8000/status-json.xsl",
            MetadataRepository().deriveIcecastStatusUrl("https://example.com:8000/radio.mp3"),
        )
    }

    @Test
    fun `rejects unsupported schemes and invalid urls`() {
        assertNull(MetadataRepository().deriveIcecastStatusUrl("ftp://example.com/stream"))
        assertNull(MetadataRepository().deriveIcecastStatusUrl("not a url"))
    }
}
