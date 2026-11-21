package com.wanderlist.domain.model

import java.time.LocalDate

data class Visit(
    val id: Long = 0,
    val placeId: Long,
    val date: LocalDate,
    /** Ocena w skali 1–5. */
    val rating: Int,
    val memo: String? = null,
    val photoUri: String? = null,
)
