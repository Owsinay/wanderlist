package com.wanderlist.domain.usecase

import com.wanderlist.domain.repository.PlaceRepository
import javax.inject.Inject

/** Usuwa miejsce; odwiedziny i powiązania z trasami usuwa baza (ON DELETE CASCADE). */
class DeletePlaceUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
) {
    suspend operator fun invoke(placeId: Long): Result<Unit> = placeRepository.deletePlace(placeId)
}
