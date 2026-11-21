package com.wanderlist.ui.map

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wanderlist.MainDispatcherRule
import com.wanderlist.data.fake.FakeLocationProvider
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.SampleData
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.PlaceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val places = FakePlaceRepository()
    private val location = FakeLocationProvider()
    private val savedState = SavedStateHandle()
    private val viewModel = MapViewModel(places, location, savedState)

    private suspend fun currentState(): MapUiState = viewModel.uiState.first { !it.isLoading }

    @Test
    fun `all places are shown by default`() = runTest {
        viewModel.uiState.test {
            awaitItem() // stan początkowy
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(SampleData.places.size, state.places.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter shows only places with the chosen status`() = runTest {
        backgroundCollect()
        viewModel.onFilterChange(MapFilter.VISITED)

        val state = currentState()
        assertEquals(MapFilter.VISITED, state.filter)
        assertTrue(state.places.isNotEmpty())
        assertTrue(state.places.all { it.status == PlaceStatus.VISITED })
    }

    @Test
    fun `filter and selection survive recreation through SavedStateHandle`() = runTest {
        viewModel.onFilterChange(MapFilter.WANT_TO_VISIT)
        viewModel.onPlaceSelected(2)

        val recreated = MapViewModel(places, location, savedState)
        val state = recreated.uiState.first { !it.isLoading }
        assertEquals(MapFilter.WANT_TO_VISIT, state.filter)
        assertEquals(2L, state.selectedPlace?.id)
    }

    @Test
    fun `deleted selected place is cleared from state`() = runTest {
        backgroundCollect()
        viewModel.onPlaceSelected(1)
        places.deletePlace(1)

        assertNull(currentState().selectedPlace)
    }

    @Test
    fun `locate me moves camera to current location`() = runTest {
        backgroundCollect()
        val point = GeoPoint(52.23, 21.01)
        location.result = Result.success(point)

        viewModel.eventFlow.test {
            viewModel.locateMe()
            assertEquals(MapEvent.MoveCamera(point), awaitItem())
        }
        assertEquals(point, currentState().userLocation)
    }

    @Test
    fun `missing permission is reported as error event`() = runTest {
        location.result = Result.failure(AppError.Permission("location"))

        viewModel.eventFlow.test {
            viewModel.locateMe()
            val event = awaitItem()
            assertTrue(event is MapEvent.ShowError && event.error is AppError.Permission)
        }
    }

    private fun kotlinx.coroutines.test.TestScope.backgroundCollect() {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
    }
}
