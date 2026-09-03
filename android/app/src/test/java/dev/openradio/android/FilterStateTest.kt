package dev.openradio.android

import dev.openradio.android.data.Station
import dev.openradio.android.ui.FilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterStateTest {

    private fun createStation(
        id: String,
        name: String = "Station $id",
        language: String = "Hindi",
        categories: List<String> = listOf("AIR")
    ): Station {
        return Station(
            id = id,
            name = name,
            nameTe = "",
            nameHi = "",
            language = language,
            country = "India",
            state = "",
            city = "",
            categories = categories,
            genre = emptyList(),
            homepage = "",
            logo = "",
            streams = listOf(Station.Stream("https://example.com/$id.mp3", "MP3", 1)),
            verified = true,
            status = "online",
            epgId = -1L,
            metadataUrl = "",
            songFirst = false
        )
    }

    private fun matchesFilter(station: Station, filter: FilterState): Boolean {
        val matchesQuery = filter.query.isBlank() ||
            station.name.contains(filter.query, ignoreCase = true) ||
            station.language.contains(filter.query, ignoreCase = true) ||
            station.city.contains(filter.query, ignoreCase = true) ||
            station.categories.any { it.contains(filter.query, ignoreCase = true) }
        val matchesLanguage = filter.language == null || station.languageTags.contains(filter.language)
        val matchesCategory = filter.category == null || station.categories.contains(filter.category)
        return matchesQuery && matchesLanguage && matchesCategory
    }

    @Test
    fun `empty filter matches all stations`() {
        val filter = FilterState()
        val stations = listOf(
            createStation("1", language = "Hindi"),
            createStation("2", language = "Telugu"),
            createStation("3", language = "Tamil")
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(3, filtered.size)
    }

    @Test
    fun `query filter matches station name`() {
        val filter = FilterState(query = "AIR")
        val stations = listOf(
            createStation("1", name = "AIR Hyderabad"),
            createStation("2", name = "Radio Mirchi"),
            createStation("3", name = "AIR Delhi")
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(2, filtered.size)
    }

    @Test
    fun `query filter is case insensitive`() {
        val filter = FilterState(query = "air")
        val stations = listOf(
            createStation("1", name = "AIR Hyderabad"),
            createStation("2", name = "Radio Mirchi")
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(1, filtered.size)
        assertEquals("AIR Hyderabad", filtered[0].name)
    }

    @Test
    fun `query filter matches language`() {
        val filter = FilterState(query = "Telugu")
        val stations = listOf(
            createStation("1", language = "Hindi"),
            createStation("2", language = "Telugu"),
            createStation("3", language = "Tamil")
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(1, filtered.size)
        assertEquals("2", filtered[0].id)
    }

    @Test
    fun `query filter matches category`() {
        val filter = FilterState(query = "FM")
        val stations = listOf(
            createStation("1", categories = listOf("AIR")),
            createStation("2", categories = listOf("FM", "Music")),
            createStation("3", categories = listOf("News"))
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(1, filtered.size)
        assertEquals("2", filtered[0].id)
    }

    @Test
    fun `language filter matches exact language`() {
        val filter = FilterState(language = "Telugu")
        val stations = listOf(
            createStation("1", language = "Hindi"),
            createStation("2", language = "Telugu"),
            createStation("3", language = "Telugu, English")
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(2, filtered.size)
    }

    @Test
    fun `category filter matches exact category`() {
        val filter = FilterState(category = "FM")
        val stations = listOf(
            createStation("1", categories = listOf("AIR")),
            createStation("2", categories = listOf("FM")),
            createStation("3", categories = listOf("AIR", "FM"))
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(2, filtered.size)
    }

    @Test
    fun `query and language filter combine`() {
        val filter = FilterState(query = "AIR", language = "Hindi")
        val stations = listOf(
            createStation("1", name = "AIR Hyderabad", language = "Telugu"),
            createStation("2", name = "AIR Delhi", language = "Hindi"),
            createStation("3", name = "Radio Mirchi", language = "Hindi")
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(1, filtered.size)
        assertEquals("AIR Delhi", filtered[0].name)
    }

    @Test
    fun `all three filters combine`() {
        val filter = FilterState(query = "AIR", language = "Hindi", category = "News")
        val stations = listOf(
            createStation("1", name = "AIR Hyderabad", language = "Telugu", categories = listOf("AIR")),
            createStation("2", name = "AIR Delhi", language = "Hindi", categories = listOf("AIR", "News")),
            createStation("3", name = "AIR Lucknow", language = "Hindi", categories = listOf("AIR"))
        )
        val filtered = stations.filter { matchesFilter(it, filter) }
        assertEquals(1, filtered.size)
        assertEquals("AIR Delhi", filtered[0].name)
    }

    @Test
    fun `FilterState default values are correct`() {
        val filter = FilterState()
        assertEquals("", filter.query)
        assertEquals(null, filter.language)
        assertEquals(null, filter.category)
        assertFalse(filter.onlyFavorites)
    }
}
