package com.wanderlist.ui.components

import androidx.annotation.StringRes
import com.wanderlist.R
import com.wanderlist.domain.model.AppError

/** Komunikat dla użytkownika odpowiadający błędowi domenowemu. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    is AppError.Network -> R.string.error_network
    is AppError.Database -> R.string.error_database
    is AppError.Validation -> R.string.error_validation
    is AppError.Permission -> R.string.error_permission
    is AppError.NotFound -> R.string.error_not_found
    is AppError.Unknown -> R.string.error_unknown
}
