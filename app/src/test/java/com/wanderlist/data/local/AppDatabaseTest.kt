package com.wanderlist.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanderlist.data.local.entity.PlaceEntity
import com.wanderlist.data.local.entity.TripEntity
import com.wanderlist.data.local.entity.VisitEntity
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Priority
import com.wanderlist.domain.model.TransportMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.seedCallback)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun place(name: String) = PlaceEntity(
        name = name,
        latitude = 50.0,
        longitude = 20.0,
        address = null,
        categoryId = null,
        status = PlaceStatus.WANT_TO_VISIT,
        priority = Priority.MEDIUM,
        note = null,
        createdAt = Instant.now(),
    )

    @Test
    fun `default categories are seeded on create`() = runTest {
        val categories = db.categoryDao().observeAll().first()
        assertEquals(AppDatabase.DEFAULT_CATEGORIES.size, categories.size)
    }

    @Test
    fun `deleting a place deletes its visits`() = runTest {
        val placeId = db.placeDao().insert(place("Wawel"))
        db.visitDao().insert(VisitEntity(placeId = placeId, date = LocalDate.now(), rating = 5, memo = null, photoUri = null))

        db.placeDao().deleteById(placeId)

        assertTrue(db.visitDao().observeAll().first().isEmpty())
    }

    @Test
    fun `trip places are returned in the saved order`() = runTest {
        val a = db.placeDao().insert(place("A"))
        val b = db.placeDao().insert(place("B"))
        val c = db.placeDao().insert(place("C"))
        val tripId = db.tripDao().insert(TripEntity(name = "Test", date = null, transportMode = TransportMode.WALKING))

        db.tripDao().replaceTripPlaces(tripId, listOf(c, a, b))

        val names = db.tripDao().observePlacesForTrip(tripId).first().map { it.name }
        assertEquals(listOf("C", "A", "B"), names)
    }
}
