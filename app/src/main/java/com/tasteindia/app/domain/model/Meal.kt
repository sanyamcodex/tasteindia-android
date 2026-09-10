package com.tasteindia.app.domain.model

data class Meal(
    val id: String,
    val name: String,
    val thumbUrl: String,
    val category: String? = null,
    val area: String? = null,
    val isFavourite: Boolean = false
)
