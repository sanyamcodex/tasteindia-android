package com.tasteindia.app.ui.details

import com.tasteindia.app.domain.model.MealDetail

sealed interface DetailsUiState {
    data object Loading : DetailsUiState

    data class Success(
        val mealDetail: MealDetail
    ) : DetailsUiState

    data class Error(
        val message: String
    ) : DetailsUiState
}
