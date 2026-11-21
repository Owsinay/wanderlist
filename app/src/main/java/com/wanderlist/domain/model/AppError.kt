package com.wanderlist.domain.model

/**
 * Błędy domenowe. Repozytoria zamieniają wyjątki techniczne (IOException, SQLiteException, HttpException…)
 * na jeden z tych typów, a UI pokazuje dla nich komunikat dla użytkownika.
 */
sealed class AppError(cause: Throwable? = null) : Exception(cause) {
    /** Brak internetu, timeout lub błąd serwera. */
    class Network(val httpCode: Int? = null, cause: Throwable? = null) : AppError(cause)

    /** Błąd zapisu lub odczytu z lokalnej bazy. */
    class Database(cause: Throwable? = null) : AppError(cause)

    /** Niepoprawne dane wejściowe; [field] wskazuje pole formularza. */
    class Validation(val field: String, val reason: String) : AppError()

    /** Brak wymaganego uprawnienia (lokalizacja, aparat). */
    class Permission(val permission: String) : AppError()

    /** Nie znaleziono zasobu, np. trasy między punktami lub miejsca o danym id. */
    class NotFound : AppError()

    class Unknown(cause: Throwable? = null) : AppError(cause)
}
