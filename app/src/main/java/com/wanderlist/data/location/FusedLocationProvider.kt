package com.wanderlist.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wanderlist.data.common.runCatchingApp
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.repository.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {
    private val client: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission") // sprawdzone w hasPermission()
    override suspend fun getCurrentLocation(): Result<GeoPoint> {
        if (!hasPermission()) return Result.failure(AppError.Permission(Manifest.permission.ACCESS_FINE_LOCATION))
        return runCatchingApp {
            val location = client.awaitCurrentLocation() ?: client.awaitLastLocation() ?: throw AppError.NotFound()
            GeoPoint(location.latitude, location.longitude)
        }
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    private suspend fun FusedLocationProviderClient.awaitCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }

    @SuppressLint("MissingPermission")
    private suspend fun FusedLocationProviderClient.awaitLastLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            lastLocation
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
}
