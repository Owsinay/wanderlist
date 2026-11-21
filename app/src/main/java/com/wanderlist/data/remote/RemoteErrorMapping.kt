package com.wanderlist.data.remote

import com.wanderlist.data.common.toAppError
import com.wanderlist.domain.model.AppError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Zamienia wyjątek z warstwy sieciowej na [AppError].
 *
 * - 429 (limit zapytań) i 5xx traktujemy jako [AppError.Network], bo po chwili można spróbować ponownie.
 * - 400/404 to [AppError.NotFound] tylko gdy [badRequestMeansNotFound] (np. ORS nie znalazł trasy).
 * - Pozostałe kody HTTP (np. 401 przy złym kluczu) to [AppError.Unknown].
 * - Brak internetu i timeout (IOException) mapuje wspólny [toAppError].
 */
fun Throwable.toRemoteAppError(badRequestMeansNotFound: Boolean = false): AppError = when (this) {
    is HttpException -> when (val code = code()) {
        400, 404 -> if (badRequestMeansNotFound) AppError.NotFound() else AppError.Unknown(this)
        429, in 500..599 -> AppError.Network(httpCode = code, cause = this)
        else -> AppError.Unknown(this)
    }
    is SerializationException -> AppError.Unknown(this)
    else -> toAppError()
}

/** Jak [com.wanderlist.data.common.runCatchingApp], ale rozumie błędy HTTP z Retrofita. */
suspend inline fun <T> runRemote(badRequestMeansNotFound: Boolean = false, block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.toRemoteAppError(badRequestMeansNotFound))
    }
