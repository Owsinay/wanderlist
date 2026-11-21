package com.wanderlist.data.local.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wanderlist.domain.model.GeoPoint
import java.util.Locale

/** Ostatnio pobrana trasa. [key] powstaje z trybu transportu i punktów (patrz [routeCacheKey]). */
@Entity(tableName = "cached_routes")
data class CachedRouteEntity(
    @PrimaryKey val key: String,
    /** Geometria jako "lat,lon;lat,lon;…". */
    val geometry: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val savedAtMillis: Long,
)

/** Ostatnia pogoda dla lokalizacji zaokrąglonej do ok. 1 km. */
@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey val key: String,
    val temperatureCelsius: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val fetchedAtMillis: Long,
)

fun List<GeoPoint>.encodeGeometry(): String =
    joinToString(";") { "${it.latitude},${it.longitude}" }

fun String.decodeGeometry(): List<GeoPoint> =
    if (isEmpty()) emptyList() else split(";").map { pair ->
        val (lat, lon) = pair.split(",")
        GeoPoint(lat.toDouble(), lon.toDouble())
    }

/** Punkty zaokrąglone do 5 miejsc (ok. 1 m), żeby ta sama trasa dawała ten sam klucz. */
fun routeCacheKey(points: List<GeoPoint>, mode: String): String =
    mode + "|" + points.joinToString(";") { "%.5f,%.5f".format(Locale.US, it.latitude, it.longitude) }

/** Zaokrąglenie do 2 miejsc (ok. 1 km) – pogoda nie zmienia się na krótkim dystansie. */
fun weatherCacheKey(point: GeoPoint): String =
    "%.2f,%.2f".format(Locale.US, point.latitude, point.longitude)
