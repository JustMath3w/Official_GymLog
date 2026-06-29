package com.example.gymlog_finale.data.repository

import com.example.gymlog_finale.data.network.NetworkModule
import com.example.gymlog_finale.data.network.model.ExerciseDBItem
import android.util.Log

class ExerciseRepository {
    private val api = NetworkModule.exerciseApi
    private val youtubeApi = NetworkModule.youtubeApi
    private val tag = "ExerciseRepository"

    suspend fun getAllExercises(limit: Int = 50): List<ExerciseDBItem> {
        return try {
            api.getAllExercises(limit).map { sanitizeExercise(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching exercises", e)
            emptyList()
        }
    }

    suspend fun searchExercises(name: String, limit: Int = 20): List<ExerciseDBItem> {
        return try {
            api.searchExercisesByName(name, limit).map { sanitizeExercise(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error searching exercises", e)
            emptyList()
        }
    }

    suspend fun getExerciseById(id: String): ExerciseDBItem? {
        return try {
            api.getExerciseById(id).let { sanitizeExercise(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching exercise by id: $id", e)
            null
        }
    }

    private fun sanitizeExercise(item: ExerciseDBItem): ExerciseDBItem {
        val finalId = item.exerciseId ?: item.id ?: ""
        
        // Se l'API ci dà una GIF, usiamola forzando HTTPS
        // Altrimenti la costruiamo noi col nuovo dominio
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
