package com.tasteindia.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tasteindia.app.ui.details.DetailsScreen
import com.tasteindia.app.ui.details.DetailsViewModel
import com.tasteindia.app.ui.recipes.RecipesScreen
import com.tasteindia.app.ui.recipes.RecipesViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Recipes.route,
        modifier = modifier.fillMaxSize()
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
            val parentEntry: NavBackStackEntry? = remember(favBackStackEntry) {
                runCatching { navController.getBackStackEntry(Routes.Recipes.route) }.getOrNull()
            }
            val recipesViewModel: RecipesViewModel = if (parentEntry != null) {
                hiltViewModel(parentEntry)
            } else {
                hiltViewModel(favBackStackEntry)
            }

            RecipesScreen(
                viewModel = recipesViewModel,
                onMealClick = { mealId ->
                    navController.navigate(Routes.Details.createRoute(mealId))
                }
            )
        }
    }
}
