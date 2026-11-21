package com.wanderlist.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlist.data.common.toAppError
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.repository.LocationProvider
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.ui.components.StatusFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val places: List<Place> = emptyList(),
    val filter: StatusFilter = StatusFilter.ALL,
    val selectedPlace: Place? = null,
    val userLocation: GeoPoint? = null,
    val isLocating: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface MapEvent {
    data class MoveCamera(val target: GeoPoint) : MapEvent
    data class ShowError(val error: AppError) : MapEvent
}

@HiltViewModel
class MapViewModel @Inject constructor(
    placeRepository: PlaceRepository,
    private val locationProvider: LocationProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Filtr i zaznaczenie w SavedStateHandle – przetrwają obrót ekranu i zabicie procesu.
    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, StatusFilter.ALL)
    private val selectedId = savedStateHandle.getStateFlow<Long?>(KEY_SELECTED, null)
    private val location = MutableStateFlow(LocationState())

    private val events = Channel<MapEvent>(Channel.BUFFERED)
    val eventFlow: Flow<MapEvent> = events.receiveAsFlow()

    private val places: Flow<List<Place>?> = placeRepository.observePlaces()
        .map<List<Place>, List<Place>?> { it }
        .onStart { emit(null) }
        .catch { error ->
            events.send(MapEvent.ShowError(error.toAppError()))
            emit(emptyList())
        }

    val uiState: StateFlow<MapUiState> =
        combine(places, filter, selectedId, location) { all, filter, selectedId, location ->
            val list = all.orEmpty()
            MapUiState(
                places = list.filter(filter::matches),
                filter = filter,
                selectedPlace = list.find { it.id == selectedId },
                userLocation = location.point,
                isLocating = location.isLocating,
                isLoading = all == null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    fun onFilterChange(newFilter: StatusFilter) {
        savedStateHandle[KEY_FILTER] = newFilter
    }

    fun onPlaceSelected(placeId: Long?) {
        savedStateHandle[KEY_SELECTED] = placeId
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        _hasLocationPermission.value = granted
    }

    /** Pobiera lokalizację i przesuwa na nią kamerę. Wywoływane po przyznaniu uprawnienia. */
    fun locateMe() {
        if (location.value.isLocating) return
        location.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            locationProvider.getCurrentLocation()
                .onSuccess { point ->
                    location.value = LocationState(point = point)
                    events.send(MapEvent.MoveCamera(point))
                }
                .onFailure { error ->
                    location.update { it.copy(isLocating = false) }
                    events.send(MapEvent.ShowError(error.toAppError()))
                }
        }
    }

    private data class LocationState(val point: GeoPoint? = null, val isLocating: Boolean = false)

    private companion object {
        const val KEY_FILTER = "filter"
        const val KEY_SELECTED = "selectedPlaceId"
    }
}
