package com.wanderlist.data.repository

import com.wanderlist.data.remote.api.OrsApi
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrsGeocodingRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: OrsGeocodingRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        repository = OrsGeocodingRepository(server.api<OrsApi>())
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `search maps features and swaps lon-lat to lat-lon`() = runTest {
        server.enqueueJson(sample("ors-geocode-search.json"))

        val results = repository.search("Poznań").getOrThrow()

        assertEquals(2, results.size)
        assertEquals("Stary Rynek, Poznań, Polska", results[0].label)
        assertEquals(GeoPoint(latitude = 52.4082, longitude = 16.9335), results[0].location)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/geocode/search?"))
        assertTrue(request.requestUrl!!.queryParameter("text") == "Poznań")
    }

    @Test
    fun `blank query returns empty list without network call`() = runTest {
        val results = repository.search("   ").getOrThrow()

        assertTrue(results.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `search skips features without coordinates or label`() = runTest {
        server.enqueueJson(
            """{"features":[
              {"geometry":{"coordinates":[16.9]},"properties":{"label":"bez szerokości"}},
              {"geometry":{"coordinates":[16.9,52.4]},"properties":{}},
              {"geometry":{"coordinates":[16.9,52.4]},"properties":{"name":"Tylko nazwa"}}
            ]}""",
        )

        val results = repository.search("x").getOrThrow()

        assertEquals(listOf("Tylko nazwa"), results.map { it.label })
    }

    @Test
    fun `reverse returns label of first feature`() = runTest {
        server.enqueueJson(sample("ors-geocode-search.json"))

        val label = repository.reverse(GeoPoint(52.4082, 16.9335)).getOrThrow()

        assertEquals("Stary Rynek, Poznań, Polska", label)
        val url = server.takeRequest().requestUrl!!
        assertEquals("16.9335", url.queryParameter("point.lon"))
        assertEquals("52.4082", url.queryParameter("point.lat"))
    }

    @Test
    fun `reverse with no results returns null`() = runTest {
        server.enqueueJson("""{"features":[]}""")

        assertNull(repository.reverse(GeoPoint(0.0, 0.0)).getOrThrow())
    }

    @Test
    fun `server error maps to Network`() = runTest {
        server.enqueueJson("{}", code = 503)

        val error = repository.search("x").exceptionOrNull()

        assertTrue(error is AppError.Network)
        assertEquals(503, (error as AppError.Network).httpCode)
    }

    @Test
    fun `connection failure maps to Network`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(repository.search("x").exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `malformed json maps to Unknown`() = runTest {
        server.enqueueJson("to nie jest json")

        assertTrue(repository.search("x").exceptionOrNull() is AppError.Unknown)
    }
}
