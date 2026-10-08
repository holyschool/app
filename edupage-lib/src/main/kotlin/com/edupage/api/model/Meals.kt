package com.edupage.api.model

import java.time.LocalDate

data class Meal(
    val mealId: String?,
    val name: String,
    val canOrder: Boolean = false,
    val isOrdered: Boolean = false,
    val allergens: List<String>? = null,
    val weight: String? = null,
)

data class MenuChoice(
    val letter: String,
    val name: String,
    val allergens: List<String>? = null,
    val weight: String? = null,
)

data class MealOrderInfo(
    val mealTypeIndex: String,
    val title: String,
    val orderedChoice: String?,
    val availableChoices: List<MenuChoice>,
    val canChangeUntil: String?,
    val servedFrom: String?,
    val servedTo: String?,
)

data class Meals(
    val date: LocalDate,
    val meals: List<Meal>,
    val mealOrderInfo: List<MealOrderInfo>? = null,
    val credit: String? = null,
    val boarderId: String? = null,
)

