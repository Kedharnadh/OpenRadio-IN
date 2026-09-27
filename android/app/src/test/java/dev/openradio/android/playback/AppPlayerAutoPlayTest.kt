package dev.openradio.android.playback

import androidx.media3.common.MediaItem
import dev.openradio.android.Prefs
import dev.openradio.android.data.Station
import dev.openradio.android.data.StationsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Reproduces the Android Auto play path: a controller sends one station id with
 * a start index, and the session has to hand back a playlist that actually
 * starts on that station.
 *
 * Before the fix the session returned the whole browsing queue from the play
 * callback, so the caller's start index (always 0 from Android Auto) landed on
 * the first station of the list rather than the station that was tapped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppPlayerAutoPlayTest {
    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        Prefs.init(context)
        StationsStore.setStations(
            listOf(
                station("a", "AIR Adilabad"),
                station("b", "AIR Kadapa"),
                station("c", "Radio One"),
                station("d", "Radio Two"),
            ),
        )
        Prefs.setFavorites(emptySet())
    }

    private fun station(
        id: String,
        name: String,
    ): Station =
        Station(
            id = id, name = name, nameTe = "", nameHi = "", nameKn = "",
            language = "Telugu", country = "India", state = "", city = "",
            categories = emptyList(), genre = emptyList(), homepage = "",
            logo = "https://example.com/$id.png",
            streams = listOf(Station.Stream("https://example.com/$id.mp3", "MP3", 1)),
            verified = true, status = "online", epgId = -1L,
            metadataUrl = "", songFirst = false,
        )

    /** Mirrors what the session does for a controller's play request. */
    private fun stationThatPlays(
        tappedId: String,
        startIndex: Int = 0,
    ): MediaItem? {
        val queue = AppPlayer.contextQueue(listOf(tappedId))
        val targetIndex = queue.indexOfFirst { it.mediaId == tappedId }
        if (targetIndex < 0) return null
        return AppPlayer.rotateTo(queue, targetIndex, startIndex)[startIndex]
    }

    @Test
    fun `tapping a station outside the favorites list plays that station`() {
        assertEquals("b", stationThatPlays("b")?.mediaId)
    }

    @Test
    fun `tapping the last station of the full list plays that station`() {
        assertEquals("d", stationThatPlays("d")?.mediaId)
    }

    @Test
    fun `tapping a favorite plays that favorite and not the first one`() {
        Prefs.setFavorites(setOf("c", "d"))
        assertEquals("c", stationThatPlays("c")?.mediaId)
        assertEquals("d", stationThatPlays("d")?.mediaId)
    }

    @Test
    fun `tapping the second favorite plays the second favorite`() {
        Prefs.setFavorites(setOf("c", "d"))
        assertEquals("d", stationThatPlays("d")?.mediaId)
    }

    @Test
    fun `favorites context queue keeps only the favorites`() {
        Prefs.setFavorites(setOf("c", "d"))
        assertEquals(listOf("c", "d"), AppPlayer.contextQueue(listOf("c")).map { it.mediaId })
    }

    @Test
    fun `non favorites context queue is the full station list`() {
        assertEquals(listOf("a", "b", "c", "d"), AppPlayer.contextQueue(listOf("b")).map { it.mediaId })
    }

    @Test
    fun `resolved station is playable and carries its stream`() {
        val resolved = AppPlayer.resolveStations(listOf("b"))
        assertNotNull(resolved)
        assertEquals(1, resolved?.size)
        assertEquals("b", resolved?.first()?.mediaId)
        assertEquals("https://example.com/b.mp3", resolved?.first()?.localConfiguration?.uri.toString())
    }

    @Test
    fun `unknown station id resolves to nothing so the caller can fall back`() {
        assertEquals(null, AppPlayer.resolveStations(listOf("missing")))
    }

    @Test
    fun `requested stations keep their order and count`() {
        val resolved = AppPlayer.resolveStations(listOf("d", "a"))
        assertEquals(listOf("d", "a"), resolved?.map { it.mediaId })
    }
}
