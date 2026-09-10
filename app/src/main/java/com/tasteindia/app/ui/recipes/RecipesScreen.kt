package com.tasteindia.app.ui.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasteindia.app.domain.model.FilterState
import com.tasteindia.app.ui.components.EmptyState
import com.tasteindia.app.ui.components.ErrorState
import com.tasteindia.app.ui.components.LoadingState
import com.tasteindia.app.ui.components.MealRow
import com.tasteindia.app.ui.filters.FilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel,
    onMealClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Determine current active filter state
    val currentFilterState = when (val state = uiState) {
        is RecipesUiState.Success -> state.activeFilters
        is RecipesUiState.Empty -> state.activeFilters
        else -> FilterState()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TasteIndia",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("recipes_top_bar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // FilterBar above the list
            FilterBar(
                filterState = currentFilterState,
                categories = categories,
                ingredients = ingredients,
                onFilterChange = { newFilters ->
                    viewModel.applyFilters(newFilters)
                }
            )

            // Content based on UiState
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is RecipesUiState.Loading -> {
                        LoadingState(modifier = Modifier.fillMaxSize())
                    }

                    is RecipesUiState.Empty -> {
                        EmptyState(
                            message = state.message,
                            actionLabel = if (currentFilterState != FilterState()) "Clear all filters" else null,
                            onAction = {
                                viewModel.clearFilters()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is RecipesUiState.Error -> {
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.retry() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is RecipesUiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Result count header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.resultCount} ${if (state.resultCount == 1) "recipe" else "recipes"} found",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("recipes_result_count")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("recipes_list")
                            ) {
                                items(
                                    items = state.meals,
                                    key = { meal -> meal.id }
                                ) { meal ->
                                    MealRow(
                                        meal = meal,
                                        onMealClick = onMealClick,
                                        onToggleFavourite = { selectedMeal -> viewModel.toggleFavourite(selectedMeal) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Recipes loading", showBackground = true)
@Composable
fun RecipesScreenLoadingPreview() {
    RecipesScreenPreviewContent(RecipesUiState.Loading)
}

@Preview(name = "Recipes empty", showBackground = true)
@Composable
fun RecipesScreenEmptyPreview() {
    RecipesScreenPreviewContent(
        RecipesUiState.Empty(
            message = "No recipes match your criteria",
            activeFilters = FilterState(query = "paneer", favouritesOnly = true)
        )
    )
}

@Preview(name = "Recipes error", showBackground = true)
@Composable
fun RecipesScreenErrorPreview() {
    RecipesScreenPreviewContent(
        RecipesUiState.Error("No internet connection. Please check your network.")
    )
}

@Preview(name = "Recipes success", showBackground = true)
@Composable
fun RecipesScreenSuccessPreview() {
    RecipesScreenPreviewContent(
        RecipesUiState.Success(
            meals = listOf(
                Meal("1", "Butter Chicken", "https://example.com/butter-chicken.jpg", isFavourite = true),
                Meal("2", "Palak Paneer", "https://example.com/palak-paneer.jpg")
            ),
            resultCount = 2,
            activeFilters = FilterState()
        )
    )
}

@Composable
private fun RecipesScreenPreviewContent(state: RecipesUiState) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("TasteIndia") })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                FilterBar(
                    filterState = when (state) {
                        is RecipesUiState.Success -> state.activeFilters
                        is RecipesUiState.Empty -> state.activeFilters
                        else -> FilterState()
                    },
                    categories = listOf("Chicken", "Vegetarian"),
                    ingredients = listOf("Paneer", "Rice"),
                    onFilterChange = {}
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (state) {
                        RecipesUiState.Loading -> LoadingState(Modifier.fillMaxSize())
                        is RecipesUiState.Empty -> EmptyState(
                            message = state.message,
                            modifier = Modifier.fillMaxSize()
                        )
                        is RecipesUiState.Error -> ErrorState(
                            message = state.message,
                            onRetry = {},
                            modifier = Modifier.fillMaxSize()
                        )
                        is RecipesUiState.Success -> LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.meals, key = { it.id }) { meal ->
                                MealRow(meal, onMealClick = {}, onToggleFavourite = {})
                            }
                        }
                    }
                }
            }
        }
    }
}
