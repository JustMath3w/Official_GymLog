package com.example.gymlog_finale.data.model

// Modello dati che rappresenta un alimento (nome e valori nutrizionali per 100 g) usato in ricerca e log pasti.

import java.util.UUID

// Data class FoodItem: aggregato immutabile di dati.
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "",
    val grams: Int = 0,
    val unit: String = "g",
    val calories: Int = 0,
    val carbs: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0
)
