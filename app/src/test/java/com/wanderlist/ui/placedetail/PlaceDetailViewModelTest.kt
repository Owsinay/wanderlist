package com.wanderlist.ui.placedetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wanderlist.MainDispatcherRule
import com.wanderlist.data.fake.FakeCategoryRepository
import com.wanderlist.data.fake.FakePhotoStorage
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.FakeVisitRepository
import com.wanderlist.data.fake.TestSettingsRepository
import com.wanderlist.data.fake.TestWeatherRepository
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Visit
import com.wanderlist.domain.model.VisitInputError
import com.wanderlist.domain.model.Weather
import com.wanderlist.domain.usecase.DeletePlaceUseCase
import com.wanderlist.domain.usecase.DeleteVisitUseCase
import com.wanderlist.domain.usecase.MarkPlaceVisitedUseCase
import com.wanderlist.testClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class PlaceDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val place = Place(id = 5, name = "Morskie Oko", location = GeoPoint(49.2013, 20.0711), categoryId = 2)
    private val places = FakePlaceRepository(initial = listOf(place))
    private val visits = FakeVisitRepository()
    private val weather = TestWeatherRepository()
    private val photos = FakePhotoStorage()
    private val today = LocalDate.now(testClock)

    // Tworzony leniwie, czyli już po podmianie Dispatchers.Main przez regułę.
    private val viewModel by lazy {
        PlaceDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("placeId" to 5L)),
            placeRepository = places,
            visitRepository = visits,
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = TestSettingsRepository(),
            weatherRepository = weather,
            photoStorage = photos,
            markPlaceVisited = MarkPlaceVisitedUseCase(places, visits, testClock),
            deleteVisit = DeleteVisitUseCase(places, visits),
            deletePlace = DeletePlaceUseCase(places),
        )
    }

    private fun TestScope.collectState() {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
    }

    private suspend fun loadedState() = viewModel.uiState.first { !it.isLoading }

    @Test
    fun `place category and weather are loaded`() = runTest {
        collectState()
        val state = viewModel.uiState.first { it.weather is WeatherState.Success }

        assertEquals("Morskie Oko", state.place?.name)
        assertEquals("Natura", state.category?.name)
        assertEquals(21.0, (state.weather as WeatherState.Success).weather.temperatureCelsius, 0.0)
        assertFalse(state.notFound)
    }

    @Test
    fun `weather error can be retried`() = runTest {
        weather.result = Result.failure(AppError.Network())
        collectState()
        assertTrue(viewModel.uiState.first { it.weather !is WeatherState.Loading }.weather is WeatherState.Error)

        weather.result = Result.success(Weather(temperatureCelsius = 5.0, windSpeedKmh = 3.0, weatherCode = 61))
        viewModel.retryWeather()

        assertTrue(viewModel.uiState.first { it.weather is WeatherState.Success }.weather is WeatherState.Success)
        assertEquals(2, weather.calls)
    }

    @Test
    fun `invalid visit keeps dialog open with errors`() = runTest {
        collectState()
        viewModel.openVisitDialog()
        viewModel.saveVisit(today.plusDays(2), rating = 0, memo = "", pickedPhotoUri = null)

        val dialog = loadedState().visitDialog
        assertTrue(dialog.isOpen)
        assertEquals(listOf(VisitInputError.RATING_OUT_OF_RANGE, VisitInputError.DATE_IN_FUTURE), dialog.errors)
        assertTrue(visits.observeVisits().first().isEmpty())
    }

    @Test
    fun `valid visit with photo is saved and dialog closes`() = runTest {
        collectState()
        viewModel.eventFlow.test {
            viewModel.openVisitDialog()
            viewModel.saveVisit(today, rating = 5, memo = "Wschód słońca", pickedPhotoUri = "content://media/1")
            assertEquals(PlaceDetailEvent.VisitSaved, awaitItem())
        }

        val state = loadedState()
        assertFalse(state.visitDialog.isOpen)
        assertEquals(PlaceStatus.VISITED, state.place?.status)
        assertEquals("file:///photos/copy.jpg", state.visits.single().photoUri)
    }

    @Test
    fun `photo copy failure still saves the visit without photo`() = runTest {
        photos.importResult = { Result.failure(AppError.Unknown()) }
        collectState()
        viewModel.eventFlow.test {
            viewModel.saveVisit(today, rating = 4, memo = "", pickedPhotoUri = "content://media/2")
            assertTrue(awaitItem() is PlaceDetailEvent.ShowError)
            assertEquals(PlaceDetailEvent.VisitSaved, awaitItem())
        }
        assertEquals(null, visits.observeVisits().first().single().photoUri)
    }

    @Test
    fun `deleting a visit removes its photo`() = runTest {
        val visitId = visits.addVisit(Visit(placeId = 5, date = today, rating = 3, photoUri = "file:///photos/a.jpg")).getOrThrow()
        collectState()

        viewModel.onDeleteVisit(Visit(id = visitId, placeId = 5, date = today, rating = 3, photoUri = "file:///photos/a.jpg"))

        assertTrue(visits.observeVisits().first().isEmpty())
        assertEquals(listOf("file:///photos/a.jpg"), photos.deleted)
    }

    @Test
    fun `deleting the place sends PlaceDeleted and does not report not found`() = runTest {
        collectState()
        loadedState()
        viewModel.eventFlow.test {
            viewModel.onDeletePlace()
            assertEquals(PlaceDetailEvent.PlaceDeleted, awaitItem())
        }
        assertTrue(places.observePlaces().first().isEmpty())
        assertFalse(viewModel.uiState.value.notFound)
    }

    @Test
    fun `missing place is reported as not found`() = runTest {
        places.deletePlace(5)
        collectState()
        assertTrue(loadedState().notFound)
    }
}
