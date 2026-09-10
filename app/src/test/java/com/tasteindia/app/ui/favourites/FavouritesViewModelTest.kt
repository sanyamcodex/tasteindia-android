package com.tasteindia.app.ui.favourites

import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tasteindia.app.data.local.FavouriteDao
import com.tasteindia.app.data.local.FavouriteEntity
import com.tasteindia.app.data.remote.MealApiService
import com.tasteindia.app.data.remote.MealSummaryDto
import com.tasteindia.app.data.repository.MealRepositoryImpl
import com.tasteindia.app.domain.model.Meal
import app.cash.turbine.test
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FavouritesViewModelTest {

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
    fun togglingFavouriteUpdatesStateFromFakeDaoWithoutNetworkCall() = runTest(dispatcher) {
        val apiService = mockk<MealApiService>()
        val dao = FakeFavouriteDao()
        val repository = MealRepositoryImpl(apiService, dao)
        val viewModel = FavouritesViewModel(repository)
        val meal = readMealFixture()

        viewModel.favouriteMeals.test {
            assertEquals(emptyList<Meal>(), awaitItem())

            viewModel.toggleFavourite(meal)
            advanceUntilIdle()

            assertEquals(listOf(meal.copy(isFavourite = true)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        confirmVerified(apiService)
    }

    private fun readMealFixture(): Meal {
        val json = checkNotNull(javaClass.getResourceAsStream("/fixtures/favourite_meal.json"))
            .bufferedReader()
            .use { it.readText() }
        val dto = checkNotNull(
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(MealSummaryDto::class.java)
                .fromJson(json)
        )
        return Meal(
            id = dto.idMeal.orEmpty(),
            name = dto.strMeal.orEmpty(),
            thumbUrl = dto.strMealThumb.orEmpty()
        )
    }

    private class FakeFavouriteDao : FavouriteDao {
        private val favourites = MutableStateFlow<List<FavouriteEntity>>(emptyList())

        override suspend fun insert(favourite: FavouriteEntity) {
            favourites.value = favourites.value
                .filterNot { it.mealId == favourite.mealId } + favourite
        }

        override suspend fun delete(mealId: String) {
            favourites.value = favourites.value.filterNot { it.mealId == mealId }
        }

        override fun getAllIds(): Flow<List<String>> = favourites.map { rows ->
            rows.map { it.mealId }
        }

        override fun getAll(): Flow<List<FavouriteEntity>> = favourites
    }
}
