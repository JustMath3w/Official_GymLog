package com.example.gymlog_finale.ui.progress

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.firebase.FirebaseAuthSource
import com.example.gymlog_finale.data.firebase.FirebaseProgressSource
import com.example.gymlog_finale.data.firebase.FirebaseUserSource
import com.example.gymlog_finale.data.model.ProgressLog
import com.example.gymlog_finale.data.repository.WorkoutRepository
import com.example.gymlog_finale.domain.usecase.GetProgressLogsUseCase
import com.example.gymlog_finale.data.model.DailyDietStats
import com.example.gymlog_finale.data.model.WorkoutLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Gestisce lo stato della schermata Progressi recuperando i dati reali
 * dai log peso/foto, dai workout logs completati e dai giorni dieta validi.
 */
class ProgressViewModel : ViewModel() {

    private val authSource = FirebaseAuthSource()
    private val progressRepository = FirebaseProgressSource()
    private val userRepository = FirebaseUserSource()
    private val workoutRepository = WorkoutRepository()
    private val getProgressLogsUseCase = GetProgressLogsUseCase(progressRepository)
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProgressUiState(isLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var cachedProgressLogs: List<ProgressLog> = emptyList()
    private var cachedWorkoutLogs: List<WorkoutLog> = emptyList()
    private var cachedDietQualifiedDays: List<LocalDate> = emptyList()

    init {
        loadProgressData()
        observeWorkoutLogs()
        loadDietProgressData()
    }

    /**
     * Aggiorna il testo di ricerca esercizio nella UI.
     */
    fun onExerciseQueryChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                exerciseQuery = value,
                errorMessage = null
            )
        }
    }

    /**
     * Aggiorna la metrica selezionata per il grafico esercizio.
     */
    fun onMetricSelected(metric: ExerciseProgressMetric) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedMetric = metric
            )
        }
    }

    /**
     * Recupera il peso corrente dal profilo utente per precompilare il dialog di inserimento progresso.
     */
    fun loadCurrentProfileWeight(onLoaded: (Double) -> Unit) {
        viewModelScope.launch {
            val userId = authSource.getCurrentUserId()

            if (userId.isNullOrBlank()) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = "Utente non autenticato.")
                }
                return@launch
            }

            userRepository.getUser(userId).fold(
                onSuccess = { user ->
                    onLoaded(user.peso)
                },
                onFailure = { error ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = error.message ?: "Impossibile recuperare il peso profilo."
                        )
                    }
                }
            )
        }
    }

    /**
     * Cerca i progressi reali di un esercizio usando i workout logs completati.
     */
    fun searchExerciseProgress() {
        val query = _uiState.value.exerciseQuery.trim()

        if (query.isBlank()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Inserisci il nome di un esercizio."
                )
            }
            return
        }

        val points = cachedWorkoutLogs
            .sortedBy { it.completedAt }
            .mapNotNull { log ->
                val matchingExercises = log.exercises.filter { exercise ->
                    exercise.name.contains(query, ignoreCase = true)
                }

                if (matchingExercises.isEmpty()) {
                    null
                } else {
                    val value = when (_uiState.value.selectedMetric) {
                        ExerciseProgressMetric.WEIGHT -> {
                            matchingExercises.maxOfOrNull { exercise ->
                                parseMaxNumericValue(exercise.weight)
                            } ?: 0.0
                        }

                        ExerciseProgressMetric.REPS -> {
                            matchingExercises.maxOfOrNull { exercise ->
                                parseMaxNumericValue(exercise.reps)
                            } ?: 0.0
                        }
                    }

                    if (value <= 0.0) {
                        null
                    } else {
                        ExerciseProgressPoint(
                            date = log.completedAt.toLocalDate(),
                            value = value
                        )
                    }
                }
            }

        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = if (points.isEmpty()) {
                    "Nessun dato trovato per l'esercizio \"$query\"."
                } else {
                    null
                },
                exerciseProgressPoints = points
            )
        }
    }

    /**
     * Salva un nuovo progresso usando il peso inserito e una foto opzionale.
     */
    fun addProgressLog(
        context: Context,
        weightText: String,
        photoUri: Uri?
    ) {
        val userId = authSource.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Utente non autenticato."
                )
            }
            return
        }

        val parsedWeight = weightText
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        if (parsedWeight == null || parsedWeight <= 0.0) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Inserisci un peso valido maggiore di 0."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            progressRepository.addProgressLogWithPhoto(
                context = context,
                userId = userId,
                weightKg = parsedWeight,
                localPhotoUri = photoUri
            ).fold(
                onSuccess = {
                    userRepository.updateUserFields(
                        uid = userId,
                        fields = mapOf("peso" to parsedWeight)
                    ).fold(
                        onSuccess = {
                            loadProgressData()
                        },
                        onFailure = { error ->
                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    errorMessage = error.message
                                        ?: "Progresso salvato, ma il peso profilo non è stato aggiornato."
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Errore durante il salvataggio del progresso."
                        )
                    }
                }
            )
        }
    }

    /**
     * Carica i log reali di peso/foto dell'utente autenticato e aggiorna lo stato UI.
     */
    fun loadProgressData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userId = authSource.getCurrentUserId()

            if (userId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Utente non autenticato."
                    )
                }
                return@launch
            }

            val result = getProgressLogsUseCase(userId)

            result.fold(
                onSuccess = { logs ->
                    cachedProgressLogs = logs.sortedBy { it.timestamp }
                    applyCombinedUiState()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Errore nel caricamento dei progressi."
                        )
                    }
                }
            )
        }
    }

    /**
     * Carica i giorni dieta dell'utente e seleziona quelli con almeno 3 categorie diverse.
     */
    private fun loadDietProgressData() {
        viewModelScope.launch {
            val userId = authSource.getCurrentUserId()

            if (userId.isNullOrBlank()) {
                cachedDietQualifiedDays = emptyList()
                applyCombinedUiState()
                return@launch
            }

            try {
                val snapshot = firestore
                    .collection("CalendarDiet")
                    .document(userId)
                    .collection("days")
                    .get()
                    .await()

                cachedDietQualifiedDays = snapshot.documents.mapNotNull { document ->
                    val stats = document.toObject(DailyDietStats::class.java) ?: return@mapNotNull null

                    val distinctCategories = stats.foods
                        .map { it.category.trim().lowercase() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (distinctCategories.size >= 3) {
                        document.id.toLocalDateFromDietDocumentId()
                    } else {
                        null
                    }
                }.sortedDescending()

                applyCombinedUiState()
            } catch (_: Exception) {
                cachedDietQualifiedDays = emptyList()
                applyCombinedUiState()
            }
        }
    }

    /**
     * Osserva i workout logs completati dell'utente per aggiornare statistiche e top esercizi.
     */
    private fun observeWorkoutLogs() {
        viewModelScope.launch {
            workoutRepository.getWorkoutLogsRealtime().collectLatest { logs ->
                val currentUserId = authSource.getCurrentUserId()

                cachedWorkoutLogs = if (currentUserId.isNullOrBlank()) {
                    emptyList()
                } else {
                    logs.filter { it.userId == currentUserId }
                }

                applyCombinedUiState()
            }
        }
    }

    /**
     * Applica allo stato UI i dati combinati provenienti da progress logs, workout logs e dieta.
     */
    private fun applyCombinedUiState() {
        val photos = cachedProgressLogs.map { it.toProgressPhotoItem() }

        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                errorMessage = currentState.errorMessage,
                firstPhoto = photos.firstOrNull(),
                lastPhoto = photos.lastOrNull(),
                allPhotos = photos,
                weightChartPoints = cachedProgressLogs.map { log ->
                    WeightChartPoint(
                        date = log.timestamp.toLocalDate(),
                        weightKg = log.weightKg
                    )
                },
                statsItems = buildStatsItems(
                    progressLogs = cachedProgressLogs,
                    workoutLogs = cachedWorkoutLogs,
                    dietQualifiedDays = cachedDietQualifiedDays
                ),
                topExercises = buildTopExercises(cachedWorkoutLogs),
                exerciseProgressPoints = if (currentState.exerciseQuery.isNotBlank()) {
                    buildExerciseProgressPoints(
                        query = currentState.exerciseQuery,
                        metric = currentState.selectedMetric
                    )
                } else {
                    emptyList()
                }
            )
        }
    }

    /**
     * Converte un log persistente in un elemento foto pronto per la UI.
     */
    private fun ProgressLog.toProgressPhotoItem(): ProgressPhotoItem {
        return ProgressPhotoItem(
            id = id,
            localPhotoUri = photoUrl,
            weightKg = weightKg,
            date = timestamp.toLocalDate()
        )
    }

    /**
     * Costruisce le statistiche sintetiche mostrate nella schermata progressi.
     */
    private fun buildStatsItems(
        progressLogs: List<ProgressLog>,
        workoutLogs: List<WorkoutLog>,
        dietQualifiedDays: List<LocalDate>
    ): List<ProgressStatItem> {
        val totalWorkouts = workoutLogs.size
        val currentWorkoutStreak = calculateWorkoutStreak(workoutLogs)
        val dietQualifiedCount = dietQualifiedDays.size
        val currentDietStreak = calculateDietStreak(dietQualifiedDays)

        return listOf(
            ProgressStatItem("Allenamenti", totalWorkouts.toString()),
            ProgressStatItem("Streak workout", "$currentWorkoutStreak giorni"),
            ProgressStatItem("Giorni dieta 3+", dietQualifiedCount.toString()),
            ProgressStatItem("Streak dieta", "$currentDietStreak giorni")
        )
    }

    /**
     * Costruisce la classifica dei top esercizi in base alle occorrenze nei workout logs.
     */
    private fun buildTopExercises(workoutLogs: List<WorkoutLog>): List<TopExerciseItem> {
        return workoutLogs
            .flatMap { it.exercises }
            .groupingBy { it.name.trim() }
            .eachCount()
            .filterKeys { it.isNotBlank() }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (exerciseName, count) ->
                TopExerciseItem(
                    exerciseName = exerciseName,
                    executionCount = count
                )
            }
    }

    /**
     * Elimina un progresso esistente usando l'id del log e aggiorna i dati mostrati in UI.
     */
    fun deleteProgressPhoto(photoItem: ProgressPhotoItem) {
        val userId = authSource.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Utente non autenticato."
                )
            }
            return
        }

        if (photoItem.id.isBlank()) {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "Impossibile eliminare il progresso selezionato."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            progressRepository.deleteProgressLog(photoItem.id).fold(
                onSuccess = {
                    loadProgressData()
                },
                onFailure = { error ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Errore durante l'eliminazione del progresso."
                        )
                    }
                }
            )
        }
    }

    /**
     * Costruisce i punti del grafico esercizio dai workout logs.
     */
    private fun buildExerciseProgressPoints(
        query: String,
        metric: ExerciseProgressMetric
    ): List<ExerciseProgressPoint> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        return cachedWorkoutLogs
            .sortedBy { it.completedAt }
            .mapNotNull { log ->
                val matchingExercises = log.exercises.filter { exercise ->
                    exercise.name.contains(normalizedQuery, ignoreCase = true)
                }

                if (matchingExercises.isEmpty()) {
                    null
                } else {
                    val value = when (metric) {
                        ExerciseProgressMetric.WEIGHT -> {
                            matchingExercises.maxOfOrNull { exercise ->
                                parseMaxNumericValue(exercise.weight)
                            } ?: 0.0
                        }

                        ExerciseProgressMetric.REPS -> {
                            matchingExercises.maxOfOrNull { exercise ->
                                parseMaxNumericValue(exercise.reps)
                            } ?: 0.0
                        }
                    }

                    if (value <= 0.0) {
                        null
                    } else {
                        ExerciseProgressPoint(
                            date = log.completedAt.toLocalDate(),
                            value = value
                        )
                    }
                }
            }
    }

    /**
     * Calcola la streak attuale in giorni distinti consecutivi per i workout.
     */
    private fun calculateWorkoutStreak(workoutLogs: List<WorkoutLog>): Int {
        val distinctDates = workoutLogs
            .map { it.completedAt.toLocalDate() }
            .distinct()
            .sortedDescending()

        if (distinctDates.isEmpty()) return 0

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val firstDate = distinctDates.first()

        if (firstDate != today && firstDate != yesterday) {
            return 0
        }

        var streak = 1

        for (index in 0 until distinctDates.lastIndex) {
            val current = distinctDates[index]
            val next = distinctDates[index + 1]
            val diff = ChronoUnit.DAYS.between(next, current)

            if (diff == 1L) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Calcola la streak attuale in giorni consecutivi con almeno 3 categorie dieta diverse.
     */
    private fun calculateDietStreak(qualifiedDays: List<LocalDate>): Int {
        if (qualifiedDays.isEmpty()) return 0

        val sortedDays = qualifiedDays.distinct().sortedDescending()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val firstDate = sortedDays.first()

        if (firstDate != today && firstDate != yesterday) {
            return 0
        }

        var streak = 1

        for (index in 0 until sortedDays.lastIndex) {
            val current = sortedDays[index]
            val next = sortedDays[index + 1]
            val diff = ChronoUnit.DAYS.between(next, current)

            if (diff == 1L) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Estrae il massimo valore numerico presente in una stringa CSV.
     */
    private fun parseMaxNumericValue(raw: String): Double {
        return raw.split(",")
            .mapNotNull { token ->
                token.trim().toDoubleOrNull()
            }
            .maxOrNull() ?: 0.0
    }

    /**
     * Converte un timestamp epoch millis in LocalDate nel fuso locale.
     */
    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /**
     * Converte il documentId CalendarDiet nel formato yyyy_m_d in LocalDate.
     */
    private fun String.toLocalDateFromDietDocumentId(): LocalDate? {
        val parts = split("_")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }
}