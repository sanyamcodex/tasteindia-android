package com.tasteindia.app.ui.navigation

sealed class Routes(val route: String) {
    data object Recipes : Routes("recipes")

    data object Details : Routes("details/{mealId}") {
        const val ARG_MEAL_ID = "mealId"
        fun createRoute(mealId: String): String = "details/$mealId"
    }

    data object Favourites : Routes("favourites")
}
