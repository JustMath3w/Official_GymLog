package com.example.gymlog_finale.data.model

// Modello dati che sintetizza le statistiche di aderenza di un amico (streak medio, esercizi top).

data class FriendStats(
    val workoutStreakDays: Int = 0,
    val dietStreakDays: Int = 0,
    val favoriteExercise: String? = null,
    val totalTrainingDays: Int = 0,
    val personalTrainerName: String? = null
)