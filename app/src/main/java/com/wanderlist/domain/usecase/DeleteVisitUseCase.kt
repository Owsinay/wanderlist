package com.wanderlist.domain.usecase

import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.repository.VisitRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Usuwa odwiedziny; jeśli były ostatnie, miejsce wraca na listę „do odwiedzenia”. */
class DeleteVisitUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val visitRepository: VisitRepository,
) {
    suspend operator fun invoke(placeId: Long, visitId: Long): Result<Unit> {
        visitRepository.deleteVisit(visitId).onFailure { return Result.failure(it) }

        val remaining = visitRepository.observeVisitsForPlace(placeId).first()
        val place = placeRepository.observePlace(placeId).first()
        if (remaining.isEmpty() && place != null && place.status == PlaceStatus.VISITED) {
            return placeRepository.updatePlace(place.copy(status = PlaceStatus.WANT_TO_VISIT))
        }
        return Result.success(Unit)
    }
}
