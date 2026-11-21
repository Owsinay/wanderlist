package com.wanderlist.domain.usecase

import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.PlaceInput
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Priority
import com.wanderlist.testClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavePlaceUseCaseTest {
    private val repository = FakePlaceRepository(initial = emptyList())
    private val savePlace = SavePlaceUseCase(repository, ValidatePlaceInputUseCase(), testClock)
    private val input = PlaceInput(name = "  Wawel ", location = GeoPoint(50.05, 19.93), address = " ", priority = Priority.HIGH)

    @Test
    fun `new place is trimmed and stored`() = runTest {
        val id = savePlace(0, input).getOrThrow()

        val place = repository.observePlace(id).first()!!
        assertEquals("Wawel", place.name)
        assertNull(place.address)
        assertEquals(Priority.HIGH, place.priority)
        assertEquals(testClock.instant(), place.createdAt)
    }

    @Test
    fun `invalid input is not stored`() = runTest {
        val result = savePlace(0, input.copy(name = ""))

        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Validation && error.field == "name")
        assertTrue(repository.observePlaces().first().isEmpty())
    }

    @Test
    fun `edit keeps status and creation date`() = runTest {
        val id = savePlace(0, input).getOrThrow()
        val original = repository.observePlace(id).first()!!
        repository.updatePlace(original.copy(status = PlaceStatus.VISITED))

        savePlace(id, input.copy(name = "Zamek Królewski")).getOrThrow()

        val edited = repository.observePlace(id).first()!!
        assertEquals("Zamek Królewski", edited.name)
        assertEquals(PlaceStatus.VISITED, edited.status)
        assertEquals(original.createdAt, edited.createdAt)
    }

    @Test
    fun `editing a missing place fails with NotFound`() = runTest {
        assertTrue(savePlace(99, input).exceptionOrNull() is AppError.NotFound)
    }
}
