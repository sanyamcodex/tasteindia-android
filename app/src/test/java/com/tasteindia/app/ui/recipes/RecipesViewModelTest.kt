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
import com.tasteindia.app.domain.model.AppError
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecipesViewModelTest {

    private lateinit var dispatcher: TestDispatcher

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

    @Test
    fun typingSearchStatePersistsWhileUiStateIsLoading() = runTest(dispatcher) {
        val viewModel = RecipesViewModel(
            repository = FakeMealRepository(readSearchFixture()),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.filterState.test {
            awaitItem()
            viewModel.applyFilters(FilterState(query = "pasta"))
            runCurrent()

            assertEquals("pasta", awaitItem().query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptySearchRestoresIndianBrowseResults() = runTest(dispatcher) {
        val meals = readSearchFixture()
        val viewModel = RecipesViewModel(
            repository = FakeMealRepository(meals),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.uiState.test {
            awaitItem()
            viewModel.applyFilters(FilterState(query = "paneer"))
            advanceTimeBy(301)
            advanceUntilIdle()
            awaitSuccess()

            viewModel.applyFilters(FilterState())
            advanceTimeBy(301)
            advanceUntilIdle()

            val browseState = awaitSuccess()
            assertEquals(meals.map { it.id }.sorted(), browseState.meals.map { it.id }.sorted())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyIndianApiResponseShowsBrowseEmptyState() = runTest(dispatcher) {
        val viewModel = RecipesViewModel(
            repository = FakeMealRepository(emptyList()),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()

            var state: RecipesUiState
            do {
                state = awaitItem()
            } while (state !is RecipesUiState.Empty)

            assertEquals("No Indian recipes available", state.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retryAfterInitialErrorLoadsIndianBrowseResults() = runTest(dispatcher) {
        val meals = readSearchFixture()
        val viewModel = RecipesViewModel(
            repository = FakeMealRepository(
                results = mutableListOf(
                    Result.failure(AppError.Network("offline")),
                    Result.success(meals)
                )
            ),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()

            val errorState = awaitRecipesError()
            assertEquals("No internet connection. Please check your network.", errorState.message)

            viewModel.retry()
            advanceUntilIdle()

            val successState = awaitSuccess()
            assertEquals(meals.map { it.id }.sorted(), successState.meals.map { it.id }.sorted())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<RecipesUiState>.awaitSuccess(): RecipesUiState.Success {
        while (true) {
            when (val state = awaitItem()) {
                is RecipesUiState.Success -> return state
                is RecipesUiState.Error -> error("Unexpected error state: ${state.message}")
                is RecipesUiState.Empty -> error("Unexpected empty state: ${state.message}")
                RecipesUiState.Loading -> Unit
            }
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<RecipesUiState>.awaitRecipesError(): RecipesUiState.Error {
        while (true) {
            when (val state = awaitItem()) {
                is RecipesUiState.Error -> return state
                RecipesUiState.Loading -> Unit
                is RecipesUiState.Success -> error("Unexpected success state")
                is RecipesUiState.Empty -> error("Unexpected empty state")
            }
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
        private val meals: List<Meal> = emptyList(),
        private val results: MutableList<Result<List<Meal>>> = mutableListOf(Result.success(meals))
    ) : MealRepository {
        override suspend fun getIndianMeals(): Result<List<Meal>> = results.removeAt(0)
        override suspend fun getMealDetail(id: String): Result<MealDetail> = error("Not used")
        override suspend fun filterByCategoryWithinIndia(category: String): Result<Set<String>> = Result.success(emptySet())
        override suspend fun filterByIngredientWithinIndia(ingredient: String): Result<Set<String>> = Result.success(emptySet())
        override suspend fun searchIndianMealsByName(name: String): Result<List<Meal>> = Result.success(
            meals.filter { it.name.contains(name, ignoreCase = true) }
        )
        override suspend fun listCategories(): Result<List<String>> = Result.success(emptyList())
        override fun getFavouriteIds(): Flow<List<String>> = flowOf(emptyList())
        override fun getFavourites(): Flow<List<FavouriteEntity>> = flowOf(emptyList())
        override suspend fun toggleFavourite(meal: Meal) = Unit
        override fun getCachedIndianMeals(): List<Meal> = emptyList()
    }
}
