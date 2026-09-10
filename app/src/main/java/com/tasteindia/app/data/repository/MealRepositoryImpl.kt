package com.tasteindia.app.data.repository

import com.tasteindia.app.data.local.FavouriteDao
import com.tasteindia.app.data.local.FavouriteEntity
import com.tasteindia.app.data.remote.MealApiService
import com.tasteindia.app.data.remote.toDomain
import com.tasteindia.app.domain.model.AppError
import com.tasteindia.app.domain.model.Meal
import com.tasteindia.app.domain.model.MealDetail
import com.tasteindia.app.domain.model.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepositoryImpl @Inject constructor(
    private val apiService: MealApiService,
    private val favouriteDao: FavouriteDao
) : MealRepository {

    // Indian boundary ID set cached in-memory for this session
    private val indianIdsMutex = Mutex()
    private val cachedIndianMealIds = mutableSetOf<String>()
    private val cachedIndianMeals = mutableListOf<Meal>()

    // Meal detail in-memory cache
    private val detailCache = ConcurrentHashMap<String, MealDetail>()

    // In-flight request deduplication map protected by mutex
    private val inflightMutex = Mutex()
    private val inflightDetails = mutableMapOf<String, Deferred<MealDetail>>()
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun getIndianMeals(): Result<List<Meal>> {
        indianIdsMutex.withLock {
            if (cachedIndianMeals.isNotEmpty()) {
                return Result.success(cachedIndianMeals.toList())
            }
        }

        val result = safeApiCall {
            val response = apiService.filterByArea("Indian")
            val list = response.meals?.map { it.toDomain() } ?: emptyList()
            list
        }

        return result.map { meals ->
            indianIdsMutex.withLock {
                cachedIndianMeals.clear()
                cachedIndianMeals.addAll(meals)
                cachedIndianMealIds.clear()
                cachedIndianMealIds.addAll(meals.map { it.id })
            }
            meals
        }
    }

    private suspend fun ensureIndianBoundary(): Result<Set<String>> {
        indianIdsMutex.withLock {
            if (cachedIndianMealIds.isNotEmpty()) {
                return Result.success(cachedIndianMealIds.toSet())
            }
        }
        val indianMealsResult = getIndianMeals()
        return indianMealsResult.map { meals ->
            meals.map { it.id }.toSet()
        }
    }

    override suspend fun getMealDetail(id: String): Result<MealDetail> {
        detailCache[id]?.let {
            return Result.success(it)
        }

        // Deduplicate in-flight requests for the same meal ID
        val deferred: Deferred<MealDetail> = inflightMutex.withLock {
            detailCache[id]?.let {
                return Result.success(it)
            }

            inflightDetails.getOrPut(id) {
                requestScope.async {
                    try {
                        val detail = safeApiCall {
                            val response = apiService.lookupById(id)
                            val dto = response.meals?.firstOrNull()
                                ?: throw AppError.Unknown("No meal detail found for ID $id")
                            dto.toDomain()
                        }.getOrThrow()
                        detailCache[id] = detail
                        detail
                    } finally {
                        inflightMutex.withLock {
                            inflightDetails.remove(id)
                        }
                    }
                }
            }
        }

        return safeApiCall { deferred.await() }
    }

    override suspend fun filterByCategoryWithinIndia(category: String): Result<Set<String>> {
        val boundaryResult = ensureIndianBoundary()
        val indianSet = boundaryResult.getOrElse { return Result.failure(it) }

        val categoryResult = safeApiCall {
            val response = apiService.filterByCategory(category)
            response.meals?.mapNotNull { it.idMeal }?.toSet() ?: emptySet()
        }

        return categoryResult.map { categoryIds ->
            categoryIds.intersect(indianSet)
        }
    }

    override suspend fun filterByIngredientWithinIndia(ingredient: String): Result<Set<String>> {
        val boundaryResult = ensureIndianBoundary()
        val indianSet = boundaryResult.getOrElse { return Result.failure(it) }

        val ingredientResult = safeApiCall {
            val response = apiService.filterByIngredient(ingredient)
            response.meals?.mapNotNull { it.idMeal }?.toSet() ?: emptySet()
        }

        return ingredientResult.map { ingredientIds ->
            ingredientIds.intersect(indianSet)
        }
    }

    override suspend fun searchIndianMealsByName(name: String): Result<List<Meal>> {
        val boundaryResult = ensureIndianBoundary()
        val indianSet = boundaryResult.getOrElse { return Result.failure(it) }

        val searchResult = safeApiCall {
            val response = apiService.searchByName(name)
            response.meals?.map { it.toDomain() } ?: emptyList()
        }

        return searchResult.map { searchMeals ->
            searchMeals.filter { meal -> indianSet.contains(meal.id) }
        }
    }

    override suspend fun listCategories(): Result<List<String>> {
        return safeApiCall {
            val response = apiService.listCategories()
            response.meals?.mapNotNull { it.name.takeIf { n -> n.isNotBlank() } } ?: emptyList()
        }
    }

    override fun getFavouriteIds(): Flow<List<String>> {
        return favouriteDao.getAllIds()
    }

    override fun getFavourites(): Flow<List<FavouriteEntity>> {
        return favouriteDao.getAll()
    }

    override suspend fun toggleFavourite(meal: Meal) {
        val currentIds = favouriteDao.getAllIds().first()
        if (currentIds.contains(meal.id)) {
            favouriteDao.delete(meal.id)
        } else {
            favouriteDao.insert(
                FavouriteEntity(
                    mealId = meal.id,
                    name = meal.name,
                    thumbUrl = meal.thumbUrl
                )
            )
        }
    }

    override fun getCachedIndianMeals(): List<Meal> {
        val detailMeals = detailCache.values.map { it.toMeal() }
        val indianMeals = synchronized(cachedIndianMeals) { cachedIndianMeals.toList() }
        val merged = (indianMeals + detailMeals).distinctBy { it.id }
        return merged
    }
}
