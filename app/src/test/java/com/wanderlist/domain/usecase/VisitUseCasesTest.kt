package com.wanderlist.domain.usecase

import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.FakeVisitRepository
import com.wanderlist.data.fake.SampleData
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.VisitInputError
import com.wanderlist.testClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VisitUseCasesTest {
    private val place = SampleData.places.first { it.status == PlaceStatus.WANT_TO_VISIT }
    private val places = FakePlaceRepository(initial = listOf(place))
    private val visits = FakeVisitRepository()
    private val markVisited = MarkPlaceVisitedUseCase(places, visits, testClock)
    private val deleteVisit = DeleteVisitUseCase(places, visits)
    private val today = LocalDate.now(testClock)

    @Test
    fun `marking as visited stores the visit and changes status`() = runTest {
        markVisited(place.id, today, rating = 5, memo = " Super ").getOrThrow()

        assertEquals(PlaceStatus.VISITED, places.observePlace(place.id).first()!!.status)
        val visit = visits.observeVisitsForPlace(place.id).first().single()
        assertEquals(5, visit.rating)
        assertEquals("Super", visit.memo)
    }

    @Test
    fun `rating outside 1 to 5 and future date are rejected`() {
        assertEquals(listOf(VisitInputError.RATING_OUT_OF_RANGE), markVisited.validate(today, 0, null))
        assertEquals(listOf(VisitInputError.RATING_OUT_OF_RANGE), markVisited.validate(today, 6, null))
        assertEquals(listOf(VisitInputError.DATE_IN_FUTURE), markVisited.validate(today.plusDays(1), 3, null))
    }

    @Test
    fun `invalid visit leaves the place unchanged`() = runTest {
        val result = markVisited(place.id, today, rating = 9)

        assertTrue(result.exceptionOrNull() is AppError.Validation)
        assertEquals(PlaceStatus.WANT_TO_VISIT, places.observePlace(place.id).first()!!.status)
        assertTrue(visits.observeVisits().first().isEmpty())
    }

    @Test
    fun `visit for a missing place fails with NotFound`() = runTest {
        assertTrue(markVisited(999, today, rating = 3).exceptionOrNull() is AppError.NotFound)
    }

    @Test
    fun `deleting the last visit moves the place back to want to visit`() = runTest {
        val first = markVisited(place.id, today.minusDays(3), rating = 4).getOrThrow()
        val second = markVisited(place.id, today, rating = 5).getOrThrow()

        deleteVisit(place.id, first).getOrThrow()
        assertEquals(PlaceStatus.VISITED, places.observePlace(place.id).first()!!.status)

        deleteVisit(place.id, second).getOrThrow()
        assertEquals(PlaceStatus.WANT_TO_VISIT, places.observePlace(place.id).first()!!.status)
    }
}
