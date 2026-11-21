package com.wanderlist.domain.usecase

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.PlaceInput
import com.wanderlist.domain.model.PlaceInputError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatePlaceInputUseCaseTest {
    private val validate = ValidatePlaceInputUseCase()
    private val valid = PlaceInput(name = "Wawel", location = GeoPoint(50.05, 19.93))

    @Test
    fun `valid input has no errors`() {
        assertTrue(validate(valid).isEmpty())
    }

    @Test
    fun `blank name is rejected`() {
        assertEquals(listOf(PlaceInputError.NAME_EMPTY), validate(valid.copy(name = "   ")))
    }

    @Test
    fun `too long name is rejected`() {
        val name = "a".repeat(ValidatePlaceInputUseCase.MAX_NAME_LENGTH + 1)
        assertEquals(listOf(PlaceInputError.NAME_TOO_LONG), validate(valid.copy(name = name)))
    }

    @Test
    fun `missing and invalid location are rejected`() {
        assertEquals(listOf(PlaceInputError.LOCATION_MISSING), validate(valid.copy(location = null)))
        assertEquals(listOf(PlaceInputError.LOCATION_INVALID), validate(valid.copy(location = GeoPoint(95.0, 10.0))))
    }

    @Test
    fun `all errors are reported at once`() {
        val input = PlaceInput(name = "", location = null, note = "x".repeat(ValidatePlaceInputUseCase.MAX_NOTE_LENGTH + 1))
        assertEquals(
            listOf(PlaceInputError.NAME_EMPTY, PlaceInputError.LOCATION_MISSING, PlaceInputError.NOTE_TOO_LONG),
            validate(input),
        )
    }
}
