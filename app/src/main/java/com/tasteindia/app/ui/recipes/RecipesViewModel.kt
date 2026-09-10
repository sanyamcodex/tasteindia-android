package com.tasteindia.app.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.app.data.repository.MealRepository
import com.tasteindia.app.domain.model.AppError
import com.tasteindia.app.domain.model.FilterState
import com.tasteindia.app.domain.model.Meal
import com.tasteindia.app.domain.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: MealRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_SEARCH_QUERY = "key_search_query"
        private const val KEY_FAVOURITES_SET = "key_favourites_set"
        private const val KEY_CATEGORY_FILTER = "key_category_filter"
        private const val KEY_INGREDIENT_FILTER = "key_ingredient_filter"
        private const val KEY_FAVOURITES_ONLY = "key_favourites_only"
        private const val KEY_SORT_ORDER = "key_sort_order"
    }

    private val searchQueryState = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    private val categoryFilterState = savedStateHandle.getStateFlow<String?>(KEY_CATEGORY_FILTER, null)
    private val ingredientFilterState = savedStateHandle.getStateFlow<String?>(KEY_INGREDIENT_FILTER, null)
    private val favouritesOnlyState = savedStateHandle.getStateFlow(KEY_FAVOURITES_ONLY, false)
    private val sortOrderState = savedStateHandle.getStateFlow(KEY_SORT_ORDER, SortOrder.NAME_ASC.name)

    // Set of favourite meal IDs sourced reactively from Room DAO flow via repository
    private val favouriteIds = repository.getFavouriteIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptySet()
        )

    // Trigger for manual retry or refresh
    private val retryTrigger = MutableStateFlow(0)

    // Debounced search query flow (300ms)
    private val debouncedQuery = searchQueryState
        .debounce(300L)
        .distinctUntilChanged()

    private val filterCriteria = combine(
        categoryFilterState,
        ingredientFilterState,
        favouritesOnlyState,
        sortOrderState
    ) { category, ingredient, favouritesOnly, sortOrderStr ->
        val sortOrder = try {
            SortOrder.valueOf(sortOrderStr)
        } catch (_: Exception) {
            SortOrder.NAME_ASC
        }
        Criteria(
            category = category,
            ingredient = ingredient,
            favouritesOnly = favouritesOnly,
            sortOrder = sortOrder
        )
    }

    val uiState: StateFlow<RecipesUiState> = combine(
        debouncedQuery,
        filterCriteria,
        favouriteIds,
        retryTrigger
    ) { query, criteria, favs, _ ->
        FilterParams(
            query = query.trim(),
            category = criteria.category,
            ingredient = criteria.ingredient,
            favouritesOnly = criteria.favouritesOnly,
            sortOrder = criteria.sortOrder,
            favourites = favs
        )
    }.distinctUntilChanged()
        .flatMapLatest { params ->
            flow {
                emit(RecipesUiState.Loading)

                val activeFilterState = FilterState(
                    query = params.query,
                    category = params.category,
                    ingredient = params.ingredient,
                    favouritesOnly = params.favouritesOnly,
                    sortOrder = params.sortOrder
                )

                val result = repository.getIndianMeals()
                result.fold(
                    onSuccess = { allIndianMeals ->
                        var filtered = allIndianMeals

                        // 1. Apply name search if non-empty
                        if (params.query.isNotBlank()) {
                            filtered = filtered.filter { meal ->
                                meal.name.contains(params.query, ignoreCase = true)
                            }
                        }

                        // 2. Filter by category if set
                        if (!params.category.isNullOrBlank()) {
                            val catResult = repository.filterByCategoryWithinIndia(params.category)
                            catResult.onSuccess { allowedIds ->
                                filtered = filtered.filter { allowedIds.contains(it.id) }
                            }
                        }

                        // 3. Filter by ingredient if set
                        if (!params.ingredient.isNullOrBlank()) {
                            val ingResult = repository.filterByIngredientWithinIndia(params.ingredient)
                            ingResult.onSuccess { allowedIds ->
                                filtered = filtered.filter { allowedIds.contains(it.id) }
                            }
                        }

                        // 4. Map favourite state
                        val mappedWithFavs = filtered.map { meal ->
                            meal.copy(isFavourite = params.favourites.contains(meal.id))
                        }

                        // 5. Favourites only filter
                        val afterFavFilter = if (params.favouritesOnly) {
                            mappedWithFavs.filter { it.isFavourite }
                        } else {
                            mappedWithFavs
                        }

                        // 6. Sort
                        val sorted = when (params.sortOrder) {
                            SortOrder.NAME_ASC -> afterFavFilter.sortedBy { it.name.lowercase() }
                            SortOrder.NAME_DESC -> afterFavFilter.sortedByDescending { it.name.lowercase() }
                        }

                        if (sorted.isEmpty()) {
                            emit(
                                RecipesUiState.Empty(
                                    message = if (params.query.isNotBlank() || params.category != null || params.ingredient != null || params.favouritesOnly) {
                                        "No recipes match your criteria"
                                    } else {
                                        "No Indian recipes available"
                                    },
                                    activeFilters = activeFilterState
                                )
                            )
                        } else {
                            emit(
                                RecipesUiState.Success(
                                    meals = sorted,
                                    resultCount = sorted.size,
                                    activeFilters = activeFilterState
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = when (error) {
                            is AppError.Network -> "No internet connection. Please check your network."
                            is AppError.Timeout -> "Connection timed out. Please try again."
                            is AppError.ServerError -> "Server error (${error.code ?: "500"}). Please try again later."
                            else -> error.message ?: "Failed to load Indian recipes."
                        }
                        emit(RecipesUiState.Error(errorMessage))
                    }
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = RecipesUiState.Loading
        )

    // Categories list loaded from repository
    val categories: StateFlow<List<String>> = flow {
        val result = repository.listCategories()
        emit(result.getOrDefault(emptyList()))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Derived ingredients list from Indian meals or popular spices
    val ingredients: StateFlow<List<String>> = flow {
        val defaultSpices = listOf(
            "Chicken", "Garam Masala", "Ginger", "Garlic", "Onion",
            "Coriander", "Cumin", "Turmeric", "Chili", "Rice", "Tomato",
            "Yogurt", "Paneer", "Butter", "Cardamom", "Cinnamon", "Cloves"
        )
        val mealsResult = repository.getIndianMeals()
        val derived = mealsResult.map { meals ->
            meals.flatMap { meal ->
                meal.name.split(" ", "-", ",")
                    .map { it.trim() }
                    .filter { it.length > 3 }
            }.distinct().sorted()
        }.getOrDefault(emptyList())

        val merged = (defaultSpices + derived).distinct().sorted()
        emit(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = newQuery
    }

    fun applyFilters(newFilters: FilterState) {
        savedStateHandle[KEY_SEARCH_QUERY] = newFilters.query
        savedStateHandle[KEY_CATEGORY_FILTER] = newFilters.category
        savedStateHandle[KEY_INGREDIENT_FILTER] = newFilters.ingredient
        savedStateHandle[KEY_FAVOURITES_ONLY] = newFilters.favouritesOnly
        savedStateHandle[KEY_SORT_ORDER] = newFilters.sortOrder.name
    }

    fun toggleFavourite(meal: Meal) {
        viewModelScope.launch {
            repository.toggleFavourite(meal)
        }
    }

    fun retry() {
        retryTrigger.value += 1
    }

    fun clearFilters() {
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        savedStateHandle[KEY_CATEGORY_FILTER] = null
        savedStateHandle[KEY_INGREDIENT_FILTER] = null
        savedStateHandle[KEY_FAVOURITES_ONLY] = false
        savedStateHandle[KEY_SORT_ORDER] = SortOrder.NAME_ASC.name
    }

    private data class Criteria(
        val category: String?,
        val ingredient: String?,
        val favouritesOnly: Boolean,
        val sortOrder: SortOrder
    )

    private data class FilterParams(
        val query: String,
        val category: String?,
        val ingredient: String?,
        val favouritesOnly: Boolean,
        val sortOrder: SortOrder,
        val favourites: Set<String>
    )
}
