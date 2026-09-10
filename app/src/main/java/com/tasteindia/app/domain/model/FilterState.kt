package com.tasteindia.app.domain.model

enum class SortOrder {
    NAME_ASC,
    NAME_DESC
}

data class FilterState(
    val query: String = "",
    val category: String? = null,
    val ingredient: String? = null,
    val favouritesOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NAME_ASC
)
