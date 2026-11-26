package com.wanderlist.data.remote.api

import com.wanderlist.data.remote.dto.DirectionsRequestDto
import com.wanderlist.data.remote.dto.DirectionsResponseDto
import com.wanderlist.data.remote.dto.GeocodeResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** OpenRouteService. Klucz dodaje [com.wanderlist.data.remote.ApiKeyInterceptor]. */
interface OrsApi {
    @POST("v2/directions/{profile}/geojson")
    suspend fun directions(
        @Path("profile") profile: String,
        @Body body: DirectionsRequestDto,
    ): DirectionsResponseDto

    @GET("geocode/search")
    suspend fun search(
        @Query("text") text: String,
        @Query("size") size: Int = 5,
        @Query("lang") lang: String = "pl",
    ): GeocodeResponseDto

    @GET("geocode/reverse")
    suspend fun reverse(
        @Query("point.lon") longitude: Double,
        @Query("point.lat") latitude: Double,
        @Query("size") size: Int = 1,
        @Query("lang") lang: String = "pl",
    ): GeocodeResponseDto

    companion object {
        const val BASE_URL = "https://api.openrouteservice.org/"
    }
}
