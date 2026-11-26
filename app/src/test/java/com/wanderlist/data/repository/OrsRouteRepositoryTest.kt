package com.wanderlist.data.repository

import com.wanderlist.data.local.cache.ApiCacheDatabase
import com.wanderlist.data.remote.ApiKeyInterceptor
import com.wanderlist.data.remote.NetworkFactory
import com.wanderlist.data.remote.api.OrsApi
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.TransportMode
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrsRouteRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var db: ApiCacheDatabase
    private lateinit var repository: OrsRouteRepository
    private val network = OfflineSwitch()

    private val poznan = GeoPoint(52.4064, 16.9252)
    private val wroclaw = GeoPoint(51.1079, 17.0385)

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        db = inMemoryCache()
        val client = NetworkFactory.clientBuilder()
            .retryOnConnectionFailure(false) // test "offline" ma dostać błąd, a nie ponowione zapytanie
            .addInterceptor(network)
            .addInterceptor(ApiKeyInterceptor("test-key"))
            .build()
        repository = OrsRouteRepository(server.api<OrsApi>(client), db.apiCacheDao(), TestClock())
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun `maps successful response to route and sends profile, key and lon-lat order`() = runTest {
        server.enqueueJson(sample("ors-directions-car.json"))

        val route = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).getOrThrow()

        assertEquals(183400.2, route.distanceMeters, 0.001)
        assertEquals(7920.5, route.durationSeconds, 0.001)
        assertEquals(5, route.points.size)
        assertEquals(GeoPoint(52.4064, 16.9252), route.points.first())
        assertFalse(route.isFromCache)

        val request = server.takeRequest()
        assertEquals("/v2/directions/driving-car/geojson", request.path)
        assertEquals("test-key", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("[[16.9252,52.4064],[17.0385,51.1079]]"))
    }

    @Test
    fun `uses profile matching transport mode`() = runTest {
        server.enqueueJson(sample("ors-directions-car.json"))
        server.enqueueJson(sample("ors-directions-car.json"))

        repository.getRoute(listOf(poznan, wroclaw), TransportMode.WALKING)
        repository.getRoute(listOf(poznan, wroclaw), TransportMode.CYCLING)

        assertEquals("/v2/directions/foot-walking/geojson", server.takeRequest().path)
        assertEquals("/v2/directions/cycling-regular/geojson", server.takeRequest().path)
    }

    @Test
    fun `fewer than two points is a validation error without network call`() = runTest {
        val error = repository.getRoute(listOf(poznan), TransportMode.DRIVING).exceptionOrNull()

        assertTrue(error is AppError.Validation)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `invalid coordinates are a validation error`() = runTest {
        val error = repository.getRoute(listOf(poznan, GeoPoint(123.0, 0.0)), TransportMode.DRIVING).exceptionOrNull()

        assertTrue(error is AppError.Validation)
    }

    @Test
    fun `no routable point maps to NotFound`() = runTest {
        server.enqueueJson(sample("ors-directions-error.json"), code = 404)

        val error = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).exceptionOrNull()

        assertTrue(error is AppError.NotFound)
    }

    @Test
    fun `rate limit maps to Network with http code`() = runTest {
        server.enqueueJson("""{"error":"rate limit"}""", code = 429)

        val error = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).exceptionOrNull()

        assertTrue(error is AppError.Network)
        assertEquals(429, (error as AppError.Network).httpCode)
    }

    @Test
    fun `bad api key maps to Unknown and does not use cache`() = runTest {
        server.enqueueJson(sample("ors-directions-car.json"))
        repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING)
        server.enqueueJson("""{"error":"forbidden"}""", code = 403)

        val error = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).exceptionOrNull()

        assertTrue(error is AppError.Unknown)
    }

    @Test
    fun `network failure returns last saved route marked as cached`() = runTest {
        server.enqueueJson(sample("ors-directions-car.json"))
        val online = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).getOrThrow()
        network.offline = true

        val offline = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).getOrThrow()

        assertTrue(offline.isFromCache)
        assertEquals(online.points, offline.points)
        assertEquals(online.distanceMeters, offline.distanceMeters, 0.001)
    }

    @Test
    fun `network failure without cache is a Network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING).exceptionOrNull()

        assertTrue(error is AppError.Network)
    }

    @Test
    fun `cache is keyed by transport mode`() = runTest {
        server.enqueueJson(sample("ors-directions-car.json"))
        repository.getRoute(listOf(poznan, wroclaw), TransportMode.DRIVING)
        network.offline = true

        val error = repository.getRoute(listOf(poznan, wroclaw), TransportMode.WALKING).exceptionOrNull()

        assertTrue(error is AppError.Network)
    }
}
