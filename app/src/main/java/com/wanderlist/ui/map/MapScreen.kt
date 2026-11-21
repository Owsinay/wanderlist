package com.wanderlist.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.wanderlist.BuildConfig
import com.wanderlist.R
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.ui.components.StatusFilter
import com.wanderlist.ui.components.rememberErrorMessage
import com.wanderlist.ui.theme.PinVisited
import com.wanderlist.ui.theme.PinWantToVisit
import kotlinx.coroutines.launch

private val POLAND_CENTER = LatLng(52.0, 19.4)
private const val DEFAULT_ZOOM = 5.5f
private const val LOCATION_ZOOM = 13f

@Composable
fun MapScreen(
    onAddPlace: (GeoPoint?) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(POLAND_CENTER, DEFAULT_ZOOM)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        viewModel.onLocationPermissionChanged(granted)
        if (granted) viewModel.locateMe()
    }

    LaunchedEffect(Unit) {
        viewModel.onLocationPermissionChanged(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }

    val permissionMessage = stringResource(R.string.map_location_permission_denied)
    val settingsLabel = stringResource(R.string.screen_settings)
    val errorMessage = rememberErrorMessage()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is MapEvent.MoveCamera -> cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(event.target.latitude, event.target.longitude), LOCATION_ZOOM),
                )
                is MapEvent.ShowError -> if (event.error is AppError.Permission) {
                    val result = snackbarHostState.showSnackbar(permissionMessage, settingsLabel, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) context.openAppSettings()
                } else {
                    snackbarHostState.showSnackbar(errorMessage(event.error))
                }
            }
        }
    }

    MapContent(
        uiState = uiState,
        hasLocationPermission = hasPermission,
        cameraPositionState = cameraPositionState,
        snackbarHostState = snackbarHostState,
        onFilterChange = viewModel::onFilterChange,
        onPlaceSelected = viewModel::onPlaceSelected,
        onMyLocationClick = {
            if (hasPermission) {
                viewModel.locateMe()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        },
        onAddPlace = onAddPlace,
        onPlaceClick = onPlaceClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
private fun MapContent(
    uiState: MapUiState,
    hasLocationPermission: Boolean,
    cameraPositionState: CameraPositionState,
    snackbarHostState: SnackbarHostState,
    onFilterChange: (StatusFilter) -> Unit,
    onPlaceSelected: (Long?) -> Unit,
    onMyLocationClick: () -> Unit,
    onAddPlace: (GeoPoint?) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val clusterItems = remember(uiState.places) { uiState.places.map(::PlaceClusterItem) }
    val mapDescription = stringResource(R.string.map_content_description, uiState.places.size)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_map)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.screen_settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = mapDescription },
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false, mapToolbarEnabled = false),
                onMapClick = { onPlaceSelected(null) },
                onMapLongClick = { latLng -> onAddPlace(GeoPoint(latLng.latitude, latLng.longitude)) },
            ) {
                Clustering(
                    items = clusterItems,
                    onClusterClick = { cluster ->
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(cluster.position, cameraPositionState.position.zoom + 2f),
                            )
                        }
                        true
                    },
                    onClusterItemClick = { item ->
                        onPlaceSelected(item.place.id)
                        true
                    },
                    clusterItemContent = { item -> PlacePin(item.place.status) },
                )
            }

            if (uiState.isLoading || uiState.isLocating) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Column(Modifier.align(Alignment.TopStart)) {
                FilterRow(selected = uiState.filter, onFilterChange = onFilterChange)
                if (!BuildConfig.HAS_MAPS_KEY) {
                    InfoBanner(stringResource(R.string.map_missing_api_key))
                } else if (!uiState.isLoading && uiState.places.isEmpty() && uiState.filter == StatusFilter.ALL) {
                    InfoBanner(stringResource(R.string.map_empty_hint))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(onClick = onMyLocationClick) {
                    if (uiState.isLocating) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_my_location))
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        val target = cameraPositionState.position.target
                        onAddPlace(GeoPoint(target.latitude, target.longitude))
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_place)) },
                )
                uiState.selectedPlace?.let { place ->
                    SelectedPlaceCard(
                        place = place,
                        onDetails = { onPlaceClick(place.id) },
                        onClose = { onPlaceSelected(null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: StatusFilter, onFilterChange: (StatusFilter) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(filter.labelRes)) },
                leadingIcon = filter.color?.let { color ->
                    { Box(Modifier.size(10.dp).background(color, CircleShape)) }
                },
            )
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(
        modifier = Modifier.padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlacePin(status: PlaceStatus) {
    Icon(
        imageVector = Icons.Filled.Place,
        contentDescription = null,
        tint = if (status == PlaceStatus.VISITED) PinVisited else PinWantToVisit,
        modifier = Modifier.size(40.dp),
    )
}

@Composable
private fun SelectedPlaceCard(place: Place, onDetails: () -> Unit, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                place.address?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    stringResource(if (place.status == PlaceStatus.VISITED) R.string.status_visited else R.string.status_want_to_visit),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (place.status == PlaceStatus.VISITED) PinVisited else PinWantToVisit,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
        TextButton(onClick = onDetails, modifier = Modifier.align(Alignment.End).padding(end = 8.dp)) {
            Text(stringResource(R.string.action_show_details))
        }
    }
}

private fun android.content.Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
