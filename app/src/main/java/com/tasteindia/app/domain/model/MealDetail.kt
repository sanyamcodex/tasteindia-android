package com.tasteindia.app.domain.model

data class MealDetail(
    val id: String,
    val name: String,
    val thumbUrl: String,
    val category: String? = null,
    val area: String? = null,
    val isFavourite: Boolean = false,
    val ingredients: List<IngredientLine> = emptyList(),
    val instructions: String = "",
    val tags: List<String> = emptyList(),
    val sourceUrl: String? = null,
    val videoUrl: String? = null
) {
    fun toMeal(): Meal = Meal(
        id = id,
        name = name,
        thumbUrl = thumbUrl,
        category = category,
        area = area,
        isFavourite = isFavourite
    )
}
