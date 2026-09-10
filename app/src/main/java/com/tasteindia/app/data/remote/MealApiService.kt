package com.tasteindia.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {

    @GET("filter.php")
    suspend fun filterByArea(
        @Query("a") area: String = "Indian"
    ): FilterResponse

    @GET("filter.php")
    suspend fun filterByCategory(
        @Query("c") category: String
    ): FilterResponse

    @GET("filter.php")
    suspend fun filterByIngredient(
        @Query("i") ingredient: String
    ): FilterResponse

    @GET("lookup.php")
    suspend fun lookupById(
        @Query("i") id: String
    ): LookupResponse

    @GET("search.php")
    suspend fun searchByName(
        @Query("s") name: String
    ): FilterResponse

    @GET("list.php?c=list")
    suspend fun listCategories(): ListResponse

    @GET("list.php?a=list")
    suspend fun listAreas(): ListResponse
}
