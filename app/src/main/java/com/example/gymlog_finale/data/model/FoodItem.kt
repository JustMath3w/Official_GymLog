package com.example.gymlog_finale.data.model

import java.util.UUID

/**
 * Rappresenta un singolo alimento inserito nella dieta dell'utente.
 */
data class FoodItem(
    val id: String = UUID.randomUUID().toString(), // Genera un ID casuale e univoco per ogni cibo
    val name: String = "",                         // Il nome dell'alimento (es. "Pollo", "Mela")
    val category: String = "",                     // La categoria (es. "Colazione", "Pranzo", "Snack")
    val grams: Int = 0,                            // Quantità consumata
    val unit: String = "g",                        // Unità di misura (default in grammi)
    val calories: Int = 0,                         // Calorie apportate da questa quantità
    val carbs: Double = 0.0,                       // Carboidrati apportati da questa quantità
    val proteins: Double = 0.0,                    // Proteine apportate da questa quantità
    val fats: Double = 0.0                         // Grassi apportati da questa quantità
)
