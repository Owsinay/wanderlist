package com.wanderlist.ui.placeedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.wanderlist.R
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult
import com.wanderlist.domain.model.PlaceInputError
import com.wanderlist.domain.model.Priority
import com.wanderlist.domain.usecase.ValidatePlaceInputUseCase
import com.wanderlist.ui.components.categoryIcon
import com.wanderlist.ui.components.rememberErrorMessage
import java.util.Locale

@Composable
fun PlaceEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = rememberErrorMessage()
    val noResults = stringResource(R.string.place_edit_no_results)
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PlaceEditEvent.Saved -> onBack()
                is PlaceEditEvent.ShowError -> snackbarHostState.showSnackbar(errorMessage(event.error))
                PlaceEditEvent.NoSearchResults -> snackbarHostState.showSnackbar(noResults)
            }
        }
    }

    val tryBack = { if (uiState.hasChanges) showDiscardDialog = true else onBack() }
    BackHandler(enabled = uiState.hasChanges) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.place_edit_discard_title)) },
            text = { Text(stringResource(R.string.place_edit_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.action_keep_editing)) }
            },
        )
    }

    PlaceEditContent(
        uiState = uiState,
        categories = categories,
        snackbarHostState = snackbarHostState,
        onBack = tryBack,
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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceEditContent(
    uiState: PlaceEditUiState,
    categories: List<Category>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onNoteChange: (String) -> Unit,
    onLocationPicked: (GeoPoint) -> Unit,
    onUseMyLocation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchResultSelected: (GeocodingResult) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = uiState.form
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.place_edit_title_new else R.string.place_edit_title_edit))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isSaving && !uiState.isLoading) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (uiState.isLoading || uiState.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())

            LocationPicker(
                location = form.location,
                onLocationPicked = onLocationPicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LocationRow(
                    location = form.location,
                    error = uiState.errors.firstOrNull { it.field == "location" },
                    isLocating = uiState.isLocating,
                    onUseMyLocation = onUseMyLocation,
                )
                AddressSearch(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    onQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onResultSelected = onSearchResultSelected,
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.place_edit_name)) },
                    isError = uiState.errors.any { it.field == "name" },
                    supportingText = {
                        FieldMessage(
                            error = uiState.errors.firstOrNull { it.field == "name" },
                            counter = "${form.name.length}/${ValidatePlaceInputUseCase.MAX_NAME_LENGTH}",
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PlaceEditTags.NAME),
                )
                OutlinedTextField(
                    value = form.address,
                    onValueChange = onAddressChange,
                    label = { Text(stringResource(R.string.place_edit_address)) },
                    isError = uiState.errors.any { it.field == "address" },
                    supportingText = { FieldMessage(error = uiState.errors.firstOrNull { it.field == "address" }) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryField(categories = categories, selectedId = form.categoryId, onCategoryChange = onCategoryChange)
                Text(stringResource(R.string.place_edit_priority), style = MaterialTheme.typography.labelLarge)
                PrioritySelector(selected = form.priority, onPriorityChange = onPriorityChange)
                OutlinedTextField(
                    value = form.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.place_edit_note)) },
                    isError = uiState.errors.any { it.field == "note" },
                    supportingText = {
                        FieldMessage(
                            error = uiState.errors.firstOrNull { it.field == "note" },
                            counter = "${form.note.length}/${ValidatePlaceInputUseCase.MAX_NOTE_LENGTH}",
                        )
                    },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PlaceEditTags.SAVE),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun LocationPicker(location: GeoPoint?, onLocationPicked: (GeoPoint) -> Unit, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location?.toLatLng() ?: LatLng(52.0, 19.4), if (location != null) 13f else 5f)
    }
    val markerState = rememberMarkerState()
    LaunchedEffect(location) {
        if (location != null) {
            markerState.position = location.toLatLng()
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(location.toLatLng()))
        }
    }
    val description = stringResource(R.string.place_edit_map_description)
    GoogleMap(
        modifier = modifier.semantics { contentDescription = description },
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false, mapToolbarEnabled = false),
        onMapClick = { onLocationPicked(GeoPoint(it.latitude, it.longitude)) },
    ) {
        if (location != null) Marker(state = markerState)
    }
}

@Composable
private fun LocationRow(location: GeoPoint?, error: PlaceInputError?, isLocating: Boolean, onUseMyLocation: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.place_edit_location), style = MaterialTheme.typography.labelLarge)
            Text(
                text = location?.let { String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) }
                    ?: stringResource(R.string.place_edit_location_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (error != null) {
                Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onUseMyLocation, enabled = !isLocating) {
            if (isLocating) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(stringResource(R.string.place_edit_use_my_location), Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AddressSearch(
    query: String,
    results: List<GeocodingResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onResultSelected: (GeocodingResult) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.place_edit_search)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onSearch, enabled = query.trim().length >= 3) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.place_edit_search))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (results.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                results.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result.label) },
                        modifier = Modifier.clickable { onResultSelected(result) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(categories: List<Category>, selectedId: Long?, onCategoryChange: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.place_edit_category_none),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.place_edit_category)) },
            leadingIcon = { Icon(categoryIcon(selected?.iconKey), contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.place_edit_category_none)) },
                onClick = {
                    onCategoryChange(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = { Icon(categoryIcon(category.iconKey), contentDescription = null) },
                    onClick = {
                        onCategoryChange(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrioritySelector(selected: Priority, onPriorityChange: (Priority) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Priority.entries.forEachIndexed { index, priority ->
            SegmentedButton(
                selected = priority == selected,
                onClick = { onPriorityChange(priority) },
                shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size),
            ) {
                Text(stringResource(priority.labelRes()))
            }
        }
    }
}

@Composable
private fun FieldMessage(error: PlaceInputError?, counter: String? = null) {
    Box(Modifier.fillMaxWidth()) {
        if (error != null) Text(stringResource(error.messageRes()))
        if (counter != null) Text(counter, Modifier.align(Alignment.CenterEnd))
    }
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

internal fun PlaceInputError.messageRes(): Int = when (this) {
    PlaceInputError.NAME_EMPTY -> R.string.error_name_empty
    PlaceInputError.NAME_TOO_LONG -> R.string.error_name_too_long
    PlaceInputError.LOCATION_MISSING -> R.string.error_location_missing
    PlaceInputError.LOCATION_INVALID -> R.string.error_location_invalid
    PlaceInputError.ADDRESS_TOO_LONG -> R.string.error_address_too_long
    PlaceInputError.NOTE_TOO_LONG -> R.string.error_note_too_long
}

internal fun Priority.labelRes(): Int = when (this) {
    Priority.LOW -> R.string.priority_low
    Priority.MEDIUM -> R.string.priority_medium
    Priority.HIGH -> R.string.priority_high
}

object PlaceEditTags {
    const val NAME = "place_edit_name"
    const val SAVE = "place_edit_save"
}
