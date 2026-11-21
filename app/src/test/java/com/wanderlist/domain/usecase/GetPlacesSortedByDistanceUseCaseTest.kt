package com.wanderlist.domain.usecase

import app.cash.turbine.test
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class GetPlacesSortedByDistanceUseCaseTest {
    private val krakow = Place(id = 1, name = "Kraków", location = GeoPoint(50.06, 19.94), createdAt = Instant.parse("2026-01-01T00:00:00Z"))
    private val gdansk = Place(id = 2, name = "Gdańsk", location = GeoPoint(54.35, 18.65), createdAt = Instant.parse("2026-03-01T00:00:00Z"))
    private val warsaw = Place(id = 3, name = "Warszawa", location = GeoPoint(52.23, 21.01), createdAt = Instant.parse("2026-02-01T00:00:00Z"))
    private val repository = FakePlaceRepository(initial = listOf(krakow, gdansk, warsaw))
    private val useCase = GetPlacesSortedByDistanceUseCase(repository)

    @Test
    fun `places are sorted from nearest to farthest`() = runTest {
        val katowice = GeoPoint(50.26, 19.02)
        useCase(katowice).test {
            val result = awaitItem()
            assertEquals(listOf("Kraków", "Warszawa", "Gdańsk"), result.map { it.place.name })
            assertEquals(68_000.0, result.first().distanceMeters!!, 3_000.0)
        }
    }

    @Test
    fun `without location places are sorted newest first`() = runTest {
        useCase(null).test {
            val result = awaitItem()
            assertEquals(listOf("Gdańsk", "Warszawa", "Kraków"), result.map { it.place.name })
            assertNull(result.first().distanceMeters)
        }
    }

    @Test
    fun `list is updated when a place is added`() = runTest {
        useCase(GeoPoint(50.06, 19.94)).test {
            awaitItem()
            repository.addPlace(Place(name = "Wieliczka", location = GeoPoint(49.98, 20.05)))
            assertEquals(listOf("Kraków", "Wieliczka", "Warszawa", "Gdańsk"), awaitItem().map { it.place.name })
        }
    }
}
