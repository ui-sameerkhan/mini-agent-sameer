package com.lazyshopper.app.core.ui

/** Generic screen-level result wrapper used by ViewModels across every feature module. */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** For actions (submit/save/delete) that don't hold a data payload but need loading/error feedback. */
sealed interface ActionState {
    data object Idle : ActionState
    data object InFlight : ActionState
    data object Done : ActionState
    data class Failed(val message: String) : ActionState
}
