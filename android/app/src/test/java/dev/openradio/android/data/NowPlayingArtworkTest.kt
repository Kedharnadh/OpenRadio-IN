package dev.openradio.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the cover-art plumbing: the structured fields the metadata proxy now
 * reports, the search terms derived from them, and the display line shown on the
 * car screen, the lock screen and in the app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowPlayingArtworkTest {
    @Test
    fun `display prefers structured song fields`() {
        val nowPlaying =
            NowPlaying(
                streamTitle = "Devi Sri Prasad - Thandaane Thandaane",
                artUrl = "",
                title = "Thandaane Thandaane",
                artist = "Devi Sri Prasad",
                album = "Vinaya Vidheya Rama",
            )
        assertEquals("Thandaane Thandaane - Devi Sri Prasad - Vinaya Vidheya Rama", nowPlaying.display)
    }

    @Test
    fun `display falls back to the raw stream title when nothing is structured`() {
        val nowPlaying = NowPlaying(streamTitle = "AIR Delhi - Morning News", artUrl = "")
        assertEquals("AIR Delhi - Morning News", nowPlaying.display)
    }

    @Test
    fun `search terms combine artist and track for structured metadata`() {
        val nowPlaying =
            NowPlaying(
                streamTitle = "Artist - Song",
                artUrl = "",
                title = "Song",
                artist = "Artist",
            )
        assertEquals(listOf("Artist Song", "Song"), nowPlaying.searchTerms)
    }

    @Test
    fun `search terms split an ICY-only stream title into artist and track`() {
        val nowPlaying = NowPlaying(streamTitle = "S. P. Balasubrahmanyam - Ye Chinna Kaada", artUrl = "")
        assertEquals(listOf("S. P. Balasubrahmanyam Ye Chinna Kaada", "Ye Chinna Kaada"), nowPlaying.searchTerms)
    }

    @Test
    fun `search terms fall back to the whole title when there is no separator`() {
        val nowPlaying = NowPlaying(streamTitle = "News Update", artUrl = "")
        assertEquals(listOf("News Update"), nowPlaying.searchTerms)
    }

    @Test
    fun `station supplied art is kept as-is`() {
        val nowPlaying =
            NowPlaying(
                streamTitle = "MLR - Thandaane Thandaane",
                artUrl = "https://a1.asurahosting.com/api/station/melody_radio/art/abc.jpg",
            )
        assertEquals(
            "https://a1.asurahosting.com/api/station/melody_radio/art/abc.jpg",
            nowPlaying.artUrl,
        )
    }

    @Test
    fun `iTunes cover art is upgraded above the 100x100 default`() {
        val repo = ArtworkRepository(RuntimeEnvironment.getApplication())
        val upgraded =
            repo.upgradeItunesArtwork(
                "https://is1-ssl.mzstatic.com/image/thumb/abc/100x100bb.jpg",
            )
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/abc/600x600bb.jpg", upgraded)
    }

    @Test
    fun `non iTunes artwork is left untouched`() {
        val repo = ArtworkRepository(RuntimeEnvironment.getApplication())
        val url = "https://cdn.example.com/cover_big.jpg"
        assertEquals(url, repo.upgradeItunesArtwork(url))
    }

    @Test
    fun `artwork resolve returns station supplied art without a lookup`() {
        val repo = ArtworkRepository(RuntimeEnvironment.getApplication())
        val art =
            kotlinx.coroutines.runBlocking {
                repo.resolve(
                    NowPlaying(streamTitle = "A - B", artUrl = "https://example.com/art.jpg"),
                    stationName = "AIR Delhi",
                )
            }
        assertEquals("https://example.com/art.jpg", art)
    }

    @Test
    fun `artwork resolve returns empty string when there is no title to search on`() {
        val repo = ArtworkRepository(RuntimeEnvironment.getApplication())
        val art =
            kotlinx.coroutines.runBlocking {
                repo.resolve(NowPlaying(streamTitle = "", artUrl = ""), stationName = "")
            }
        assertTrue(art.isEmpty())
    }
}
