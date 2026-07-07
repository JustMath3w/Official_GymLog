package com.example.gymlog_finale.data.model

// Modello dati per un log di progresso: data, peso registrato ed eventuale foto associata.

data class ProgressLog(
    val id: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Double = 0.0,
    val photoUrl: String = ""
)