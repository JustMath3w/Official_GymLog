package com.example.gymlog_finale.ui.progress

// Data class immutabile che rappresenta lo stato UI della schermata Progressi.

import java.time.LocalDate

// Data class ProgressPhotoItem: aggregato immutabile di dati.
data class ProgressPhotoItem(
    val id: String = "",
    val localPhotoUri: String = "",
    val weightKg: Double = 0.0,
    val date: LocalDate = LocalDate.now()
)

// Data class WeightChartPoint: aggregato immutabile di dati.
data class WeightChartPoint(
    val date: LocalDate,
    val weightKg: Double
)

// Data class ProgressStatItem: aggregato immutabile di dati.
data class ProgressStatItem(
    val title: String,
    val value: String
)

// Data class TopExerciseItem: aggregato immutabile di dati.
data class TopExerciseItem(
    val exerciseName: String,
    val executionCount: Int
)

// Enum ExerciseProgressMetric: insieme finito di valori usati nell'app.
enum class ExerciseProgressMetric {
    WEIGHT,
    REPS
}

// Data class ExerciseProgressPoint: aggregato immutabile di dati.
data class ExerciseProgressPoint(
    val date: LocalDate,
    val value: Double
)

// Data class ProgressUiState: aggregato immutabile di dati.
data class ProgressUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val firstPhoto: ProgressPhotoItem? = null,
    val lastPhoto: ProgressPhotoItem? = null,
    val allPhotos: List<ProgressPhotoItem> = emptyList(),
    val weightChartPoints: List<WeightChartPoint> = emptyList(),
    val statsItems: List<ProgressStatItem> = emptyList(),
    val topExercises: List<TopExerciseItem> = emptyList(),
    val exerciseQuery: String = "",
    val selectedMetric: ExerciseProgressMetric = ExerciseProgressMetric.WEIGHT,
    val exerciseProgressPoints: List<ExerciseProgressPoint> = emptyList()
)