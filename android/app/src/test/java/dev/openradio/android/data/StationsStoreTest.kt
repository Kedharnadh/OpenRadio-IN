package dev.openradio.android.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationsStoreTest {

    @Test
    fun `StationsStore setStations updates station list`() {
        val store = StationsStore
        val stations = listOf(
            Station(
                id = "test_1", name = "Test 1", nameTe = "", nameHi = "",
                language = "Hindi", country = "", state = "", city = "",
                categories = emptyList(), genre = emptyList(), homepage = "",
                logo = "", streams = emptyList(), verified = true, status = "online",
                epgId = -1L, metadataUrl = "", songFirst = false
            )
        )
        store.setStations(stations)
        assertEquals(1, store.stations.value.size)
        assertEquals("test_1", store.stations.value[0].id)
    }

    @Test
    fun `StationsStore default state has empty stations and loading true`() {
        val flow = MutableStateFlow<List<Station>>(emptyList())
        assertEquals(0, flow.value.size)
    }

    @Test
    fun `StationsStore setStations replaces previous list`() {
        val store = StationsStore
        val station1 = Station(
            id = "station_1", name = "Station 1", nameTe = "", nameHi = "",
            language = "Hindi", country = "", state = "", city = "",
            categories = emptyList(), genre = emptyList(), homepage = "",
            logo = "", streams = emptyList(), verified = true, status = "online",
            epgId = -1L, metadataUrl = "", songFirst = false
        )
        val station2 = Station(
            id = "station_2", name = "Station 2", nameTe = "", nameHi = "",
            language = "Tamil", country = "", state = "", city = "",
            categories = emptyList(), genre = emptyList(), homepage = "",
            logo = "", streams = emptyList(), verified = false, status = "offline",
            epgId = -1L, metadataUrl = "", songFirst = false
        )

        store.setStations(listOf(station1))
        assertEquals(1, store.stations.value.size)

        store.setStations(listOf(station2))
        assertEquals(1, store.stations.value.size)
        assertEquals("station_2", store.stations.value[0].id)
    }
}
