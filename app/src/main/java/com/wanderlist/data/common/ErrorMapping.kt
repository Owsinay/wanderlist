package com.wanderlist.data.common

import android.database.SQLException
import com.wanderlist.domain.model.AppError
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Zamienia wyjątek techniczny na błąd domenowy. */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is IOException -> AppError.Network(cause = this)
    is SQLException -> AppError.Database(cause = this)
    else -> AppError.Unknown(this)
}

/**
 * Jak [runCatching], ale nie połyka anulowania korutyny i zwraca błąd jako [AppError].
 * Używane w implementacjach repozytoriów.
 */
inline fun <T> runCatchingApp(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.toAppError())
    }
