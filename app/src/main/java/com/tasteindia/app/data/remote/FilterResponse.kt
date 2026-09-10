package com.tasteindia.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FilterResponse(
    @Json(name = "meals") val meals: List<MealSummaryDto>? = null
)
