package com.edupage.api.model

import java.time.LocalDate

data class Meal(
    val mealId: String?,
    val name: String,
    val canOrder: Boolean,
    val isOrdered: Boolean,
    val allergens: List<String>?,
    val weight: String?
)

data class Meals(
    val date: LocalDate,
    val meals: List<Meal>
)
