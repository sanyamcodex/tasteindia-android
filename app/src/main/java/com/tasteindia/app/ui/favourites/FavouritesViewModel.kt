package com.tasteindia.app.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.app.data.repository.MealRepository
import com.tasteindia.app.domain.model.Meal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: MealRepository
) : ViewModel() {

    val favouriteMeals: StateFlow<List<Meal>> = repository.getFavourites()
        .map { favourites ->
            favourites.map { favourite ->
                Meal(
                    id = favourite.mealId,
                    name = favourite.name,
                    thumbUrl = favourite.thumbUrl,
                    isFavourite = true
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun toggleFavourite(meal: Meal) {
        viewModelScope.launch {
            repository.toggleFavourite(meal)
        }
    }
}
