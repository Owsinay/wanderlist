package com.wanderlist.ui.placedetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanderlist.data.fake.FakeCategoryRepository
import com.wanderlist.data.fake.FakePhotoStorage
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.FakeVisitRepository
import com.wanderlist.data.fake.TestSettingsRepository
import com.wanderlist.data.fake.TestWeatherRepository
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.usecase.DeletePlaceUseCase
import com.wanderlist.domain.usecase.DeleteVisitUseCase
import com.wanderlist.domain.usecase.MarkPlaceVisitedUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaceDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val places = FakePlaceRepository(
        initial = listOf(Place(id = 3, name = "Zamek w Malborku", location = GeoPoint(54.0398, 19.0283), address = "Malbork")),
    )
    private val visits = FakeVisitRepository()
    private val viewModel = PlaceDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("placeId" to 3L)),
        placeRepository = places,
        visitRepository = visits,
        categoryRepository = FakeCategoryRepository(),
        settingsRepository = TestSettingsRepository(),
        weatherRepository = TestWeatherRepository(),
        photoStorage = FakePhotoStorage(),
        markPlaceVisited = MarkPlaceVisitedUseCase(places, visits, Clock.systemDefaultZone()),
        deleteVisit = DeleteVisitUseCase(places, visits),
        deletePlace = DeletePlaceUseCase(places),
    )

    @Composable
    private fun Screen() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        if (uiState.visitDialog.isOpen) {
            VisitDialog(state = uiState.visitDialog, onDismiss = viewModel::closeVisitDialog, onSave = viewModel::saveVisit)
        }
        PlaceDetailContent(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onEdit = {},
            onDelete = {},
            onNavigate = {},
            onAddVisit = viewModel::openVisitDialog,
            onDeleteVisit = viewModel::onDeleteVisit,
            onRetryWeather = viewModel::retryWeather,
            miniMap = {},
        )
    }

    @Test
    fun detailsShowPlaceAndWeather() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithText("Malbork").assertIsDisplayed()
        composeRule.onNodeWithText("21°C, bezchmurnie").assertIsDisplayed()
        composeRule.onNodeWithText("Odwiedziny (0)").assertIsDisplayed()
    }

    @Test
    fun markingAsVisitedRequiresRatingAndUpdatesStatus() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithTag(PlaceDetailTags.MARK_VISITED).performClick()
        composeRule.onNodeWithTag(PlaceDetailTags.SAVE_VISIT).performClick()
        composeRule.onNodeWithText("Wybierz ocenę od 1 do 5").assertIsDisplayed()

        composeRule.onNodeWithTag("${PlaceDetailTags.STAR_PREFIX}4").performClick()
        composeRule.onNodeWithTag(PlaceDetailTags.SAVE_VISIT).performClick()

        composeRule.onNodeWithText("Odwiedziny (1)").assertIsDisplayed()
        val place = runBlocking { places.observePlace(3).first() }
        assertEquals(PlaceStatus.VISITED, place?.status)
        assertEquals(4, runBlocking { visits.observeVisits().first() }.single().rating)
    }

    @Test
    fun visitDialogKeepsInputAfterRecreation() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            VisitDialog(state = VisitDialogState(isOpen = true), onDismiss = {}, onSave = { _, _, _, _ -> })
        }

        composeRule.onNodeWithText("Wspomnienie").performTextInput("Piękne krużganki")
        composeRule.onNodeWithTag("${PlaceDetailTags.STAR_PREFIX}5").performClick()

        // Symuluje zniszczenie i odtworzenie Activity (np. obrót ekranu).
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Piękne krużganki").assertIsDisplayed()
        composeRule.onNodeWithText("16/1000").assertIsDisplayed()
    }
}
