package com.wanderlist.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanderlist.R
import com.wanderlist.domain.model.Category
import com.wanderlist.domain.model.DistanceUnit
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.ui.components.StatusFilter
import com.wanderlist.ui.components.categoryIcon
import com.wanderlist.ui.components.formatDistance
import com.wanderlist.ui.components.rememberErrorMessage
import com.wanderlist.ui.theme.PinVisited
import com.wanderlist.ui.theme.PinWantToVisit

@Composable
fun PlacesScreen(
    onAddPlace: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = rememberErrorMessage()
    val deletedTemplate = stringResource(R.string.places_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PlacesEvent.PlaceDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedTemplate.format(event.place.name),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.place.id)
                    } else {
                        viewModel.confirmDelete(event.place.id)
                    }
                }
                is PlacesEvent.ShowError -> snackbarHostState.showSnackbar(errorMessage(event.error))
            }
        }
    }

    PlacesContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onStatusChange = viewModel::onStatusChange,
        onCategoryChange = viewModel::onCategoryChange,
        onSortChange = viewModel::onSortChange,
        onDelete = viewModel::onDeleteRequest,
        onAddPlace = onAddPlace,
        onPlaceClick = onPlaceClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlacesContent(
    uiState: PlacesUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onStatusChange: (StatusFilter) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onSortChange: (PlaceSort) -> Unit,
    onDelete: (Place) -> Unit,
    onAddPlace: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_places)) },
                actions = {
                    SortMenu(selected = uiState.filters.sort, hasLocation = uiState.hasLocation, onSortChange = onSortChange)
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.screen_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlace) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_place))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = uiState.filters.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.places_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.filters.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                singleLine = true,
            )
            FilterRow(
                filters = uiState.filters,
                categories = uiState.categories,
                onStatusChange = onStatusChange,
                onCategoryChange = onCategoryChange,
            )
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !uiState.hasAnyPlaces -> EmptyState(
                    text = stringResource(R.string.places_empty),
                    actionLabel = stringResource(R.string.action_add_place),
                    onAction = onAddPlace,
                )
                uiState.items.isEmpty() -> EmptyState(text = stringResource(R.string.places_no_results))
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(uiState.items, key = { it.place.id }) { item ->
                        SwipeToDeleteItem(onDelete = { onDelete(item.place) }) {
                            PlaceRow(item = item, unit = uiState.distanceUnit, onClick = { onPlaceClick(item.place.id) })
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filters: PlacesFilters,
    categories: List<Category>,
    onStatusChange: (StatusFilter) -> Unit,
    onCategoryChange: (Long?) -> Unit,
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == filters.categoryId }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == filters.status,
                onClick = { onStatusChange(filter) },
                label = { Text(stringResource(filter.labelRes)) },
                leadingIcon = filter.color?.let { color -> { StatusDot(color) } },
            )
        }
        Box {
            FilterChip(
                selected = selectedCategory != null,
                onClick = { categoryMenuOpen = true },
                label = { Text(selectedCategory?.name ?: stringResource(R.string.places_category_any)) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.places_category_any)) },
                    onClick = {
                        onCategoryChange(null)
                        categoryMenuOpen = false
                    },
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        leadingIcon = { Icon(categoryIcon(category.iconKey), contentDescription = null) },
                        onClick = {
                            onCategoryChange(category.id)
                            categoryMenuOpen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(selected: PlaceSort, hasLocation: Boolean, onSortChange: (PlaceSort) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.places_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PlaceSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(sort.labelRes()))
                            if (sort == PlaceSort.DISTANCE && !hasLocation) {
                                Text(
                                    stringResource(R.string.places_sort_distance_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        if (sort == selected) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        onSortChange(sort)
                        open = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            value == SwipeToDismissBoxValue.EndToStart
        },
    )
    val deleteLabel = stringResource(R.string.action_delete)
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        // Alternatywa dla gestu dla czytników ekranu (TalkBack → akcje).
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        content()
    }
}

@Composable
private fun PlaceRow(item: PlaceListItem, unit: DistanceUnit, onClick: () -> Unit) {
    val place = item.place
    val statusLabel = stringResource(if (place.status == PlaceStatus.VISITED) R.string.status_visited else R.string.status_want_to_visit)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val details = listOfNotNull(item.category?.name, place.address).joinToString(" · ")
            if (details.isNotEmpty()) Text(details, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Icon(categoryIcon(item.category?.iconKey), contentDescription = item.category?.name)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                item.distanceMeters?.let { Text(formatDistance(it, unit), style = MaterialTheme.typography.labelLarge) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusDot(if (place.status == PlaceStatus.VISITED) PinVisited else PinWantToVisit)
                    Text(statusLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
    )
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun EmptyState(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun PlaceSort.labelRes() = when (this) {
    PlaceSort.DISTANCE -> R.string.places_sort_distance
    PlaceSort.NEWEST -> R.string.places_sort_newest
    PlaceSort.NAME -> R.string.places_sort_name
    PlaceSort.PRIORITY -> R.string.places_sort_priority
}
