package com.wanderlist.data.repository

import com.wanderlist.data.local.cache.ApiCacheDao
import com.wanderlist.data.local.cache.CachedRouteEntity
import com.wanderlist.data.local.cache.decodeGeometry
import com.wanderlist.data.local.cache.encodeGeometry
import com.wanderlist.data.local.cache.routeCacheKey
import com.wanderlist.data.remote.api.OrsApi
import com.wanderlist.data.remote.dto.DirectionsRequestDto
import com.wanderlist.data.remote.runRemote
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Route
import com.wanderlist.domain.model.TransportMode
import com.wanderlist.domain.repository.RouteRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trasa z OpenRouteService. Udana odpowiedź trafia do cache; gdy nie ma sieci, zwracamy ostatnią zapisaną
 * trasę dla tych samych punktów i trybu (z [Route.isFromCache] = true).
 */
@Singleton
class OrsRouteRepository @Inject constructor(
    private val api: OrsApi,
    private val cache: ApiCacheDao,
    private val clock: Clock,
) : RouteRepository {

    override suspend fun getRoute(points: List<GeoPoint>, mode: TransportMode): Result<Route> {
        if (points.size < 2) return Result.failure(AppError.Validation("points", "min_two_points"))
        if (points.any { !it.isValid }) return Result.failure(AppError.Validation("points", "invalid_coordinates"))

        val key = routeCacheKey(points, mode.name)
        val remote = runRemote(badRequestMeansNotFound = true) {
            val response = api.directions(
                profile = mode.orsProfile(),
                body = DirectionsRequestDto(points.map { listOf(it.longitude, it.latitude) }),
            )
            val feature = response.features.firstOrNull() ?: throw AppError.NotFound()
            Route(
                points = feature.geometry.coordinates.map { GeoPoint(latitude = it[1], longitude = it[0]) },
                distanceMeters = feature.properties.summary.distance,
                durationSeconds = feature.properties.summary.duration,
                transportMode = mode,
            )
        }

        remote.onSuccess { route ->
            runCatching {
                cache.upsertRoute(
                    CachedRouteEntity(
                        key = key,
                        geometry = route.points.encodeGeometry(),
                        distanceMeters = route.distanceMeters,
                        durationSeconds = route.durationSeconds,
                        savedAtMillis = clock.millis(),
                    ),
                )
            } // błąd zapisu cache nie może zepsuć udanej odpowiedzi
            return remote
        }

        // Fallback na cache tylko przy problemach z siecią, nie przy "brak trasy" ani złym kluczu.
        if (remote.exceptionOrNull() is AppError.Network) {
            val cached = runCatching { cache.getRoute(key) }.getOrNull()
            if (cached != null) {
                return Result.success(
                    Route(
                        points = cached.geometry.decodeGeometry(),
                        distanceMeters = cached.distanceMeters,
                        durationSeconds = cached.durationSeconds,
                        transportMode = mode,
                        isFromCache = true,
                    ),
                )
            }
        }
        return remote
    }

    private fun TransportMode.orsProfile(): String = when (this) {
        TransportMode.DRIVING -> "driving-car"
        TransportMode.WALKING -> "foot-walking"
        TransportMode.CYCLING -> "cycling-regular"
    }
}
