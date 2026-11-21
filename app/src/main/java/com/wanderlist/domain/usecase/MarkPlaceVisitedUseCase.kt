package com.wanderlist.domain.usecase

import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Visit
import com.wanderlist.domain.model.VisitInputError
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.repository.VisitRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Zapisuje odwiedziny i zmienia status miejsca na „odwiedzone”. */
class MarkPlaceVisitedUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val visitRepository: VisitRepository,
    private val clock: Clock,
) {
    fun validate(date: LocalDate, rating: Int, memo: String?): List<VisitInputError> = buildList {
        if (rating !in MIN_RATING..MAX_RATING) add(VisitInputError.RATING_OUT_OF_RANGE)
        if (date.isAfter(LocalDate.now(clock))) add(VisitInputError.DATE_IN_FUTURE)
        if ((memo?.trim()?.length ?: 0) > MAX_MEMO_LENGTH) add(VisitInputError.MEMO_TOO_LONG)
    }

    suspend operator fun invoke(
        placeId: Long,
        date: LocalDate,
        rating: Int,
        memo: String? = null,
        photoUri: String? = null,
    ): Result<Long> {
        validate(date, rating, memo).firstOrNull()?.let { error ->
            return Result.failure(AppError.Validation(error.field, error.name))
        }
        val place = placeRepository.observePlace(placeId).first()
            ?: return Result.failure(AppError.NotFound())

        val visitId = visitRepository.addVisit(
            Visit(
                placeId = placeId,
                date = date,
                rating = rating,
                memo = memo?.trim()?.takeIf { it.isNotEmpty() },
                photoUri = photoUri,
            ),
        ).getOrElse { return Result.failure(it) }

        if (place.status != PlaceStatus.VISITED) {
            placeRepository.updatePlace(place.copy(status = PlaceStatus.VISITED))
                .onFailure { return Result.failure(it) }
        }
        return Result.success(visitId)
    }

    companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 5
        const val MAX_MEMO_LENGTH = 1000
    }
}
