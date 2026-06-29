package com.example.gymlog_finale.data.model

/**
 * Statistiche aggregate mostrate nella scheda della community (es. quando guardi il profilo di un amico).
 */
data class FriendStats(
    val workoutStreakDays: Int = 0,         // Quanti giorni di fila si è allenato
    val dietStreakDays: Int = 0,            // Da quanti giorni di fila sta seguendo la dieta
    val favoriteExercise: String? = null,   // L'esercizio che esegue più spesso in assoluto
    val totalTrainingDays: Int = 0,         // Quanti giorni totali di allenamento ha completato
    val personalTrainerName: String? = null // Il nome del suo personal trainer (se ne ha uno)
)