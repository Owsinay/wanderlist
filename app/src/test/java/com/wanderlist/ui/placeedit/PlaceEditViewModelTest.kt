package com.wanderlist.ui.placeedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wanderlist.MainDispatcherRule
import com.wanderlist.data.fake.FakeCategoryRepository
import com.wanderlist.data.fake.FakeLocationProvider
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.TestGeocodingRepository
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceInputError
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Priority
import com.wanderlist.domain.usecase.SavePlaceUseCase
import com.wanderlist.domain.usecase.ValidatePlaceInputUseCase
import com.wanderlist.testClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaceEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val existing = Place(
        id = 7, name = "Wawel", location = GeoPoint(50.054, 19.935), address = "Wawel 5, Kraków",
        categoryId = 1, status = PlaceStatus.VISITED, priority = Priority.HIGH, note = "Dziedziniec",
    )
    private val places = FakePlaceRepository(initial = listOf(existing))
    private val geocoding = TestGeocodingRepository()
    private val location = FakeLocationProvider()

    private fun createViewModel(args: Map<String, Any?> = emptyMap()): PlaceEditViewModel {
        val validate = ValidatePlaceInputUseCase()
        return PlaceEditViewModel(
            savedStateHandle = SavedStateHandle(args),
            placeRepository = places,
            categoryRepository = FakeCategoryRepository(),
            geocodingRepository = geocoding,
            locationProvider = location,
            savePlace = SavePlaceUseCase(places, validate, testClock),
            validate = validate,
        )
    }

    @Test
    fun `new place from map long press has location and reverse geocoded address`() = runTest {
        geocoding.reverseResult = Result.success("Rynek Główny 1, Kraków")
        val viewModel = createViewModel(mapOf("latitude" to "50.0617", "longitude" to "19.9373"))

        val state = viewModel.uiState.value
        assertTrue(state.isNew)
        assertEquals(GeoPoint(50.0617, 19.9373), state.form.location)
        assertEquals("Rynek Główny 1, Kraków", state.form.address)
    }

    @Test
    fun `editing loads the existing place without marking changes`() = runTest {
        val viewModel = createViewModel(mapOf("placeId" to 7L))

        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isNew)
        assertEquals("Wawel", state.form.name)
        assertEquals(Priority.HIGH, state.form.priority)
        assertFalse(state.hasChanges)

        viewModel.onNameChange("Zamek Królewski")
        assertTrue(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `errors are hidden until first save and then update live`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNameChange("")
        assertTrue(viewModel.uiState.value.errors.isEmpty())

        viewModel.onSave()
        assertEquals(
            listOf(PlaceInputError.NAME_EMPTY, PlaceInputError.LOCATION_MISSING),
            viewModel.uiState.value.errors,
        )

        viewModel.onNameChange("Wieliczka")
        assertEquals(listOf(PlaceInputError.LOCATION_MISSING), viewModel.uiState.value.errors)
        assertEquals(1, places.observePlaces().first().size)
    }

    @Test
    fun `valid new place is saved and Saved event is sent`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.onNameChange("Kopalnia Soli")
            viewModel.onLocationPicked(GeoPoint(49.98, 20.05))
            viewModel.onPriorityChange(Priority.LOW)
            viewModel.onSave()

            val event = awaitItem()
            assertTrue(event is PlaceEditEvent.Saved)
            val saved = places.observePlace((event as PlaceEditEvent.Saved).placeId).first()!!
            assertEquals("Kopalnia Soli", saved.name)
            assertEquals(Priority.LOW, saved.priority)
        }
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `edit keeps visited status`() = runTest {
        val viewModel = createViewModel(mapOf("placeId" to 7L))
        viewModel.uiState.first { !it.isLoading }

        viewModel.onNoteChange("Krużganki")
        viewModel.onSave()

        val saved = places.observePlace(7).first()!!
        assertEquals("Krużganki", saved.note)
        assertEquals(PlaceStatus.VISITED, saved.status)
    }

    @Test
    fun `selecting a search result fills location address and empty name`() = runTest {
        val result = GeocodingResult("Morskie Oko, Tatrzański Park Narodowy", GeoPoint(49.2013, 20.0711))
        geocoding.searchResult = Result.success(listOf(result))
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("morskie")
        viewModel.onSearch()
        assertEquals(listOf(result), viewModel.uiState.value.searchResults)

        viewModel.onSearchResultSelected(result)
        val form = viewModel.uiState.value.form
        assertEquals("Morskie Oko", form.name)
        assertEquals(result.location, form.location)
        assertEquals(result.label, form.address)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `too short query is not sent and network error is reported`() = runTest {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChange("ab")
        viewModel.onSearch()
        assertTrue(geocoding.searchedQueries.isEmpty())

        geocoding.searchResult = Result.failure(AppError.Network())
        viewModel.eventFlow.test {
            viewModel.onSearchQueryChange("Gdańsk")
            viewModel.onSearch()
            val event = awaitItem()
            assertTrue(event is PlaceEditEvent.ShowError && event.error is AppError.Network)
        }
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `use my location sets the point or reports missing permission`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUseMyLocation()
        assertEquals(GeoPoint(50.0617, 19.9373), viewModel.uiState.value.form.location)

        location.result = Result.failure(AppError.Permission("location"))
        viewModel.eventFlow.test {
            viewModel.onUseMyLocation()
            val event = awaitItem()
            assertTrue(event is PlaceEditEvent.ShowError && event.error is AppError.Permission)
        }
    }

    @Test
    fun `missing place shows NotFound`() = runTest {
        val viewModel = createViewModel(mapOf("placeId" to 99L))
        viewModel.eventFlow.test {
            val event = awaitItem()
            assertTrue(event is PlaceEditEvent.ShowError && event.error is AppError.NotFound)
        }
    }
}
