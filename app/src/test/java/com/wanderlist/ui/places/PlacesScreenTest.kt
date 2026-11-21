package com.wanderlist.ui.places

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.ui.components.StatusFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlacesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val wawel = Place(id = 1, name = "Wawel", location = GeoPoint(50.054, 19.935), address = "Kraków", status = PlaceStatus.VISITED)
    private val oko = Place(id = 2, name = "Morskie Oko", location = GeoPoint(49.2, 20.07), address = "Tatry")

    private fun setContent(
        state: PlacesUiState,
        onPlaceClick: (Long) -> Unit = {},
        onQueryChange: (String) -> Unit = {},
        onStatusChange: (StatusFilter) -> Unit = {},
        onAddPlace: () -> Unit = {},
    ) {
        composeRule.setContent {
            PlacesContent(
                uiState = state,
                snackbarHostState = SnackbarHostState(),
                onQueryChange = onQueryChange,
                onStatusChange = onStatusChange,
                onCategoryChange = {},
                onSortChange = {},
                onDelete = {},
                onAddPlace = onAddPlace,
                onPlaceClick = onPlaceClick,
                onSettingsClick = {},
            )
        }
    }

    private val loaded = PlacesUiState(
        items = listOf(PlaceListItem(wawel, null, 1_200.0), PlaceListItem(oko, null, 95_000.0)),
        isLoading = false,
        hasAnyPlaces = true,
        hasLocation = true,
    )

    @Test
    fun listShowsPlacesWithDistanceAndStatus() {
        setContent(loaded)

        composeRule.onNodeWithText("Wawel").assertIsDisplayed()
        composeRule.onNodeWithText("1.2 km", substring = true, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Tatry").assertIsDisplayed()
    }

    @Test
    fun clickingPlaceOpensItsDetails() {
        var opened: Long? = null
        setContent(loaded, onPlaceClick = { opened = it })

        composeRule.onNodeWithText("Morskie Oko").performClick()

        assertEquals(2L, opened)
    }

    @Test
    fun searchAndFilterCallbacksReceiveUserInput() {
        var query = ""
        var status: StatusFilter? = null
        setContent(loaded, onQueryChange = { query = it }, onStatusChange = { status = it })

        composeRule.onNodeWithText("Szukaj po nazwie lub adresie").performTextInput("oko")
        composeRule.onAllNodesWithText("Odwiedzone").onFirst().performClick() // pierwszy to chip filtra

        assertEquals("oko", query)
        assertEquals(StatusFilter.VISITED, status)
    }

    @Test
    fun emptyDatabaseShowsCallToAction() {
        var added = false
        setContent(PlacesUiState(isLoading = false, hasAnyPlaces = false), onAddPlace = { added = true })

        composeRule.onNodeWithText("Nie masz jeszcze żadnych miejsc", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dodaj miejsce").assertIsDisplayed()
        composeRule.onNodeWithText("Dodaj miejsce").performClick()
        assertEquals(true, added)
    }

    @Test
    fun noResultsMessageWhenFiltersMatchNothing() {
        setContent(PlacesUiState(isLoading = false, hasAnyPlaces = true, items = emptyList()))

        composeRule.onNodeWithText("Brak miejsc pasujących do wyszukiwania i filtrów.").assertIsDisplayed()
    }
}
