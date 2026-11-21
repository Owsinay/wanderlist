package com.wanderlist.ui.components

import com.wanderlist.domain.model.AppError

/** Wspólny stan ekranu ładującego dane. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}
