package com.tasteindia.app.data.repository

import com.tasteindia.app.domain.model.Meal
import com.tasteindia.app.domain.model.MealDetail
import com.tasteindia.app.data.local.FavouriteEntity

interface MealRepository {
    suspend fun getIndianMeals(): Result<List<Meal>>
    suspend fun getMealDetail(id: String): Result<MealDetail>
    suspend fun filterByCategoryWithinIndia(category: String): Result<Set<String>>
    suspend fun filterByIngredientWithinIndia(ingredient: String): Result<Set<String>>
    suspend fun searchIndianMealsByName(name: String): Result<List<Meal>>
    suspend fun listCategories(): Result<List<String>>
    fun getFavouriteIds(): kotlinx.coroutines.flow.Flow<List<String>>
    fun getFavourites(): kotlinx.coroutines.flow.Flow<List<FavouriteEntity>>
    suspend fun toggleFavourite(meal: Meal)
    fun getCachedIndianMeals(): List<Meal>
}
