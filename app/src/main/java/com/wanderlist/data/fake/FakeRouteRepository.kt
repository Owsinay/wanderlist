package com.wanderlist.data.fake

import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Route
import com.wanderlist.domain.model.TransportMode
import com.wanderlist.domain.repository.RouteRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Trasa w linii prostej; zastąpiona przez OpenRouteService w zadaniu B1. */
@Singleton
class FakeRouteRepository @Inject constructor() : RouteRepository {
    override suspend fun getRoute(points: List<GeoPoint>, mode: TransportMode): Result<Route> {
        if (points.size < 2) return Result.failure(AppError.Validation("points", "min_two_points"))
        val distance = points.zipWithNext { a, b -> a.distanceMetersTo(b) }.sum()
        val speedMetersPerSecond = when (mode) {
            TransportMode.DRIVING -> 15.0
            TransportMode.CYCLING -> 4.5
            TransportMode.WALKING -> 1.4
        }
        return Result.success(Route(points, distance, distance / speedMetersPerSecond, mode))
    }
}
