package com.example.gymlog_finale.ui.progress

import java.time.LocalDate

/**
 * Rappresenta una foto progresso mostrata nella UI con peso e data associati.
 */
data class ProgressPhotoItem(
    val id: String = "",
    val localPhotoUri: String = "",
    val weightKg: Double = 0.0,
    val date: LocalDate = LocalDate.now()
)

/**
 * Rappresenta un punto del grafico peso nel tempo.
 */
data class WeightChartPoint(
    val date: LocalDate,
    val weightKg: Double
)

/**
 * Rappresenta una statistica sintetica mostrata nella schermata progressi.
 */
data class ProgressStatItem(
    val title: String,
    val value: String
)

/**
 * Rappresenta un esercizio nella classifica dei più eseguiti.
 */
data class TopExerciseItem(
    val exerciseName: String,
    val executionCount: Int
)

/**
 * Identifica la metrica selezionata per il grafico esercizio.
 */
enum class ExerciseProgressMetric {
    WEIGHT,
    REPS
}

/**
 * Rappresenta un punto del grafico di progressione di un esercizio.
 */
data class ExerciseProgressPoint(
    val date: LocalDate,
    val value: Double
)

/**
 * Stato completo osservato dalla schermata Progressi.
 */
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