package com.wanderlist.domain.usecase

import com.wanderlist.domain.model.PlaceInput
import com.wanderlist.domain.model.PlaceInputError
import javax.inject.Inject

class ValidatePlaceInputUseCase @Inject constructor() {
    operator fun invoke(input: PlaceInput): List<PlaceInputError> = buildList {
        val name = input.name.trim()
        when {
            name.isEmpty() -> add(PlaceInputError.NAME_EMPTY)
            name.length > MAX_NAME_LENGTH -> add(PlaceInputError.NAME_TOO_LONG)
        }
        when {
            input.location == null -> add(PlaceInputError.LOCATION_MISSING)
            !input.location.isValid -> add(PlaceInputError.LOCATION_INVALID)
        }
        if ((input.address?.trim()?.length ?: 0) > MAX_ADDRESS_LENGTH) add(PlaceInputError.ADDRESS_TOO_LONG)
        if ((input.note?.trim()?.length ?: 0) > MAX_NOTE_LENGTH) add(PlaceInputError.NOTE_TOO_LONG)
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_ADDRESS_LENGTH = 200
        const val MAX_NOTE_LENGTH = 1000
    }
}
