package com.tasteindia.app.ui.recipes

import com.tasteindia.app.domain.model.FilterState
import com.tasteindia.app.domain.model.Meal

sealed interface RecipesUiState {
    data object Loading : RecipesUiState

    data class Success(
        val meals: List<Meal>,
        val resultCount: Int,
        val activeFilters: FilterState
    ) : RecipesUiState

    data class Empty(
        val message: String = "No Indian recipes found",
        val activeFilters: FilterState = FilterState()
    ) : RecipesUiState

    data class Error(
        val message: String
    ) : RecipesUiState
}
