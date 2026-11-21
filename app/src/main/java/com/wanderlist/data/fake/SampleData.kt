package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Priority

/** Przykładowe dane dla implementacji tymczasowych i podglądów Compose. */
object SampleData {
    val places = listOf(
        Place(
            id = 1,
            name = "Wawel",
            location = GeoPoint(50.0540, 19.9354),
            address = "Wawel 5, Kraków",
            categoryId = 1,
            status = PlaceStatus.VISITED,
            priority = Priority.HIGH,
        ),
        Place(
            id = 2,
            name = "Morskie Oko",
            location = GeoPoint(49.2013, 20.0711),
            address = "Tatrzański Park Narodowy",
            categoryId = 2,
            priority = Priority.HIGH,
        ),
        Place(
            id = 3,
            name = "Zamek w Malborku",
            location = GeoPoint(54.0398, 19.0283),
            address = "Starościńska 1, Malbork",
            categoryId = 1,
        ),
        Place(
            id = 4,
            name = "Muzeum Powstania Warszawskiego",
            location = GeoPoint(52.2323, 20.9810),
            address = "Grzybowska 79, Warszawa",
            categoryId = 3,
            priority = Priority.LOW,
        ),
    )
}
