package dev.openradio.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingTest {
    @Test
    fun `NowPlaying data class holds title and art URL`() {
        val nowPlaying = NowPlaying(streamTitle = "Song Name", artUrl = "https://example.com/art.jpg")
        assertEquals("Song Name", nowPlaying.streamTitle)
        assertEquals("https://example.com/art.jpg", nowPlaying.artUrl)
    }

    @Test
    fun `EpgProgram holds start, end, and title`() {
        val program = EpgProgram(start = "09:00", end = "10:00", title = "Morning News")
        assertEquals("09:00", program.start)
        assertEquals("10:00", program.end)
        assertEquals("Morning News", program.title)
    }

    @Test
    fun `EpgSchedule holds date and programs list`() {
        val programs =
            listOf(
                EpgProgram(start = "09:00", end = "10:00", title = "News"),
                EpgProgram(start = "10:00", end = "11:00", title = "Music"),
            )
        val schedule = EpgSchedule(date = "2026-09-03", programs = programs)
        assertEquals("2026-09-03", schedule.date)
        assertEquals(2, schedule.programs.size)
        assertEquals("News", schedule.programs[0].title)
        assertEquals("Music", schedule.programs[1].title)
    }

    @Test
    fun `EpgSchedule with empty programs`() {
        val schedule = EpgSchedule(date = "2026-09-03", programs = emptyList())
        assertEquals(0, schedule.programs.size)
    }
}
