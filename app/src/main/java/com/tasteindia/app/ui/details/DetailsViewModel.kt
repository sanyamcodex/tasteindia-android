package com.tasteindia.app.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.app.data.repository.MealRepository
import com.tasteindia.app.domain.model.AppError
import com.tasteindia.app.domain.model.MealDetail
import com.tasteindia.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: MealRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_IS_FAVOURITE = "key_is_favourite"
    }

    private val mealId: String = checkNotNull(savedStateHandle[Routes.Details.ARG_MEAL_ID])

    private val isFavouriteFlow = repository.getFavouriteIds()
        .map { favIds -> favIds.contains(mealId) }
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<DetailsUiState> = retryTrigger.flatMapLatest {
        flow {
            emit(DetailsUiState.Loading)
            val result = repository.getMealDetail(mealId)
            result.fold(
                onSuccess = { detail ->
                    emit(DetailsUiState.Success(detail))
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is AppError.Network -> "Network unavailable. Please check your internet connection."
                        is AppError.Timeout -> "Request timed out while loading recipe details."
                        is AppError.ServerError -> "Server error (${error.code ?: "500"}). Please try again later."
                        else -> error.message ?: "Failed to load recipe details."
                    }
                    emit(DetailsUiState.Error(errorMessage))
                }
            )
        }
    }.combine(isFavouriteFlow) { state, isFav ->
        if (state is DetailsUiState.Success) {
            state.copy(mealDetail = state.mealDetail.copy(isFavourite = isFav))
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DetailsUiState.Loading
    )

    fun toggleFavourite() {
        viewModelScope.launch {
            val state = uiState.value
            if (state is DetailsUiState.Success) {
                repository.toggleFavourite(state.mealDetail.toMeal())
            }
        }
    }

    fun setFavourite(isFav: Boolean) {
        savedStateHandle[KEY_IS_FAVOURITE] = isFav
    }

    fun retry() {
        retryTrigger.value += 1
    }
}
