package com.tasteindia.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tasteindia.app.ui.details.DetailsScreen
import com.tasteindia.app.ui.details.DetailsViewModel
import com.tasteindia.app.ui.favourites.FavouritesScreen
import com.tasteindia.app.ui.favourites.FavouritesViewModel
import com.tasteindia.app.ui.recipes.RecipesScreen
import com.tasteindia.app.ui.recipes.RecipesViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom nav only on top-level destinations (Recipes and Favourites)
    val showBottomBar = currentRoute == Routes.Recipes.route || currentRoute == Routes.Favourites.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.Recipes.route,
                        onClick = {
                            if (currentRoute != Routes.Recipes.route) {
                                navController.navigate(Routes.Recipes.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Recipes tab"
                            )
                        },
                        label = { Text("Recipes") },
                        colors = NavigationBarItemDefaults.colors(),
                        modifier = Modifier.testTag("nav_item_recipes")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Routes.Favourites.route,
                        onClick = {
                            if (currentRoute != Routes.Favourites.route) {
                                navController.navigate(Routes.Favourites.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favourites tab"
                            )
                        },
                        label = { Text("Favourites") },
                        colors = NavigationBarItemDefaults.colors(),
                        modifier = Modifier.testTag("nav_item_favourites")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Recipes.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Recipes Destination
            composable(route = Routes.Recipes.route) { backStackEntry ->
                // Obtain RecipesViewModel scoped to the Recipes backStackEntry to retain its state across navigation
                val recipesViewModel: RecipesViewModel = hiltViewModel(backStackEntry)

                RecipesScreen(
                    viewModel = recipesViewModel,
                    onMealClick = { mealId ->
                        navController.navigate(Routes.Details.createRoute(mealId))
                    }
                )
            }

            // Details Destination
            composable(
                route = Routes.Details.route,
                arguments = listOf(
                    navArgument(Routes.Details.ARG_MEAL_ID) {
                        type = NavType.StringType
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                val detailsViewModel: DetailsViewModel = hiltViewModel(backStackEntry)

                DetailsScreen(
                    viewModel = detailsViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Favourites Destination
            composable(route = Routes.Favourites.route) { favBackStackEntry ->
                val favouritesViewModel: FavouritesViewModel = hiltViewModel(favBackStackEntry)

                FavouritesScreen(
                    viewModel = favouritesViewModel,
                    onMealClick = { mealId ->
                        navController.navigate(Routes.Details.createRoute(mealId))
                    }
                )
            }
        }
    }
}
