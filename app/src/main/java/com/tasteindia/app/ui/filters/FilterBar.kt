package com.tasteindia.app.ui.filters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasteindia.app.domain.model.FilterState
import com.tasteindia.app.domain.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    filterState: FilterState,
    categories: List<String>,
    ingredients: List<String>,
    onFilterChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var ingredientDropdownExpanded by remember { mutableStateOf(false) }

    val hasActiveFilters = filterState.query.isNotBlank() ||
            filterState.category != null ||
            filterState.ingredient != null ||
            filterState.favouritesOnly ||
            filterState.sortOrder != SortOrder.NAME_ASC

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("filter_bar_container")
    ) {
        // 1. Search TextField
        OutlinedTextField(
            value = filterState.query,
            onValueChange = { newQuery ->
                onFilterChange(filterState.copy(query = newQuery))
            },
            placeholder = { Text("Search Indian recipes...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = filterState.query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            onFilterChange(filterState.copy(query = ""))
                        },
                        modifier = Modifier.testTag("filter_search_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search query"
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("filter_search_input")
        )

        // 2. Horizontally scrollable control chips row
        val controlScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(controlScrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Dropdown Filter Chip
            Column {
                FilterChip(
                    selected = filterState.category != null,
                    onClick = { categoryDropdownExpanded = true },
                    label = {
                        Text(text = filterState.category?.let { "Category: $it" } ?: "Category ▾")
                    },
                    modifier = Modifier.testTag("filter_category_chip")
                )
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories", fontWeight = FontWeight.Bold) },
                        onClick = {
                            categoryDropdownExpanded = false
                            onFilterChange(filterState.copy(category = null))
                        }
                    )
                    categories.forEach { categoryName ->
                        DropdownMenuItem(
                            text = { Text(categoryName) },
                            onClick = {
                                categoryDropdownExpanded = false
                                onFilterChange(filterState.copy(category = categoryName))
                            }
                        )
                    }
                }
            }

            // Ingredient Dropdown Filter Chip
            Column {
                FilterChip(
                    selected = filterState.ingredient != null,
                    onClick = { ingredientDropdownExpanded = true },
                    label = {
                        Text(text = filterState.ingredient?.let { "Ingredient: $it" } ?: "Ingredient ▾")
                    },
                    modifier = Modifier.testTag("filter_ingredient_chip")
                )
                DropdownMenu(
                    expanded = ingredientDropdownExpanded,
                    onDismissRequest = { ingredientDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Ingredients", fontWeight = FontWeight.Bold) },
                        onClick = {
                            ingredientDropdownExpanded = false
                            onFilterChange(filterState.copy(ingredient = null))
                        }
                    )
                    ingredients.forEach { ingredientName ->
                        DropdownMenuItem(
                            text = { Text(ingredientName) },
                            onClick = {
                                ingredientDropdownExpanded = false
                                onFilterChange(filterState.copy(ingredient = ingredientName))
                            }
                        )
                    }
                }
            }

            // Favourites-only Toggle Chip
            FilterChip(
                selected = filterState.favouritesOnly,
                onClick = {
                    onFilterChange(filterState.copy(favouritesOnly = !filterState.favouritesOnly))
                },
                label = { Text("Favourites") },
                leadingIcon = {
                    Icon(
                        imageVector = if (filterState.favouritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("filter_favourites_chip")
            )

            // Sort Toggle (A-Z / Z-A)
            ElevatedFilterChip(
                selected = filterState.sortOrder == SortOrder.NAME_DESC,
                onClick = {
                    val nextSort = if (filterState.sortOrder == SortOrder.NAME_ASC) {
                        SortOrder.NAME_DESC
                    } else {
                        SortOrder.NAME_ASC
                    }
                    onFilterChange(filterState.copy(sortOrder = nextSort))
                },
                label = {
                    Text(if (filterState.sortOrder == SortOrder.NAME_ASC) "A → Z" else "Z → A")
                },
                modifier = Modifier.testTag("filter_sort_chip")
            )

            // Clear All Button
            if (hasActiveFilters) {
                OutlinedButton(
                    onClick = {
                        onFilterChange(
                            FilterState(
                                query = "",
                                category = null,
                                ingredient = null,
                                favouritesOnly = false,
                                sortOrder = SortOrder.NAME_ASC
                            )
                        )
                    },
                    modifier = Modifier.testTag("filter_clear_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear all")
                }
            }
        }

        // 3. Active filter chips row reflecting active FilterState
        val activeChipsScrollState = rememberScrollState()
        AnimatedVisibility(
            visible = filterState.category != null ||
                    filterState.ingredient != null ||
                    filterState.favouritesOnly ||
                    filterState.sortOrder != SortOrder.NAME_ASC,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(activeChipsScrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterState.category?.let { cat ->
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filterState.copy(category = null)) },
                        label = { Text("Category: $cat") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove category filter",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("active_chip_category")
                    )
                }

                filterState.ingredient?.let { ing ->
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filterState.copy(ingredient = null)) },
                        label = { Text("Ingredient: $ing") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove ingredient filter",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("active_chip_ingredient")
                    )
                }

                if (filterState.favouritesOnly) {
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filterState.copy(favouritesOnly = false)) },
                        label = { Text("Favourites Only") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove favourites only filter",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("active_chip_favourites")
                    )
                }

                if (filterState.sortOrder == SortOrder.NAME_DESC) {
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filterState.copy(sortOrder = SortOrder.NAME_ASC)) },
                        label = { Text("Sorted: Z → A") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset sort order to A-Z",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("active_chip_sort")
                    )
                }
            }
        }
    }
}
