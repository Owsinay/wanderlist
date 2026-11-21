package com.wanderlist.domain.usecase

import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.model.PlaceInput
import com.wanderlist.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Dodaje nowe miejsce (placeId = 0) albo aktualizuje istniejące. Zwraca id zapisanego miejsca.
 * Przy edycji zachowuje status i datę utworzenia.
 */
class SavePlaceUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val validate: ValidatePlaceInputUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(placeId: Long, input: PlaceInput): Result<Long> {
        validate(input).firstOrNull()?.let { error ->
            return Result.failure(AppError.Validation(error.field, error.name))
        }
        val location = requireNotNull(input.location)
        val name = input.name.trim()
        val address = input.address?.trim()?.takeIf { it.isNotEmpty() }
        val note = input.note?.trim()?.takeIf { it.isNotEmpty() }

        if (placeId == 0L) {
            return placeRepository.addPlace(
                Place(
                    name = name,
                    location = location,
                    address = address,
                    categoryId = input.categoryId,
                    priority = input.priority,
                    note = note,
                    createdAt = clock.instant(),
                ),
            )
        }

        val existing = placeRepository.observePlace(placeId).first()
            ?: return Result.failure(AppError.NotFound())
        return placeRepository.updatePlace(
            existing.copy(
                name = name,
                location = location,
                address = address,
                categoryId = input.categoryId,
                priority = input.priority,
                note = note,
            ),
        ).map { placeId }
    }
}
