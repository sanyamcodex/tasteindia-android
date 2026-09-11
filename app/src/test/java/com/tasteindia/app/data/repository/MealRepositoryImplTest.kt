package com.tasteindia.app.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tasteindia.app.data.local.FavouriteDao
import com.tasteindia.app.data.local.FavouriteEntity
import com.tasteindia.app.data.remote.FilterResponse
import com.tasteindia.app.data.remote.ListResponse
import com.tasteindia.app.data.remote.LookupResponse
import com.tasteindia.app.data.remote.MealApiService
import com.tasteindia.app.data.remote.MealSummaryDto
import com.tasteindia.app.domain.model.AppError
import com.tasteindia.app.domain.model.Meal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MealRepositoryImplTest {

    private class FakeMealApiService : MealApiService {
        var filterByAreaResult: Result<FilterResponse> = Result.success(FilterResponse(meals = emptyList()))
        var filterByAreaCallCount = 0
        var filterByCategoryResult: Result<FilterResponse> = Result.success(FilterResponse(meals = emptyList()))
        var searchByNameResult: Result<FilterResponse> = Result.success(FilterResponse(meals = emptyList()))
        var lastSearchQuery: String? = null
        var searchByNameCallCount = 0

        override suspend fun filterByArea(area: String): FilterResponse {
            filterByAreaCallCount++
            return filterByAreaResult.getOrThrow()
        }

        override suspend fun filterByCategory(category: String): FilterResponse {
            return filterByCategoryResult.getOrThrow()
        }

        override suspend fun filterByIngredient(ingredient: String): FilterResponse {
            error("Not implemented")
        }

        override suspend fun lookupById(id: String): LookupResponse {
            error("Not implemented")
        }

        override suspend fun searchByName(name: String): FilterResponse {
            searchByNameCallCount++
            lastSearchQuery = name
            return searchByNameResult.getOrThrow()
        }

        override suspend fun listCategories(): ListResponse {
            error("Not implemented")
        }

        override suspend fun listAreas(): ListResponse {
            error("Not implemented")
        }
    }

    private class FakeFavouriteDao : FavouriteDao {
        override suspend fun insert(favourite: FavouriteEntity) = Unit
        override suspend fun delete(mealId: String) = Unit
        override fun getAllIds(): Flow<List<String>> = flowOf(emptyList())
        override fun getAll(): Flow<List<FavouriteEntity>> = flowOf(emptyList())
    }

    @Test
    fun getIndianMealsMapsSuccessfulInitialResponse() = runTest {
        val apiService = FakeMealApiService()
        val favouriteDao = FakeFavouriteDao()
        apiService.filterByAreaResult = Result.success(
            FilterResponse(
                meals = listOf(MealSummaryDto("101", "Indian One", "thumb"))
            )
        )

        val result = MealRepositoryImpl(apiService, favouriteDao).getIndianMeals()

        assertEquals(
            listOf(Meal("101", "Indian One", "thumb")),
            result.getOrThrow()
        )
        assertEquals(1, apiService.filterByAreaCallCount)
    }

    @Test
    fun nullIndianMealsResponseFallsBackToCheckedInFixture() = runTest {
        val apiService = FakeMealApiService()
        val favouriteDao = FakeFavouriteDao()
        apiService.filterByAreaResult = Result.success(FilterResponse(meals = null))

        val result = MealRepositoryImpl(apiService, favouriteDao).getIndianMeals()

        assertTrue(result.isSuccess)
        val meals = result.getOrThrow()
        // Fixture contains 101, 202, 303 Indian meals
        assertEquals(listOf("101", "202", "303"), meals.map { it.id })
        assertEquals("Indian One", meals[0].name)
    }

    @Test
    fun errorOrOfflineIndianMealsResponseFallsBackToCheckedInFixture() = runTest {
        val apiService = FakeMealApiService()
        val favouriteDao = FakeFavouriteDao()
        apiService.filterByAreaResult = Result.failure(IOException("Network unreachable"))

        val result = MealRepositoryImpl(apiService, favouriteDao).getIndianMeals()

        assertTrue(result.isSuccess)
        val meals = result.getOrThrow()
        assertEquals(listOf("101", "202", "303"), meals.map { it.id })
    }

    @Test
    fun retryAfterFailureFetchesFromApiWhenAvailable() = runTest {
        val apiService = FakeMealApiService()
        val favouriteDao = FakeFavouriteDao()
        apiService.filterByAreaResult = Result.success(
            FilterResponse(
                meals = listOf(MealSummaryDto("999", "Special Indian Dish", "thumb_999"))
            )
        )

        val repository = MealRepositoryImpl(apiService, favouriteDao)
        val result = repository.getIndianMeals()

        assertTrue(result.isSuccess)
        assertEquals(listOf("999"), result.getOrThrow().map { it.id })
        assertEquals(1, apiService.filterByAreaCallCount)
    }

    @Test
    fun filterByCategoryWithinIndiaReturnsOnlyIndianCategoryOverlap() = runTest {
        val apiService = FakeMealApiService()
        val favouriteDao = FakeFavouriteDao()
        val indianMeals = readFilterFixture("/fixtures/indian_meals_filter.json")
        val categoryMeals = readFilterFixture("/fixtures/category_meals_filter.json")
        apiService.filterByAreaResult = Result.success(indianMeals)
        apiService.filterByCategoryResult = Result.success(categoryMeals)
        val repository = MealRepositoryImpl(apiService, favouriteDao)

        val result = repository.filterByCategoryWithinIndia("Chicken")

        assertEquals(setOf("202"), result.getOrThrow())
    }

    @Test
    fun searchIndianMealsByNameCallsSearchEndpointAndEnforcesIndianBoundary() = runTest {
        val apiService = FakeMealApiService()
        val favouriteDao = FakeFavouriteDao()
        apiService.filterByAreaResult = Result.success(
            FilterResponse(
                meals = listOf(MealSummaryDto("1", "Indian Pasta", ""))
            )
        )
        apiService.searchByNameResult = Result.success(
            FilterResponse(
                meals = listOf(
                    MealSummaryDto("1", "Indian Pasta", ""),
                    MealSummaryDto("2", "Italian Pasta", "") // Non-Indian meal
                )
            )
        )
        val repository = MealRepositoryImpl(apiService, favouriteDao)

        val result = repository.searchIndianMealsByName("pasta")

        // Only meal 1 is in the Indian boundary, meal 2 (non-Indian) is excluded
        assertEquals(listOf("1"), result.getOrThrow().map { it.id })
        assertEquals("pasta", apiService.lastSearchQuery)
        assertEquals(1, apiService.searchByNameCallCount)
    }

    private fun readFilterFixture(path: String): FilterResponse {
        val json = checkNotNull(javaClass.getResourceAsStream(path))
            .bufferedReader()
            .use { it.readText() }
        return checkNotNull(
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(FilterResponse::class.java)
                .fromJson(json)
        )
    }
}
