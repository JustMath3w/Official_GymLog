package com.example.gymlog_finale.data.model

/**
 * Rappresenta le statistiche e gli alimenti consumati in un singolo giorno della dieta.
 */
data class DailyDietStats(
    val totalCalories: Int = 2000,          // Obiettivo o totale delle calorie (Kcal)
    val totalCarbs: Double = 250.0,         // Obiettivo o totale dei carboidrati (Grammi)
    val totalProteins: Double = 150.0,      // Obiettivo o totale delle proteine (Grammi)
    val totalFats: Double = 70.0,           // Obiettivo o totale dei grassi (Grammi)
    val foods: List<FoodItem> = emptyList() // Lista di tutti i cibi mangiati in questo giorno
) {
    // Valori calcolati automaticamente: sommano i nutrienti di ogni singolo cibo nella lista "foods"
    val consumedCalories: Int get() = foods.sumOf { it.calories }
    val consumedCarbs: Double get() = foods.sumOf { it.carbs }
    val consumedProteins: Double get() = foods.sumOf { it.proteins }
    val consumedFats: Double get() = foods.sumOf { it.fats }
}
