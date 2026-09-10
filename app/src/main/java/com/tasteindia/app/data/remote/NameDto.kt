package com.tasteindia.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NameDto(
    @Json(name = "strCategory") val strCategory: String? = null,
    @Json(name = "strArea") val strArea: String? = null,
    @Json(name = "strIngredient") val strIngredient: String? = null
) {
    val name: String
        get() = strCategory ?: strArea ?: strIngredient.orEmpty()
}
