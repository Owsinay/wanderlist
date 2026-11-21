package com.wanderlist.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.wanderlist.data.local.AppDatabase
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Visit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRepositoriesTest {
    private lateinit var db: AppDatabase
    private lateinit var places: RoomPlaceRepository
    private lateinit var visits: RoomVisitRepository
    private lateinit var categories: RoomCategoryRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        places = RoomPlaceRepository(db.placeDao())
        visits = RoomVisitRepository(db.visitDao())
        categories = RoomCategoryRepository(db.categoryDao())
    }

    @After
    fun tearDown() = db.close()

    private val wawel = Place(name = "Wawel", location = GeoPoint(50.054, 19.935), address = "Kraków")

    @Test
    fun `added place is emitted with generated id and mapped fields`() = runTest {
        val id = places.addPlace(wawel).getOrThrow()

        val stored = places.observePlace(id).first()!!
        assertEquals("Wawel", stored.name)
        assertEquals(GeoPoint(50.054, 19.935), stored.location)
        assertEquals(PlaceStatus.WANT_TO_VISIT, stored.status)
    }

    @Test
    fun `places flow emits again after update and delete`() = runTest {
        places.observePlaces().test {
            assertTrue(awaitItem().isEmpty())

            val id = places.addPlace(wawel).getOrThrow()
            assertEquals("Wawel", awaitItem().single().name)

            places.updatePlace(wawel.copy(id = id, name = "Zamek Królewski na Wawelu")).getOrThrow()
            assertEquals("Zamek Królewski na Wawelu", awaitItem().single().name)

            places.deletePlace(id).getOrThrow()
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `updating or deleting a missing place fails with NotFound`() = runTest {
        assertTrue(places.updatePlace(wawel.copy(id = 99)).exceptionOrNull() is AppError.NotFound)
        assertTrue(places.deletePlace(99).exceptionOrNull() is AppError.NotFound)
    }

    @Test
    fun `visit for a missing place fails with database error`() = runTest {
        val result = visits.addVisit(Visit(placeId = 42, date = LocalDate.of(2026, 1, 1), rating = 4))
        assertTrue(result.exceptionOrNull() is AppError.Database)
    }

    @Test
    fun `visits are filtered by place and removed with it`() = runTest {
        val a = places.addPlace(wawel).getOrThrow()
        val b = places.addPlace(wawel.copy(name = "Sukiennice")).getOrThrow()
        visits.addVisit(Visit(placeId = a, date = LocalDate.of(2026, 1, 1), rating = 5)).getOrThrow()
        visits.addVisit(Visit(placeId = b, date = LocalDate.of(2026, 2, 1), rating = 3)).getOrThrow()

        assertEquals(1, visits.observeVisitsForPlace(a).first().size)

        places.deletePlace(a).getOrThrow()
        assertEquals(listOf(b), visits.observeVisits().first().map { it.placeId })
    }

    @Test
    fun `deleting a category keeps its places without category`() = runTest {
        val categoryId = categories.addCategory(Category(name = "Zamki", iconKey = "landmark", color = 0xFF000000)).getOrThrow()
        val placeId = places.addPlace(wawel.copy(categoryId = categoryId)).getOrThrow()

        categories.deleteCategory(categoryId).getOrThrow()

        assertNull(places.observePlace(placeId).first()!!.categoryId)
    }
}
