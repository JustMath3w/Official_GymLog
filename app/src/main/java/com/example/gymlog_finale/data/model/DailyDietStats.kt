package com.example.gymlog_finale.data.model

// Modello dati che aggrega calorie e macronutrienti consumati in una giornata di dieta.

data class DailyDietStats(
    val totalCalories: Int = 2000,
    val totalCarbs: Double = 250.0,
    val totalProteins: Double = 150.0,
    val totalFats: Double = 70.0,
    val foods: List<FoodItem> = emptyList()
) {

    val consumedCalories: Int get() = foods.sumOf { it.calories }
    val consumedCarbs: Double get() = foods.sumOf { it.carbs }
    val consumedProteins: Double get() = foods.sumOf { it.proteins }
    val consumedFats: Double get() = foods.sumOf { it.fats }
}
