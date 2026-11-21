package com.wanderlist.ui.placeedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlist.data.common.toAppError
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult
import com.wanderlist.domain.model.PlaceInput
import com.wanderlist.domain.model.PlaceInputError
import com.wanderlist.domain.model.Priority
import com.wanderlist.domain.repository.CategoryRepository
import com.wanderlist.domain.repository.GeocodingRepository
import com.wanderlist.domain.repository.LocationProvider
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.usecase.SavePlaceUseCase
import com.wanderlist.domain.usecase.ValidatePlaceInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceForm(
    val name: String = "",
    val location: GeoPoint? = null,
    val address: String = "",
    val categoryId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val note: String = "",
) {
    fun toInput() = PlaceInput(name, location, address, categoryId, priority, note)
}

data class PlaceEditUiState(
    val isNew: Boolean = true,
    val form: PlaceForm = PlaceForm(),
    /** Błędy pokazywane dopiero po pierwszej próbie zapisu. */
    val errors: List<PlaceInputError> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLocating: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
)

sealed interface PlaceEditEvent {
    data class Saved(val placeId: Long) : PlaceEditEvent
    data class ShowError(val error: AppError) : PlaceEditEvent
    data object NoSearchResults : PlaceEditEvent
}

@HiltViewModel
class PlaceEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    categoryRepository: CategoryRepository,
    private val geocodingRepository: GeocodingRepository,
    private val locationProvider: LocationProvider,
    private val savePlace: SavePlaceUseCase,
    private val validate: ValidatePlaceInputUseCase,
) : ViewModel() {
    // Argumenty trasy PlaceEditRoute (nawigacja zapisuje je w SavedStateHandle pod nazwami pól).
    private val placeId: Long = savedStateHandle.get<Long>("placeId") ?: 0L
    private val initialPoint: GeoPoint? = run {
        val lat = savedStateHandle.get<String>("latitude")?.toDoubleOrNull()
        val lng = savedStateHandle.get<String>("longitude")?.toDoubleOrNull()
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }

    private val _uiState = MutableStateFlow(PlaceEditUiState(isNew = placeId == 0L, isLoading = placeId != 0L))
    val uiState: StateFlow<PlaceEditUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val events = Channel<PlaceEditEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlaceEditEvent> = events.receiveAsFlow()

    private var savedForm = PlaceForm()
    private var showErrors = false
    private var searchJob: Job? = null

    init {
        if (placeId == 0L) {
            initialPoint?.let { point ->
                updateForm { it.copy(location = point) }
                fillAddressFor(point)
            }
            savedForm = PlaceForm(location = initialPoint)
            refreshChanges()
        } else {
            loadPlace()
        }
    }

    private fun loadPlace() {
        viewModelScope.launch {
            val place = runCatching { placeRepository.observePlace(placeId).first() }.getOrNull()
            if (place == null) {
                _uiState.update { it.copy(isLoading = false) }
                events.send(PlaceEditEvent.ShowError(AppError.NotFound()))
                return@launch
            }
            val form = PlaceForm(
                name = place.name,
                location = place.location,
                address = place.address.orEmpty(),
                categoryId = place.categoryId,
                priority = place.priority,
                note = place.note.orEmpty(),
            )
            savedForm = form
            _uiState.update { it.copy(form = form, isLoading = false, hasChanges = false) }
        }
    }

    fun onNameChange(value: String) = updateForm { it.copy(name = value) }
    fun onAddressChange(value: String) = updateForm { it.copy(address = value) }
    fun onCategoryChange(value: Long?) = updateForm { it.copy(categoryId = value) }
    fun onPriorityChange(value: Priority) = updateForm { it.copy(priority = value) }
    fun onNoteChange(value: String) = updateForm { it.copy(note = value) }

    /** Punkt wybrany na mapie; adres uzupełniany automatycznie, jeśli pole jest puste. */
    fun onLocationPicked(point: GeoPoint) {
        updateForm { it.copy(location = point) }
        fillAddressFor(point)
    }

    fun onUseMyLocation() {
        if (_uiState.value.isLocating) return
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            locationProvider.getCurrentLocation()
                .onSuccess { onLocationPicked(it) }
                .onFailure { events.send(PlaceEditEvent.ShowError(it.toAppError())) }
            _uiState.update { it.copy(isLocating = false) }
        }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value, searchResults = if (value.isBlank()) emptyList() else it.searchResults) }
    }

    fun onSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            geocodingRepository.search(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(searchResults = results) }
                    if (results.isEmpty()) events.send(PlaceEditEvent.NoSearchResults)
                }
                .onFailure { events.send(PlaceEditEvent.ShowError(it.toAppError())) }
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    fun onSearchResultSelected(result: GeocodingResult) {
        updateForm { form ->
            form.copy(
                location = result.location,
                address = result.label,
                // Nazwa podpowiadana z pierwszego członu adresu, jeśli użytkownik jeszcze jej nie wpisał.
                name = form.name.ifBlank { result.label.substringBefore(',').trim() },
            )
        }
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        showErrors = true
        val errors = validate(state.form.toInput())
        _uiState.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            savePlace(placeId, state.form.toInput())
                .onSuccess { id ->
                    savedForm = _uiState.value.form
                    _uiState.update { it.copy(isSaving = false, hasChanges = false) }
                    events.send(PlaceEditEvent.Saved(id))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    events.send(PlaceEditEvent.ShowError(error.toAppError()))
                }
        }
    }

    private fun fillAddressFor(point: GeoPoint) {
        if (_uiState.value.form.address.isNotBlank()) return
        viewModelScope.launch {
            geocodingRepository.reverse(point).onSuccess { address ->
                if (address != null && _uiState.value.form.address.isBlank() && _uiState.value.form.location == point) {
                    updateForm { it.copy(address = address) }
                }
            }
        }
    }

    private fun updateForm(transform: (PlaceForm) -> PlaceForm) {
        _uiState.update { state ->
            val form = transform(state.form)
            state.copy(
                form = form,
                errors = if (showErrors) validate(form.toInput()) else emptyList(),
                hasChanges = form != savedForm,
            )
        }
    }

    private fun refreshChanges() = _uiState.update { it.copy(hasChanges = it.form != savedForm) }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
    }
}
