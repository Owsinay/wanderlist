package com.wanderlist.ui.places

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wanderlist.MainDispatcherRule
import com.wanderlist.data.fake.FakeCategoryRepository
import com.wanderlist.data.fake.FakeLocationProvider
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.TestSettingsRepository
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Priority
import com.wanderlist.domain.usecase.DeletePlaceUseCase
import com.wanderlist.domain.usecase.GetPlacesSortedByDistanceUseCase
import com.wanderlist.ui.components.StatusFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class PlacesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val krakow = Place(
        id = 1, name = "Sukiennice", location = GeoPoint(50.0617, 19.9373), address = "Rynek Główny, Kraków",
        categoryId = 1, priority = Priority.LOW, createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    private val gdansk = Place(
        id = 2, name = "Żuraw", location = GeoPoint(54.3498, 18.6566), address = "Gdańsk",
        categoryId = 1, status = PlaceStatus.VISITED, priority = Priority.HIGH, createdAt = Instant.parse("2026-03-01T00:00:00Z"),
    )
    private val tatry = Place(
        id = 3, name = "Morskie Oko", location = GeoPoint(49.2013, 20.0711), address = "Tatry",
        categoryId = 2, createdAt = Instant.parse("2026-02-01T00:00:00Z"),
    )

    private val places = FakePlaceRepository(initial = listOf(krakow, gdansk, tatry))
    private val location = FakeLocationProvider(Result.success(GeoPoint(50.06, 19.94))) // Kraków

    private fun createViewModel(savedState: SavedStateHandle = SavedStateHandle()) = PlacesViewModel(
        getPlacesSortedByDistance = GetPlacesSortedByDistanceUseCase(places),
        categoryRepository = FakeCategoryRepository(),
        settingsRepository = TestSettingsRepository(),
        locationProvider = location,
        deletePlace = DeletePlaceUseCase(places),
        savedStateHandle = savedState,
    )

    private val viewModel by lazy { createViewModel() }

    private fun TestScope.collectState() {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
    }

    private suspend fun names() = viewModel.uiState.first { !it.isLoading }.items.map { it.place.name }

    @Test
    fun `places are sorted by distance when location is known`() = runTest {
        collectState()
        assertEquals(listOf("Sukiennice", "Morskie Oko", "Żuraw"), names())
        assertTrue(viewModel.uiState.value.hasLocation)
    }

    @Test
    fun `without location permission list falls back to newest first`() = runTest {
        location.result = Result.failure(AppError.Permission("location"))
        collectState()

        assertEquals(listOf("Żuraw", "Morskie Oko", "Sukiennice"), names())
        assertFalse(viewModel.uiState.value.hasLocation)
        assertNull(viewModel.uiState.value.items.first().distanceMeters)
    }

    @Test
    fun `search matches name and address ignoring case`() = runTest {
        collectState()
        viewModel.onQueryChange("gdańsk")
        assertEquals(listOf("Żuraw"), names())

        viewModel.onQueryChange("MORSKIE")
        assertEquals(listOf("Morskie Oko"), names())
    }

    @Test
    fun `status and category filters combine`() = runTest {
        collectState()
        viewModel.onStatusChange(StatusFilter.WANT_TO_VISIT)
        viewModel.onCategoryChange(1)

        assertEquals(listOf("Sukiennice"), names())
    }

    @Test
    fun `sort by name and by priority`() = runTest {
        collectState()
        viewModel.onSortChange(PlaceSort.NAME)
        assertEquals(listOf("Morskie Oko", "Sukiennice", "Żuraw"), names())

        viewModel.onSortChange(PlaceSort.PRIORITY)
        assertEquals(listOf("Żuraw", "Morskie Oko", "Sukiennice"), names())
    }

    @Test
    fun `filters survive recreation`() = runTest {
        val savedState = SavedStateHandle()
        createViewModel(savedState).apply {
            onQueryChange("tatry")
            onSortChange(PlaceSort.NAME)
        }

        val filters = createViewModel(savedState).uiState.first { !it.isLoading }.filters
        assertEquals("tatry", filters.query)
        assertEquals(PlaceSort.NAME, filters.sort)
    }

    @Test
    fun `delete hides place immediately and undo brings it back`() = runTest {
        collectState()
        viewModel.eventFlow.test {
            viewModel.onDeleteRequest(krakow)
            assertEquals(PlacesEvent.PlaceDeleted(krakow), awaitItem())
        }
        assertFalse("Sukiennice" in names())

        viewModel.undoDelete(krakow.id)
        assertTrue("Sukiennice" in names())
        assertEquals(3, places.observePlaces().first().size)
    }

    @Test
    fun `confirmed delete removes place from repository`() = runTest {
        collectState()
        viewModel.onDeleteRequest(krakow)
        viewModel.confirmDelete(krakow.id)

        assertEquals(listOf(2L, 3L), places.observePlaces().first().map { it.id })
        assertFalse("Sukiennice" in names())
    }

    @Test
    fun `confirm after undo does not delete`() = runTest {
        collectState()
        viewModel.onDeleteRequest(krakow)
        viewModel.undoDelete(krakow.id)
        viewModel.confirmDelete(krakow.id)

        assertEquals(3, places.observePlaces().first().size)
    }
}
