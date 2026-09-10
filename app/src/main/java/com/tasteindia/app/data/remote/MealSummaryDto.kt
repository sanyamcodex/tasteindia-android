package com.tasteindia.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MealSummaryDto(
    @Json(name = "idMeal") val idMeal: String? = null,
    @Json(name = "strMeal") val strMeal: String? = null,
    @Json(name = "strMealThumb") val strMealThumb: String? = null
)
