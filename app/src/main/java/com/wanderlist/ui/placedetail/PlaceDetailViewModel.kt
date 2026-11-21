package com.wanderlist.ui.placedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlist.data.common.toAppError
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.AppSettings
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.Visit
import com.wanderlist.domain.model.VisitInputError
import com.wanderlist.domain.model.Weather
import com.wanderlist.domain.repository.CategoryRepository
import com.wanderlist.domain.repository.PhotoStorage
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.repository.SettingsRepository
import com.wanderlist.domain.repository.VisitRepository
import com.wanderlist.domain.repository.WeatherRepository
import com.wanderlist.domain.usecase.DeletePlaceUseCase
import com.wanderlist.domain.usecase.DeleteVisitUseCase
import com.wanderlist.domain.usecase.MarkPlaceVisitedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Success(val weather: Weather) : WeatherState
    data class Error(val error: AppError) : WeatherState
}

data class VisitDialogState(
    val isOpen: Boolean = false,
    val errors: List<VisitInputError> = emptyList(),
    val isSaving: Boolean = false,
)

data class PlaceDetailUiState(
    val place: Place? = null,
    val category: Category? = null,
    val visits: List<Visit> = emptyList(),
    val weather: WeatherState = WeatherState.Loading,
    val visitDialog: VisitDialogState = VisitDialogState(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
)

sealed interface PlaceDetailEvent {
    data object PlaceDeleted : PlaceDetailEvent
    data object VisitSaved : PlaceDetailEvent
    data class ShowError(val error: AppError) : PlaceDetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    placeRepository: PlaceRepository,
    visitRepository: VisitRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    private val weatherRepository: WeatherRepository,
    private val photoStorage: PhotoStorage,
    private val markPlaceVisited: MarkPlaceVisitedUseCase,
    private val deleteVisit: DeleteVisitUseCase,
    private val deletePlace: DeletePlaceUseCase,
) : ViewModel() {
    val placeId: Long = savedStateHandle.get<Long>("placeId") ?: 0L

    private val events = Channel<PlaceDetailEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlaceDetailEvent> = events.receiveAsFlow()

    private val visitDialog = MutableStateFlow(VisitDialogState())
    private val weatherRetry = MutableStateFlow(0)
    private var isDeleting = false

    private val place: Flow<Place?> = placeRepository.observePlace(placeId)
        .catch { error ->
            events.send(PlaceDetailEvent.ShowError(error.toAppError()))
            emit(null)
        }

    /** Pogoda pobierana raz na lokalizację (i ponownie po „Spróbuj ponownie”). */
    private val weather: Flow<WeatherState> =
        combine(place.filterNotNull().map { it.location }.distinctUntilChanged(), weatherRetry) { location, _ -> location }
            .flatMapLatest { location ->
                flow {
                    emit(WeatherState.Loading)
                    weatherRepository.getCurrentWeather(location).fold(
                        onSuccess = { emit(WeatherState.Success(it)) },
                        onFailure = { emit(WeatherState.Error(it.toAppError())) },
                    )
                }
            }
            .onStart { emit(WeatherState.Loading) }

    private val related = combine(
        visitRepository.observeVisitsForPlace(placeId).catch { emit(emptyList()) },
        categoryRepository.observeCategories().catch { emit(emptyList()) },
        settingsRepository.settings.catch { emit(AppSettings()) },
    ) { visits, categories, settings -> Triple(visits, categories, settings) }

    val uiState: StateFlow<PlaceDetailUiState> =
        combine(place.map { Loaded(it) }.onStart<Loaded?> { emit(null) }, related, weather, visitDialog) { loaded, related, weather, dialog ->
            val (visits, categories, settings) = related
            val place = loaded?.place
            PlaceDetailUiState(
                place = place,
                category = categories.find { it.id == place?.categoryId },
                visits = visits,
                weather = weather,
                visitDialog = dialog,
                settings = settings,
                isLoading = loaded == null,
                notFound = loaded != null && place == null && !isDeleting,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceDetailUiState())

    fun retryWeather() = weatherRetry.update { it + 1 }

    fun openVisitDialog() = visitDialog.update { VisitDialogState(isOpen = true) }

    fun closeVisitDialog() = visitDialog.update { VisitDialogState() }

    fun saveVisit(date: LocalDate, rating: Int, memo: String, pickedPhotoUri: String?) {
        if (visitDialog.value.isSaving) return
        val errors = markPlaceVisited.validate(date, rating, memo)
        if (errors.isNotEmpty()) {
            visitDialog.update { it.copy(errors = errors) }
            return
        }
        visitDialog.update { it.copy(errors = emptyList(), isSaving = true) }
        viewModelScope.launch {
            // Błąd kopiowania zdjęcia nie blokuje zapisu odwiedzin – zapisujemy je bez zdjęcia.
            val photoUri = pickedPhotoUri?.let { uri ->
                photoStorage.importPhoto(uri).onFailure { events.send(PlaceDetailEvent.ShowError(it.toAppError())) }.getOrNull()
            }
            markPlaceVisited(placeId, date, rating, memo, photoUri)
                .onSuccess {
                    visitDialog.value = VisitDialogState()
                    events.send(PlaceDetailEvent.VisitSaved)
                }
                .onFailure { error ->
                    photoUri?.let { photoStorage.deletePhoto(it) }
                    visitDialog.update { it.copy(isSaving = false) }
                    events.send(PlaceDetailEvent.ShowError(error.toAppError()))
                }
        }
    }

    fun onDeleteVisit(visit: Visit) {
        viewModelScope.launch {
            deleteVisit(placeId, visit.id)
                .onSuccess { visit.photoUri?.let { photoStorage.deletePhoto(it) } }
                .onFailure { events.send(PlaceDetailEvent.ShowError(it.toAppError())) }
        }
    }

    fun onDeletePlace() {
        val photos = uiState.value.visits.mapNotNull { it.photoUri }
        isDeleting = true
        viewModelScope.launch {
            deletePlace(placeId)
                .onSuccess {
                    photos.forEach { photoStorage.deletePhoto(it) }
                    events.send(PlaceDetailEvent.PlaceDeleted)
                }
                .onFailure {
                    isDeleting = false
                    events.send(PlaceDetailEvent.ShowError(it.toAppError()))
                }
        }
    }

    /** Odróżnia „jeszcze nie wczytano” (null) od „wczytano, ale brak miejsca” (Loaded(null)). */
    private data class Loaded(val place: Place?)
}
