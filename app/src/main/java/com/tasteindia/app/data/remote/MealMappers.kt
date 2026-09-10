package com.tasteindia.app.data.remote

import com.tasteindia.app.domain.model.IngredientLine
import com.tasteindia.app.domain.model.Meal
import com.tasteindia.app.domain.model.MealDetail

fun MealSummaryDto.toDomain(): Meal {
    return Meal(
        id = idMeal.orEmpty(),
        name = strMeal.orEmpty(),
        thumbUrl = strMealThumb.orEmpty(),
        category = null,
        area = null,
        isFavourite = false
    )
}

fun MealDetailDto.toDomain(): MealDetail {
    val ingredientAccessors = listOf(
        strIngredient1 to strMeasure1,
        strIngredient2 to strMeasure2,
        strIngredient3 to strMeasure3,
        strIngredient4 to strMeasure4,
        strIngredient5 to strMeasure5,
        strIngredient6 to strMeasure6,
        strIngredient7 to strMeasure7,
        strIngredient8 to strMeasure8,
        strIngredient9 to strMeasure9,
        strIngredient10 to strMeasure10,
        strIngredient11 to strMeasure11,
        strIngredient12 to strMeasure12,
        strIngredient13 to strMeasure13,
        strIngredient14 to strMeasure14,
        strIngredient15 to strMeasure15,
        strIngredient16 to strMeasure16,
        strIngredient17 to strMeasure17,
        strIngredient18 to strMeasure18,
        strIngredient19 to strMeasure19,
        strIngredient20 to strMeasure20
    )

    val ingredients = ingredientAccessors.mapNotNull { (ingredient, measure) ->
        val trimmedIngredient = ingredient?.trim()
        if (trimmedIngredient.isNullOrBlank()) {
            null
        } else {
            IngredientLine(
                name = trimmedIngredient,
                measure = measure?.trim().orEmpty()
            )
        }
    }

    val parsedTags = strTags
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    return MealDetail(
        id = idMeal.orEmpty(),
        name = strMeal.orEmpty(),
        thumbUrl = strMealThumb.orEmpty(),
        category = strCategory?.trim()?.takeIf { it.isNotBlank() },
        area = strArea?.trim()?.takeIf { it.isNotBlank() },
        isFavourite = false,
        ingredients = ingredients,
        instructions = strInstructions?.trim().orEmpty(),
        tags = parsedTags,
        sourceUrl = strSource?.trim()?.takeIf { it.isNotBlank() },
        videoUrl = strYoutube?.trim()?.takeIf { it.isNotBlank() }
    )
}
