package com.example.gymlog_finale.data.model

/**
 * A differenza di "Workout" (che è solo la scheda base), questo modello "WorkoutLog" 
 * rappresenta un allenamento che è stato REALMENTE FATTO e completato nello storico.
 */
data class WorkoutLog(
    val id: String = "",                             // ID unico di questo storico
    val userId: String = "",                         // Chi ha sudato e completato l'allenamento
    val workoutId: String = "",                      // L'ID della scheda "Workout" originale da cui si è partiti
    val workoutName: String = "",                    // Il nome di quell'allenamento
    val completedAt: Long = System.currentTimeMillis(), // Il momento esatto in cui si è premuto "Fine Allenamento"
    val exercises: List<Exercise> = emptyList()      // I pesi/ripetizioni che l'utente ha EFFETTIVAMENTE sollevato
)
