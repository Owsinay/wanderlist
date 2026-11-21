package com.wanderlist.ui.placedetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.wanderlist.R
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Visit
import com.wanderlist.ui.components.categoryIcon
import com.wanderlist.ui.components.messageRes
import com.wanderlist.ui.components.rememberErrorMessage
import com.wanderlist.ui.components.weatherDescription
import com.wanderlist.ui.placeedit.labelRes
import com.wanderlist.ui.theme.PinVisited
import com.wanderlist.ui.theme.PinWantToVisit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlaceDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = rememberErrorMessage()
    val visitSaved = stringResource(R.string.visit_saved)
    val noMapsApp = stringResource(R.string.detail_no_maps_app)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                PlaceDetailEvent.PlaceDeleted -> onBack()
                PlaceDetailEvent.VisitSaved -> snackbarHostState.showSnackbar(visitSaved)
                is PlaceDetailEvent.ShowError -> snackbarHostState.showSnackbar(errorMessage(event.error))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.onDeletePlace()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }

    if (uiState.visitDialog.isOpen) {
        VisitDialog(state = uiState.visitDialog, onDismiss = viewModel::closeVisitDialog, onSave = viewModel::saveVisit)
    }

    PlaceDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = { onEdit(viewModel.placeId) },
        onDelete = { confirmDelete = true },
        onNavigate = { place ->
            if (!context.openNavigation(place)) {
                scope.launch { snackbarHostState.showSnackbar(noMapsApp) }
            }
        },
        onAddVisit = viewModel::openVisitDialog,
        onDeleteVisit = viewModel::onDeleteVisit,
        onRetryWeather = viewModel::retryWeather,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceDetailContent(
    uiState: PlaceDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: (Place) -> Unit,
    onAddVisit: () -> Unit,
    onDeleteVisit: (Visit) -> Unit,
    onRetryWeather: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val place = uiState.place
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(place?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (place != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            place == null -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error_not_found), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { MiniMap(place) }
                item { InfoSection(uiState) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { onNavigate(place) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Directions, contentDescription = null)
                            Text(stringResource(R.string.detail_navigate), Modifier.padding(start = 8.dp))
                        }
                        Button(
                            onClick = onAddVisit,
                            modifier = Modifier.weight(1f).testTag(PlaceDetailTags.MARK_VISITED),
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Text(
                                stringResource(
                                    if (place.status == PlaceStatus.VISITED) R.string.detail_add_visit else R.string.detail_mark_visited,
                                ),
                                Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                item { WeatherCard(uiState.weather, onRetryWeather) }
                place.note?.let { note ->
                    item {
                        Column {
                            Text(stringResource(R.string.place_edit_note), style = MaterialTheme.typography.titleSmall)
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.detail_visits, uiState.visits.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (uiState.visits.isEmpty()) {
                    item { Text(stringResource(R.string.detail_no_visits), style = MaterialTheme.typography.bodyMedium) }
                }
                items(uiState.visits, key = { it.id }) { visit ->
                    VisitRow(visit = visit, onDelete = { onDeleteVisit(visit) })
                }
            }
        }
    }
}

@Composable
private fun MiniMap(place: Place) {
    val position = LatLng(place.location.latitude, place.location.longitude)
    val cameraPositionState = rememberCameraPositionState(key = place.id.toString()) {
        this.position = CameraPosition.fromLatLngZoom(position, 13f)
    }
    val markerState = rememberMarkerState(key = place.id.toString(), position = position)
    val description = stringResource(R.string.detail_map_description, place.name)
    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = description },
        cameraPositionState = cameraPositionState,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        uiSettings = MapUiSettings(mapToolbarEnabled = false),
    ) {
        Marker(state = markerState, title = place.name)
    }
}

@Composable
private fun InfoSection(uiState: PlaceDetailUiState) {
    val place = uiState.place ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val visited = place.status == PlaceStatus.VISITED
            AssistChip(
                onClick = {},
                label = { Text(stringResource(if (visited) R.string.status_visited else R.string.status_want_to_visit)) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(if (visited) PinVisited else PinWantToVisit, CircleShape),
                    )
                },
            )
            uiState.category?.let { category ->
                AssistChip(
                    onClick = {},
                    label = { Text(category.name) },
                    leadingIcon = { Icon(categoryIcon(category.iconKey), contentDescription = null, Modifier.size(18.dp)) },
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.detail_priority, stringResource(place.priority.labelRes()))) },
            )
        }
        place.address?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        Text(
            "%.5f, %.5f".format(java.util.Locale.US, place.location.latitude, place.location.longitude),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeatherCard(state: WeatherState, onRetry: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        when (state) {
            WeatherState.Loading -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.weather_loading), Modifier.padding(start = 12.dp))
            }
            is WeatherState.Success -> {
                val description = weatherDescription(state.weather.weatherCode)
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.weather_now, state.weather.temperatureCelsius.roundToInt(), stringResource(description.labelRes)),
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.weather_wind, state.weather.windSpeedKmh.roundToInt())) },
                    leadingContent = { Icon(description.icon, contentDescription = null) },
                    overlineContent = { Text(stringResource(R.string.weather_title)) },
                )
            }
            is WeatherState.Error -> Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(state.error.messageRes()), Modifier.weight(1f).padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}

@Composable
private fun VisitRow(visit: Visit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            visit.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.visit_photo_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(visit.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), style = MaterialTheme.typography.titleSmall)
                Box(Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${visit.rating}/5"
                }) {
                    RatingBar(rating = visit.rating, onRatingChange = null, starSize = 18)
                }
                visit.memo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.visit_delete))
            }
        }
    }
}

/** Otwiera nawigację w Google Maps, a gdy jej brak – dowolną aplikację map. Zwraca false, gdy nie ma żadnej. */
private fun Context.openNavigation(place: Place): Boolean {
    val (lat, lng) = place.location
    val navigation = Intent(Intent.ACTION_VIEW, "google.navigation:q=$lat,$lng".toUri())
        .setPackage("com.google.android.apps.maps")
    val generic = Intent(Intent.ACTION_VIEW, "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(place.name)})".toUri())
    return listOf(navigation, generic).any { intent ->
        try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

object PlaceDetailTags {
    const val MARK_VISITED = "detail_mark_visited"
    const val SAVE_VISIT = "visit_save"
    const val STAR_PREFIX = "visit_star_"
}
