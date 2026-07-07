package com.example.gymlog_finale.ui.home

// ViewModel della Home: aggrega dati da Workout, Progress e Diet Repository per il feed del giorno.

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.firebase.FirebaseUserSource
import com.example.gymlog_finale.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.example.gymlog_finale.data.model.WorkoutLog
import java.util.Calendar
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Classe HomeViewModel: unità principale definita in questo file.
class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val userRepository: UserRepository = FirebaseUserSource()
    private var dietListener: ListenerRegistration? = null
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    // Carica i dati necessari per la schermata o il caso d'uso.
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = userRepository.fetchCurrentUser()
            if (result == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Nessuna sessione attiva") }
                return@launch
            }

            val user = result.getOrNull()
            if (user != null) {

                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val currentDayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

                var kcalAssunte = 0
                var kcalObiettivo = 2000
                var streakAttuale = 0
                var dietStreakAttuale = 0

                try {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                        val userDoc = db.collection("CalendarDiet").document(uid).get().await()
                        if (userDoc.exists()) {
                            val goals = userDoc.get("diet_goals") as? Map<String, Any>
                            if (goals != null) {
                                val targetCal = (goals["calories"] as? Number)?.toInt()
                                if (targetCal != null) kcalObiettivo = targetCal
                            }
                        }

                        val y = calendar.get(Calendar.YEAR)
                        val m = calendar.get(Calendar.MONTH) + 1
                        val d = calendar.get(Calendar.DAY_OF_MONTH)
                        val docId = "${y}_${m}_${d}"

                        val dayDoc = db.collection("CalendarDiet").document(uid).collection("days").document(docId).get().await()

                        if (dayDoc.exists()) {
                            val stats = dayDoc.toObject(com.example.gymlog_finale.data.model.DailyDietStats::class.java)
                            if (stats != null) {
                                if (stats.totalCalories > 0) kcalObiettivo = stats.totalCalories
                                kcalAssunte = stats.consumedCalories
                            }
                        }

                        val logsSnapshot = db.collection("workout_logs").whereEqualTo("userId", uid).get().await()
                        val logs = logsSnapshot.documents.mapNotNull { it.toObject(WorkoutLog::class.java) }
                        streakAttuale = calculateWorkoutStreak(logs)

                        val dietDaysSnapshot = db.collection("CalendarDiet").document(uid).collection("days").get().await()
                        val dietQualifiedDays = dietDaysSnapshot.documents.mapNotNull { doc ->
                            val dietStats = doc.toObject(com.example.gymlog_finale.data.model.DailyDietStats::class.java)
                            if (dietStats != null) {
                                val distinctCategories = dietStats.foods.map { it.category.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
                                if (distinctCategories.size >= 3) {
                                    val parts = doc.id.split("_")
                                    if (parts.size == 3) {
                                        try {
                                            LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                                        } catch (e: Exception) { null }
                                    } else null
                                } else null
                            } else null
                        }
                        dietStreakAttuale = calculateDietStreak(dietQualifiedDays)                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _uiState.update {
                    it.copy(
                        user = user,
                        workoutOdierno = "Push Day",
                        pesoAttuale = user.peso.takeIf { p -> p > 0.0 },
                        kcalAssunte = kcalAssunte,
                        kcalObiettivo = kcalObiettivo,
                        streakGiorni = streakAttuale,
                        workoutStreakGiorni = streakAttuale,
                        dietStreakGiorni = dietStreakAttuale,
                        isLoading = false
                    )
                }
            } else {
                val e = result.exceptionOrNull()
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e?.message ?: "Errore caricamento profilo")
                }
            }
        }
    }

    // Calcola l'aggregato richiesto a partire dai dati forniti.
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

    // Calcola l'aggregato richiesto a partire dai dati forniti.
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

    // Funzione di supporto interna alla classe.
    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}