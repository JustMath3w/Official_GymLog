package com.example.gymlog_finale.ui.workout

import com.example.gymlog_finale.data.model.Workout
import com.example.gymlog_finale.data.model.WorkoutLog
import com.example.gymlog_finale.data.model.Exercise
import com.example.gymlog_finale.data.model.SplitPlan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.network.model.ExerciseDBItem
import com.example.gymlog_finale.data.repository.ExerciseRepository
import com.example.gymlog_finale.data.repository.TranslationRepository
import com.example.gymlog_finale.data.repository.WorkoutRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel completo per gestire le logiche di creazione, consultazione e svolgimento
 * delle schede d'allenamento.
 */
class WorkoutViewModel : ViewModel() {
    private val repository = WorkoutRepository()
    private val exerciseRepository = ExerciseRepository()
    private val translationRepository = TranslationRepository()
    private val tag = "WorkoutViewModel"

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    // Giorno della settimana selezionato (0=Lun, ..., 6=Dom)
    private val _currentDayIndex = MutableStateFlow(getTodayIndex())
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

    // Storico degli allenamenti completati
    private val _workoutLogs = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val workoutLogs: StateFlow<List<WorkoutLog>> = _workoutLogs.asStateFlow()

    // Scheda attualmente in esecuzione (delegata a companion object statico)
    val activeWorkout: StateFlow<Workout?> = globalActiveWorkout

    // Scheda selezionata manualmente nella Lobby per oggi
    private val _selectedWorkoutForToday = MutableStateFlow<Workout?>(null)
    val selectedWorkoutForToday: StateFlow<Workout?> = _selectedWorkoutForToday.asStateFlow()

    // Programmazione degli split (startDate, endDate, splitMap, overrides)
    private val _splitPlan = MutableStateFlow(SplitPlan())
    val splitPlan: StateFlow<SplitPlan> = _splitPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    val activeWorkoutId: StateFlow<String?> = globalActiveWorkoutId
    val isWorkoutMinimized: StateFlow<Boolean> = Companion.isWorkoutMinimized

    private val _workoutCompleted = MutableStateFlow(false)
    val workoutCompleted: StateFlow<Boolean> = _workoutCompleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun resetSaveSuccess() { _saveSuccess.value = false }

    fun saveWorkoutForClient(clientUid: String, name: String, exercises: List<Exercise>, splitType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.saveWorkoutForClient(clientUid, name, exercises, splitType)
                result.onSuccess { _saveSuccess.value = true }
                result.onFailure { _errorMessage.value = it.message ?: "Errore creazione scheda" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // ExerciseDB States
    private val _exerciseSearchResults = MutableStateFlow<List<ExerciseDBItem>>(emptyList())
    val exerciseSearchResults: StateFlow<List<ExerciseDBItem>> = _exerciseSearchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private fun getTodayIndex(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun selectDay(index: Int) {
        _currentDayIndex.value = index
    }

    init {
        observeWorkouts()
        observeWorkoutLogs()
        loadSplitPlan()
        observeSearchQuery()
    }

    private fun observeWorkoutLogs() {
        viewModelScope.launch {
            repository.getWorkoutLogsRealtime().collect { list ->
                val translatedList = list.map { log ->
                    val namesToTranslate = log.exercises.map { it.name }
                    val translatedNames = translationRepository.translateTexts(namesToTranslate, "en|it")
                    val translatedExercises = log.exercises.mapIndexed { idx, exercise ->
                        exercise.copy(name = translatedNames.getOrNull(idx) ?: exercise.name)
                    }
                    log.copy(exercises = translatedExercises)
                }
                _workoutLogs.value = translatedList
            }
        }
    }

    fun loadSplitPlan() {
        viewModelScope.launch {
            repository.getSplitPlan().onSuccess { plan ->
                Log.d(tag, "Caricato SplitPlan in ViewModel: $plan")
                _splitPlan.value = plan
            }.onFailure { e ->
                Log.e(tag, "Fallito caricamento SplitPlan: ${e.message}", e)
                _errorMessage.value = "Caricamento split fallito: ${e.message}"
            }
        }
    }

    fun saveSplitPlan(startDate: Long, endDate: Long, splitMap: Map<Int, String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentPlan = _splitPlan.value
                val updatedPlan = currentPlan.copy(
                    startDate = startDate,
                    endDate = endDate,
                    split = splitMap
                )
                val result = repository.saveSplitPlan(updatedPlan)
                if (result.isSuccess) {
                    _splitPlan.value = updatedPlan
                    Log.d(tag, "Salvataggio SplitPlan riuscito")
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    _errorMessage.value = "Salvataggio fallito: $msg"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveDailyOverride(dayIndex: Int, newSplitType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentPlan = _splitPlan.value
                val dateStr = getDateStringForDayIndex(dayIndex)
                val updatedOverrides = currentPlan.overrides.toMutableMap().apply {
                    put(dateStr, newSplitType)
                }
                val updatedPlan = currentPlan.copy(overrides = updatedOverrides)
                val result = repository.saveSplitPlan(updatedPlan)
                if (result.isSuccess) {
                    _splitPlan.value = updatedPlan
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    _errorMessage.value = "Salvataggio override fallito: $msg"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDailyOverride(dayIndex: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentPlan = _splitPlan.value
                val dateStr = getDateStringForDayIndex(dayIndex)
                val updatedOverrides = currentPlan.overrides.toMutableMap().apply {
                    remove(dateStr)
                }
                val updatedPlan = currentPlan.copy(overrides = updatedOverrides)
                val result = repository.saveSplitPlan(updatedPlan)
                if (result.isSuccess) {
                    _splitPlan.value = updatedPlan
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    _errorMessage.value = "Rimozione override fallita: $msg"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getCurrentWeekMondayCalendar(): Calendar {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        return cal
    }

    fun getDateStringForDayIndex(dayIndex: Int): String {
        val cal = getCurrentWeekMondayCalendar().apply {
            add(Calendar.DAY_OF_YEAR, dayIndex)
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    fun getSplitForDayIndex(dayIndex: Int): String {
        val plan = _splitPlan.value
        val dateStr = getDateStringForDayIndex(dayIndex)

        // 1. Prima controlla gli override giornalieri
        if (plan.overrides.containsKey(dateStr)) {
            return plan.overrides[dateStr] ?: "Rest"
        }

        // 2. Se non c'è una data di inizio/fine programmata, restituisce lo split settimanale
        if (plan.startDate == 0L || plan.endDate == 0L) {
            return plan.split[dayIndex] ?: "Rest"
        }

        // 3. Controlla se la data cade nell'intervallo programmato
        val targetCal = getCurrentWeekMondayCalendar().apply {
            add(Calendar.DAY_OF_YEAR, dayIndex)
            // Normalizza l'orario a mezzogiorno per evitare problemi di fuso orario o confini
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMillis = targetCal.timeInMillis

        val startCal = Calendar.getInstance().apply {
            timeInMillis = plan.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = plan.endDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        if (targetMillis in startCal.timeInMillis..endCal.timeInMillis) {
            val targetDayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeekIndex = when (targetDayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            return plan.split[dayOfWeekIndex] ?: "Rest"
        }

        // 4. Se fuori intervallo, restituiamo comunque lo split settimanale configurato
        return plan.split[dayIndex] ?: "Rest"
    }

    fun hasDailyOverride(dayIndex: Int): Boolean {
        val plan = _splitPlan.value
        val dateStr = getDateStringForDayIndex(dayIndex)
        return plan.overrides.containsKey(dateStr)
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
                    searchExercises(query)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _exerciseSearchResults.value = emptyList()
        }
    }

    private fun searchExercises(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Prova prima con la mappatura locale (veloce e precisa)
            var searchTerms = translationRepository.getLocalTranslation(query)
            Log.d(tag, "Ricerca locale: '$query' -> '$searchTerms'")
            
            var results = exerciseRepository.searchExercises(searchTerms)
            
            // 2. Se non trova risultati ed è diverso dalla query originale, prova a cercare con la query originaria
            if (results.isEmpty() && searchTerms != query) {
                Log.d(tag, "Nessun risultato con traduzione locale, provo con query originale: '$query'")
                results = exerciseRepository.searchExercises(query)
            }
            
            // 3. Fallback: Se ancora vuoto, chiediamo all'API di traduzione
            if (results.isEmpty()) {
                val translated = translationRepository.translateText(query, "it|en")
                val cleaned = translationRepository.cleanEnglishQuery(translated)
                Log.d(tag, "Ricerca API: '$query' tradotto in '$translated' -> pulito in '$cleaned'")
                if (cleaned.isNotEmpty() && cleaned != query && cleaned != searchTerms) {
                    results = exerciseRepository.searchExercises(cleaned)
                }
            }
            
            Log.d(tag, "Risultati trovati: ${results.size}")
            
            // 4. Traduci in blocco i nomi degli esercizi trovati
            val namesToTranslate = results.map { item -> item.name ?: "" }
            val translatedNames = translationRepository.translateTexts(namesToTranslate, "en|it")
            
            // 5. Mappa i risultati con i nomi tradotti e usa le traduzioni statiche veloci per bodyPart e target
            val translatedResults = results.mapIndexed { idx, item ->
                item.copy(
                    name = translatedNames.getOrNull(idx) ?: item.name ?: "",
                    bodyPart = translationRepository.translateBodyPart(item.bodyPart),
                    target = translationRepository.translateTarget(item.target)
                )
            }
            
            _exerciseSearchResults.value = translatedResults
            _isLoading.value = false
        }
    }



    private fun observeWorkouts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getWorkoutsRealtime().collectLatest { list ->
                val translatedList = list.map { workout ->
                    val namesToTranslate = workout.exercises.map { it.name }
                    val translatedNames = translationRepository.translateTexts(namesToTranslate, "en|it")
                    val translatedExercises = workout.exercises.mapIndexed { idx, exercise ->
                        exercise.copy(name = translatedNames.getOrNull(idx) ?: exercise.name)
                    }
                    workout.copy(exercises = translatedExercises)
                }
                _workouts.value = translatedList
                _isLoading.value = false
            }
        }
    }

    fun saveWorkout(name: String, exercises: List<Exercise>, id: String = "", splitType: String = "Rest") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filteredExercises = exercises.filter { it.name.isNotBlank() }
                val workout = Workout(id = id, name = name, exercises = filteredExercises, splitType = splitType)

                val result = repository.saveWorkout(workout)
                result.onSuccess { _saveSuccess.value = true }
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteWorkout(workoutId)
                if (result.isFailure) {
                    val msg = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    _errorMessage.value = "Impossibile eliminare: $msg"
                    Log.e(tag, "Errore eliminazione scheda: $msg")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun activateWorkout(workoutId: String) {
        _globalActiveWorkoutId.value = workoutId
        val workout = workouts.value.firstOrNull { it.id == workoutId }
        if (workout != null) {
            _globalActiveWorkout.value = workout
            _isWorkoutMinimized.value = false
        }
    }

    fun startWorkout(workout: Workout) {
        _globalActiveWorkout.value = workout
        _globalActiveWorkoutId.value = workout.id
        _isWorkoutMinimized.value = false
    }

    fun cancelWorkout() {
        _globalActiveWorkout.value = null
        _globalActiveWorkoutId.value = null
        _isWorkoutMinimized.value = false
    }

    fun completeWorkout(workout: Workout, actualExercises: List<Exercise>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val targetCal = getCurrentWeekMondayCalendar().apply {
                    val currentCal = Calendar.getInstance()
                    set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, currentCal.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, currentCal.get(Calendar.MILLISECOND))
                    add(Calendar.DAY_OF_YEAR, _currentDayIndex.value)
                }

                val log = WorkoutLog(
                    workoutId = workout.id,
                    workoutName = workout.name,
                    exercises = actualExercises,
                    completedAt = targetCal.timeInMillis
                )
                val result = repository.saveWorkoutLog(log)
                if (result.isSuccess) {
                    _globalActiveWorkout.value = null
                    _globalActiveWorkoutId.value = null
                    _isWorkoutMinimized.value = false
                    _workoutCompleted.value = true
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    _errorMessage.value = "Salvataggio fallito: $msg"
                    Log.e(tag, "Errore salvataggio log allenamento: $msg")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore: ${e.message}"
                Log.e(tag, "Errore nel completamento dell'allenamento", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectWorkoutForToday(workout: Workout?) {
        _selectedWorkoutForToday.value = workout
    }

    fun resetSuccess() {
        _saveSuccess.value = false
    }

    fun resetWorkoutCompleted() {
        _workoutCompleted.value = false
    }

    private val _currentExerciseDetails = MutableStateFlow<Exercise?>(null)
    val currentExerciseDetails: StateFlow<Exercise?> = _currentExerciseDetails.asStateFlow()

    fun loadExerciseDetails(exercise: Exercise) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (exercise.id.isNotEmpty()) {
                    val freshData = exerciseRepository.getExerciseById(exercise.id)
                    if (freshData != null) {
                        // Facciamo le chiamate di traduzione e ricerca in parallelo
                        val youtubeIdDeferred = async { exerciseRepository.searchYoutubeVideo(freshData.name ?: "") }
                        val translatedNameDeferred = async { translationRepository.translateText(freshData.name ?: "") }
                        val translatedBodyPartDeferred = async { translationRepository.translateText(freshData.bodyPart ?: "") }
                        val translatedTargetDeferred = async { translationRepository.translateText(freshData.target ?: "") }
                        
                        val translatedInstructionsDeferreds = freshData.instructions.map { 
                            async { translationRepository.translateText(it) } 
                        }
                        
                        val youtubeId = youtubeIdDeferred.await()
                        val translatedName = translatedNameDeferred.await()
                        val translatedBodyPart = translatedBodyPartDeferred.await()
                        val translatedTarget = translatedTargetDeferred.await()
                        val translatedInstructions = translatedInstructionsDeferreds.map { it.await() }

                        _currentExerciseDetails.value = exercise.copy(
                            name = translatedName,
                            gifUrl = freshData.gifUrl ?: exercise.gifUrl,
                            instructions = translatedInstructions,
                            bodyPart = translatedBodyPart,
                            target = translatedTarget,
                            youtubeVideoId = youtubeId
                        )
                    } else {
                        _currentExerciseDetails.value = exercise
                    }
                } else {
                    _currentExerciseDetails.value = exercise
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearExerciseDetails() {
        _currentExerciseDetails.value = null
    }

    fun setWorkoutMinimized(minimized: Boolean) {
        Companion.setWorkoutMinimized(minimized)
    }

    companion object {
        private val _globalActiveWorkout = MutableStateFlow<Workout?>(null)
        val globalActiveWorkout: StateFlow<Workout?> = _globalActiveWorkout.asStateFlow()

        private val _globalActiveWorkoutId = MutableStateFlow<String?>(null)
        val globalActiveWorkoutId: StateFlow<String?> = _globalActiveWorkoutId.asStateFlow()

        private val _isWorkoutMinimized = MutableStateFlow(false)
        val isWorkoutMinimized: StateFlow<Boolean> = _isWorkoutMinimized.asStateFlow()

        fun setWorkoutMinimized(minimized: Boolean) {
            _isWorkoutMinimized.value = minimized
        }
    }
}