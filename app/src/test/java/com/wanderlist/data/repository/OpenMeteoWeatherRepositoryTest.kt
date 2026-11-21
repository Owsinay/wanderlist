package com.wanderlist.data.repository

import com.wanderlist.data.local.cache.ApiCacheDatabase
import com.wanderlist.data.remote.NetworkFactory
import com.wanderlist.data.remote.api.OpenMeteoApi
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenMeteoWeatherRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var db: ApiCacheDatabase
    private lateinit var clock: TestClock
    private val network = OfflineSwitch()
    private lateinit var repository: OpenMeteoWeatherRepository

    private val poznan = GeoPoint(52.4, 16.9)

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        db = inMemoryCache()
        clock = TestClock()
        repository = OpenMeteoWeatherRepository(server.api<OpenMeteoApi>(NetworkFactory.clientBuilder().addInterceptor(network).build()), db.apiCacheDao(), clock)
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun `maps current weather and sends coordinates`() = runTest {
        server.enqueueJson(sample("open-meteo-forecast.json"))

        val weather = repository.getCurrentWeather(poznan).getOrThrow()

        assertEquals(21.4, weather.temperatureCelsius, 0.001)
        assertEquals(11.2, weather.windSpeedKmh, 0.001)
        assertEquals(2, weather.weatherCode)
        val url = server.takeRequest().requestUrl!!
        assertEquals("52.4", url.queryParameter("latitude"))
        assertEquals("16.9", url.queryParameter("longitude"))
    }

    @Test
    fun `fresh cache is returned without second request`() = runTest {
        server.enqueueJson(sample("open-meteo-forecast.json"))
        repository.getCurrentWeather(poznan)
        clock.nowMillis += TimeUnit.MINUTES.toMillis(30)

        val weather = repository.getCurrentWeather(poznan).getOrThrow()

        assertEquals(1, server.requestCount)
        assertEquals(21.4, weather.temperatureCelsius, 0.001)
    }

    @Test
    fun `stale cache triggers a new request`() = runTest {
        server.enqueueJson(sample("open-meteo-forecast.json"))
        repository.getCurrentWeather(poznan)
        clock.nowMillis += TimeUnit.HOURS.toMillis(2)
        server.enqueueJson(
            """{"current":{"temperature_2m":5.0,"weather_code":71,"wind_speed_10m":3.0}}""",
        )

        val weather = repository.getCurrentWeather(poznan).getOrThrow()

        assertEquals(2, server.requestCount)
        assertEquals(5.0, weather.temperatureCelsius, 0.001)
    }

    @Test
    fun `stale cache is used when offline`() = runTest {
        server.enqueueJson(sample("open-meteo-forecast.json"))
        repository.getCurrentWeather(poznan)
        clock.nowMillis += TimeUnit.HOURS.toMillis(5)
        network.offline = true

        val weather = repository.getCurrentWeather(poznan).getOrThrow()

        assertEquals(21.4, weather.temperatureCelsius, 0.001)
    }

    @Test
    fun `offline without cache is a Network error`() = runTest {
        network.offline = true

        assertTrue(repository.getCurrentWeather(poznan).exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `nearby points share cache entry`() = runTest {
        server.enqueueJson(sample("open-meteo-forecast.json"))
        repository.getCurrentWeather(GeoPoint(52.4001, 16.9001))

        repository.getCurrentWeather(GeoPoint(52.4004, 16.8996))

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `invalid coordinates are a validation error without network call`() = runTest {
        val error = repository.getCurrentWeather(GeoPoint(123.0, 0.0)).exceptionOrNull()

        assertTrue(error is AppError.Validation)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `api 400 error maps to Unknown`() = runTest {
        server.enqueueJson(sample("open-meteo-error.json"), code = 400)

        assertTrue(repository.getCurrentWeather(poznan).exceptionOrNull() is AppError.Unknown)
    }
}
