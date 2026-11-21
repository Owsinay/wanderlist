package com.wanderlist.domain.repository

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Route
import com.wanderlist.domain.model.TransportMode

interface RouteRepository {
    /** Wyznacza trasę przez [points] w podanej kolejności (min. 2 punkty). */
    suspend fun getRoute(points: List<GeoPoint>, mode: TransportMode): Result<Route>
}
