package com.wanderlist.ui.places

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlist.data.common.toAppError
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.AppSettings
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.DistanceUnit
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceWithDistance
import com.wanderlist.domain.repository.CategoryRepository
import com.wanderlist.domain.repository.LocationProvider
import com.wanderlist.domain.repository.SettingsRepository
import com.wanderlist.domain.usecase.DeletePlaceUseCase
import com.wanderlist.domain.usecase.GetPlacesSortedByDistanceUseCase
import com.wanderlist.ui.components.StatusFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlaceSort { DISTANCE, NEWEST, NAME, PRIORITY }

data class PlaceListItem(
    val place: Place,
    val category: Category?,
    val distanceMeters: Double?,
)

data class PlacesFilters(
    val query: String = "",
    val status: StatusFilter = StatusFilter.ALL,
    val categoryId: Long? = null,
    val sort: PlaceSort = PlaceSort.DISTANCE,
)

data class PlacesUiState(
    val items: List<PlaceListItem> = emptyList(),
    val filters: PlacesFilters = PlacesFilters(),
    val categories: List<Category> = emptyList(),
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    /** false, gdy lokalizacja jest nieznana – sortowanie po odległości działa wtedy jak „najnowsze”. */
    val hasLocation: Boolean = false,
    val isLoading: Boolean = true,
    /** Czy są jakiekolwiek miejsca (odróżnia pustą bazę od braku wyników wyszukiwania). */
    val hasAnyPlaces: Boolean = false,
)

sealed interface PlacesEvent {
    data class PlaceDeleted(val place: Place) : PlacesEvent
    data class ShowError(val error: AppError) : PlacesEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlacesViewModel @Inject constructor(
    getPlacesSortedByDistance: GetPlacesSortedByDistanceUseCase,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val deletePlace: DeletePlaceUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val status = savedStateHandle.getStateFlow(KEY_STATUS, StatusFilter.ALL)
    private val categoryId = savedStateHandle.getStateFlow<Long?>(KEY_CATEGORY, null)
    private val sort = savedStateHandle.getStateFlow(KEY_SORT, PlaceSort.DISTANCE)

    private val origin = MutableStateFlow<GeoPoint?>(null)

    /** Miejsca usunięte gestem, ukryte do czasu potwierdzenia lub cofnięcia. */
    private val pendingDeletion = MutableStateFlow<Set<Long>>(emptySet())

    private val events = Channel<PlacesEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlacesEvent> = events.receiveAsFlow()

    private val filters = combine(query, status, categoryId, sort, ::PlacesFilters)

    private val places: Flow<List<PlaceWithDistance>?> = origin
        .flatMapLatest { getPlacesSortedByDistance(it) }
        .map<List<PlaceWithDistance>, List<PlaceWithDistance>?> { it }
        .onStart { emit(null) }
        .catch { error ->
            events.send(PlacesEvent.ShowError(error.toAppError()))
            emit(emptyList())
        }

    private val categories = categoryRepository.observeCategories().catch { emit(emptyList()) }

    private val settings = settingsRepository.settings.catch { emit(AppSettings()) }

    val uiState: StateFlow<PlacesUiState> =
        combine(places, filters, categories, settings, pendingDeletion) { all, filters, categories, settings, pending ->
            val visible = all.orEmpty().filterNot { it.place.id in pending }
            PlacesUiState(
                items = visible
                    .filter { filters.matches(it.place) }
                    .sortedWith(filters.sort.comparator())
                    .map { PlaceListItem(it.place, categories.find { c -> c.id == it.place.categoryId }, it.distanceMeters) },
                filters = filters,
                categories = categories,
                distanceUnit = settings.distanceUnit,
                hasLocation = visible.any { it.distanceMeters != null },
                isLoading = all == null,
                hasAnyPlaces = visible.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacesUiState())

    init {
        refreshLocation()
    }

    /** Pobiera lokalizację do sortowania; bez uprawnienia lista po prostu nie pokazuje odległości. */
    fun refreshLocation() {
        viewModelScope.launch {
            locationProvider.getCurrentLocation().onSuccess { origin.value = it }
        }
    }

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun onStatusChange(value: StatusFilter) {
        savedStateHandle[KEY_STATUS] = value
    }

    fun onCategoryChange(value: Long?) {
        savedStateHandle[KEY_CATEGORY] = value
    }

    fun onSortChange(value: PlaceSort) {
        savedStateHandle[KEY_SORT] = value
    }

    /** Ukrywa miejsce i proponuje cofnięcie; faktyczne usunięcie w [confirmDelete]. */
    fun onDeleteRequest(place: Place) {
        pendingDeletion.update { it + place.id }
        viewModelScope.launch { events.send(PlacesEvent.PlaceDeleted(place)) }
    }

    fun undoDelete(placeId: Long) {
        pendingDeletion.update { it - placeId }
    }

    fun confirmDelete(placeId: Long) {
        if (placeId !in pendingDeletion.value) return
        viewModelScope.launch {
            deletePlace(placeId).onFailure { error ->
                events.send(PlacesEvent.ShowError(error.toAppError()))
            }
            pendingDeletion.update { it - placeId }
        }
    }

    private fun PlacesFilters.matches(place: Place): Boolean {
        val text = query.trim()
        val matchesQuery = text.isEmpty() ||
            place.name.contains(text, ignoreCase = true) ||
            place.address.orEmpty().contains(text, ignoreCase = true)
        return matchesQuery && status.matches(place) && (categoryId == null || place.categoryId == categoryId)
    }

    private fun PlaceSort.comparator(): Comparator<PlaceWithDistance> = when (this) {
        // Lista z use case'u jest już posortowana po odległości (lub od najnowszych bez lokalizacji).
        PlaceSort.DISTANCE -> Comparator { _, _ -> 0 }
        PlaceSort.NEWEST -> compareByDescending { it.place.createdAt }
        PlaceSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.place.name }
        PlaceSort.PRIORITY -> compareByDescending<PlaceWithDistance> { it.place.priority }.thenBy { it.place.name }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_STATUS = "status"
        const val KEY_CATEGORY = "categoryId"
        const val KEY_SORT = "sort"
    }
}
