package com.tasteindia.app.ui.recipes

import androidx.lifecycle.SavedStateHandle
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tasteindia.app.data.local.FavouriteEntity
import com.tasteindia.app.data.repository.MealRepository
import com.tasteindia.app.data.remote.FilterResponse
import com.tasteindia.app.data.remote.toDomain
import com.tasteindia.app.domain.model.FilterState
import com.tasteindia.app.domain.model.Meal
import com.tasteindia.app.domain.model.MealDetail
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecipesViewModelTest {

    private lateinit var dispatcher: StandardTestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rapidSequentialSearchQueriesOnlyLatestResultReachesUiState() = runTest(dispatcher) {
        val viewModel = RecipesViewModel(
            repository = FakeMealRepository(readSearchFixture()),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.uiState.test {
            awaitItem()
            viewModel.applyFilters(FilterState(query = "chicken"))
            viewModel.applyFilters(FilterState(query = "paneer"))
            advanceTimeBy(301)
            advanceUntilIdle()

            val successStates = mutableListOf<RecipesUiState.Success>()
            while (successStates.isEmpty()) {
                when (val state = awaitItem()) {
                    is RecipesUiState.Success -> successStates += state
                    is RecipesUiState.Empty -> error("Latest query unexpectedly returned empty")
                    is RecipesUiState.Error -> error("Latest query unexpectedly failed")
                    RecipesUiState.Loading -> Unit
                }
            }

            assertEquals(listOf("Paneer Tikka"), successStates.last().meals.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun readSearchFixture(): List<Meal> {
        val json = checkNotNull(javaClass.getResourceAsStream("/fixtures/search_meals.json"))
            .bufferedReader()
            .use { it.readText() }
        val response = checkNotNull(
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(FilterResponse::class.java)
                .fromJson(json)
        )
        return response.meals.orEmpty().map { it.toDomain() }
    }

    private class FakeMealRepository(
        private val meals: List<Meal>
    ) : MealRepository {
        override suspend fun getIndianMeals(): Result<List<Meal>> = Result.success(meals)
        override suspend fun getMealDetail(id: String): Result<MealDetail> = error("Not used")
        override suspend fun filterByCategoryWithinIndia(category: String): Result<Set<String>> = Result.success(emptySet())
        override suspend fun filterByIngredientWithinIndia(ingredient: String): Result<Set<String>> = Result.success(emptySet())
        override suspend fun searchIndianMealsByName(name: String): Result<List<Meal>> = Result.success(emptyList())
        override suspend fun listCategories(): Result<List<String>> = Result.success(emptyList())
        override fun getFavouriteIds(): Flow<List<String>> = flowOf(emptyList())
        override fun getFavourites(): Flow<List<FavouriteEntity>> = flowOf(emptyList())
        override suspend fun toggleFavourite(meal: Meal) = Unit
        override fun getCachedIndianMeals(): List<Meal> = emptyList()
    }
}
