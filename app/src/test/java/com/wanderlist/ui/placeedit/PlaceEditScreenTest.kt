package com.wanderlist.ui.placeedit

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanderlist.data.fake.FakeCategoryRepository
import com.wanderlist.data.fake.FakeLocationProvider
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.TestGeocodingRepository
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.usecase.SavePlaceUseCase
import com.wanderlist.domain.usecase.ValidatePlaceInputUseCase
import com.wanderlist.testClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaceEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val places = FakePlaceRepository(initial = emptyList())
    private val viewModel = ValidatePlaceInputUseCase().let { validate ->
        PlaceEditViewModel(
            savedStateHandle = SavedStateHandle(),
            placeRepository = places,
            categoryRepository = FakeCategoryRepository(),
            geocodingRepository = TestGeocodingRepository(),
            locationProvider = FakeLocationProvider(),
            savePlace = SavePlaceUseCase(places, validate, testClock),
            validate = validate,
        )
    }

    @Composable
    private fun Screen() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val categories by viewModel.categories.collectAsStateWithLifecycle()
        PlaceEditContent(
            uiState = uiState,
            categories = categories,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onNameChange = viewModel::onNameChange,
            onAddressChange = viewModel::onAddressChange,
            onCategoryChange = viewModel::onCategoryChange,
            onPriorityChange = viewModel::onPriorityChange,
            onNoteChange = viewModel::onNoteChange,
            onLocationPicked = viewModel::onLocationPicked,
            onUseMyLocation = viewModel::onUseMyLocation,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearch = viewModel::onSearch,
            onSearchResultSelected = viewModel::onSearchResultSelected,
            onSave = viewModel::onSave,
            locationPicker = {},
        )
    }

    @Test
    fun savingEmptyFormShowsFieldErrors() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithTag(PlaceEditTags.SAVE).performScrollTo().performClick()

        composeRule.onNodeWithText("Podaj nazwę miejsca").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Wybierz lokalizację na mapie").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun errorDisappearsAfterTypingName() {
        composeRule.setContent { Screen() }
        composeRule.onNodeWithTag(PlaceEditTags.SAVE).performScrollTo().performClick()

        composeRule.onNodeWithTag(PlaceEditTags.NAME).performScrollTo().performTextInput("Wieliczka")

        composeRule.onNodeWithText("Podaj nazwę miejsca").assertDoesNotExist()
        composeRule.onNodeWithText("9/100").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun validFormIsSaved() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithTag(PlaceEditTags.NAME).performTextInput("Kopalnia Soli")
        composeRule.runOnIdle { viewModel.onLocationPicked(GeoPoint(49.98, 20.05)) }
        composeRule.onNodeWithTag(PlaceEditTags.SAVE).performScrollTo().performClick()
        composeRule.waitForIdle()

        val saved = runBlocking { places.observePlaces().first() }
        assertEquals(listOf("Kopalnia Soli"), saved.map { it.name })
    }
}
