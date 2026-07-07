package com.example.gymlog_finale.data.repository

// Repository che combina ExerciseDB, traduzione e YouTube per fornire una vista arricchita di ciascun esercizio.

import com.example.gymlog_finale.data.network.NetworkModule
import com.example.gymlog_finale.data.network.model.ExerciseDBItem
import android.util.Log

// Classe ExerciseRepository: unità principale definita in questo file.
class ExerciseRepository {
    private val api = NetworkModule.exerciseApi
    private val youtubeApi = NetworkModule.youtubeApi
    private val tag = "ExerciseRepository"

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    suspend fun getAllExercises(limit: Int = 50): List<ExerciseDBItem> {
        return try {
            api.getAllExercises(limit).map { sanitizeExercise(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching exercises", e)
            emptyList()
        }
    }

    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    suspend fun searchExercises(name: String, limit: Int = 20): List<ExerciseDBItem> {
        return try {
            api.searchExercisesByName(name, limit).map { sanitizeExercise(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error searching exercises", e)
            emptyList()
        }
    }

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    suspend fun getExerciseById(id: String): ExerciseDBItem? {
        return try {
            api.getExerciseById(id).let { sanitizeExercise(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching exercise by id: $id", e)
            null
        }
    }

    // Funzione di supporto interna alla classe.
    private fun sanitizeExercise(item: ExerciseDBItem): ExerciseDBItem {
        val finalId = item.exerciseId ?: item.id ?: ""

        val gif = if (!item.gifUrl.isNullOrEmpty()) {
            item.gifUrl?.replace("http://", "https://")
        } else if (finalId.isNotEmpty()) {
            "https://static.exercisedb.dev/media/$finalId.gif"
        } else {
            null
        }

        Log.d("ExerciseDB", "Final Asset -> ID: $finalId, URL: $gif")

        return item.copy(id = finalId, gifUrl = gif)
    }

    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    suspend fun searchYoutubeVideo(exerciseName: String): String? {
        return try {
            val query = "$exerciseName exercise tutorial"
            val response = youtubeApi.searchVideos(query)
            response.items.firstOrNull()?.id?.videoId
        } catch (e: Exception) {
            Log.e(tag, "Error searching YouTube video", e)
            null
        }
    }
}
