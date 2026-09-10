package com.tasteindia.app.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.app.data.repository.MealRepository
import com.tasteindia.app.domain.model.Meal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: MealRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    val favouriteMeals: StateFlow<List<Meal>> = combine(
        repository.getFavouriteIds(),
        refreshTrigger
    ) { favIds, _ ->
        favIds
    }.flatMapLatest { favIds ->
        flow {
            if (favIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            // 1. First check local in-memory cache
            val cachedMeals = repository.getCachedIndianMeals()
            val cachedMap = cachedMeals.associateBy { it.id }

            // Check which IDs are missing from cache
            val missingIds = favIds.filter { !cachedMap.containsKey(it) }

            // If any are missing, try fetching all Indian meals to hydrate cache
            if (missingIds.isNotEmpty()) {
                repository.getIndianMeals()
            }

            // Get refreshed cached meals after hydration attempt
            val refreshedMap = repository.getCachedIndianMeals().associateBy { it.id }

            val resultList = favIds.mapNotNull { id ->
                val meal = refreshedMap[id]
                if (meal != null) {
                    meal.copy(isFavourite = true)
                } else {
                    // Fallback representation if offline or not loaded yet
                    Meal(
                        id = id,
                        name = "Recipe #$id",
                        thumbUrl = "",
                        isFavourite = true
                    )
                }
            }

            emit(resultList)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun toggleFavourite(id: String) {
        viewModelScope.launch {
            repository.toggleFavourite(id)
        }
    }

    fun refresh() {
        refreshTrigger.value += 1
    }
}
