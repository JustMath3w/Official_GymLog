package com.example.gymlog_finale.data.model

// Modello dati per la sessione di allenamento effettivamente eseguita dall'utente.

data class WorkoutLog(
    val id: String = "",
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val completedAt: Long = System.currentTimeMillis(),
    val exercises: List<Exercise> = emptyList()
)
