package com.tasteindia.app.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tasteindia.app.data.local.FavouriteDao
import com.tasteindia.app.data.remote.FilterResponse
import com.tasteindia.app.data.remote.MealApiService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MealRepositoryImplTest {

    @Test
    fun filterByCategoryWithinIndiaReturnsOnlyIndianCategoryOverlap() = runTest {
        val apiService = mockk<MealApiService>()
        val favouriteDao = mockk<FavouriteDao>()
        val indianMeals = readFilterFixture("/fixtures/indian_meals_filter.json")
        val categoryMeals = readFilterFixture("/fixtures/category_meals_filter.json")
        every { favouriteDao.getAllIds() } returns flowOf(emptyList())
        coEvery { apiService.filterByArea("Indian") } returns indianMeals
        coEvery { apiService.filterByCategory("Chicken") } returns categoryMeals
        val repository = MealRepositoryImpl(apiService, favouriteDao)

        val result = repository.filterByCategoryWithinIndia("Chicken")

        assertEquals(setOf("202"), result.getOrThrow())
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
