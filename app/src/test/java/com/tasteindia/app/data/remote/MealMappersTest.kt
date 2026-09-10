package com.tasteindia.app.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class MealMappersTest {

    @Test
    fun mealDetailDtoWithIngredientGapsMapsOrderedNonBlankIngredients() {
        val fixture = checkNotNull(javaClass.getResourceAsStream("/fixtures/meal_detail_gaps.json"))
            .bufferedReader()
            .use { it.readText() }
        val dto = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter(MealDetailDto::class.java)
            .fromJson(fixture)

        val ingredients = checkNotNull(dto).toDomain().ingredients

        assertEquals(
            listOf(
                "Chicken" to "500 g",
                "Garam masala" to "",
                "Coriander" to "1 tbsp",
                "Salt" to "to taste"
            ),
            ingredients.map { it.name to it.measure }
        )
    }
}
