package com.example.gymlog_finale.ui.diet

// ViewModel dello storico dieta: espone gli aggregati settimanali/giornalieri.

import com.example.gymlog_finale.data.model.DailyDietStats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.UUID

// Classe DietHistoryViewModel: unità principale definita in questo file.
class DietHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _selectedDayStats = MutableStateFlow<DailyDietStats?>(null)
    val selectedDayStats: StateFlow<DailyDietStats?> = _selectedDayStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var globalGoals = DailyDietStats()
    private var goalsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var currentDayListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            goalsListener = db.collection("CalendarDiet").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (snapshot != null && snapshot.exists()) {
                        val goals = snapshot.get("diet_goals") as? Map<String, Any>
                        val calGoal = (goals?.get("calories") as? Number)?.toInt() ?: 2000
                        val carbsGoal = (goals?.get("carbs") as? Number)?.toDouble() ?: 250.0
                        val protGoal = (goals?.get("proteins") as? Number)?.toDouble() ?: 150.0
                        val fatsGoal = (goals?.get("fats") as? Number)?.toDouble() ?: 70.0
                        globalGoals = DailyDietStats(calGoal, carbsGoal, protGoal, fatsGoal)

                        if (_selectedDayStats.value?.foods?.isEmpty() == true) {
                            _selectedDayStats.value = globalGoals.copy()
                        }
                    }
                }
        }

        loadDataForDate(_selectedDate.value)
    }

    // Imposta l'elemento indicato come selezione corrente nello stato UI.
    fun selectDate(calendar: Calendar) {
        _selectedDate.value = calendar
        loadDataForDate(calendar)
    }

    private val userId: String? get() = auth.currentUser?.uid

    // Carica i dati necessari per la schermata o il caso d'uso.
    private fun loadDataForDate(date: Calendar) {
        val uid = userId
        if (uid == null) return

        _isLoading.value = true
        currentDayListener?.remove()

        val y = date.get(Calendar.YEAR)
        val m = date.get(Calendar.MONTH) + 1
        val d = date.get(Calendar.DAY_OF_MONTH)

        currentDayListener = db.collection("CalendarDiet").document(uid).collection("days").document("${y}_${m}_${d}")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val stats = snapshot.toObject(DailyDietStats::class.java)
                    if (stats != null) {
                        val tCal = if (stats.totalCalories > 0) stats.totalCalories else globalGoals.totalCalories
                        val tCarbs = if (stats.totalCarbs > 0.0) stats.totalCarbs else globalGoals.totalCarbs
                        val tProt = if (stats.totalProteins > 0.0) stats.totalProteins else globalGoals.totalProteins
                        val tFats = if (stats.totalFats > 0.0) stats.totalFats else globalGoals.totalFats

                        _selectedDayStats.value = stats.copy(
                            totalCalories = tCal,
                            totalCarbs = tCarbs,
                            totalProteins = tProt,
                            totalFats = tFats
                        )
                    } else {
                        _selectedDayStats.value = globalGoals.copy()
                    }
                } else {

                    _selectedDayStats.value = globalGoals.copy()
                }
                _isLoading.value = false
            }
    }

    // Callback del ViewModel: rilascia risorse asincrone alla distruzione.
    override fun onCleared() {
        super.onCleared()
        currentDayListener?.remove()
        goalsListener?.remove()
    }
    // Aggiorna i campi indicati dell'entità sulla sorgente dati.
    fun updateGoalsForDateRange(
        startDate: Calendar,
        endDate: Calendar,
        calories: Int,
        carbs: Double,
        proteins: Double,
        fats: Double
    ) {
        val uid = userId ?: return

        _isLoading.value = true

        val datesToUpdate = mutableListOf<String>()
        val cal = startDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val endCal = endDate.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)

        while (cal.before(endCal)) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            datesToUpdate.add("${y}_${m}_${d}")
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val collectionRef = db.collection("CalendarDiet").document(uid).collection("days")
        val batches = mutableListOf<com.google.firebase.firestore.WriteBatch>()
        var currentBatch = db.batch()
        var count = 0

        val updates = mapOf(
            "totalCalories" to calories,
            "totalCarbs" to carbs,
            "totalProteins" to proteins,
            "totalFats" to fats
        )

        for (docId in datesToUpdate) {
            currentBatch.set(collectionRef.document(docId), updates, com.google.firebase.firestore.SetOptions.merge())
            count++
            if (count == 490) {
                batches.add(currentBatch)
                currentBatch = db.batch()
                count = 0
            }
        }
        if (count > 0) {
            batches.add(currentBatch)
        }

        var completedBatches = 0
        if (batches.isEmpty()) {
            _isLoading.value = false
            loadDataForDate(_selectedDate.value)
            return
        }

        for (batch in batches) {
            batch.commit().addOnCompleteListener {
                completedBatches++
                if (completedBatches == batches.size) {
                    _isLoading.value = false

                    loadDataForDate(_selectedDate.value)
                }
            }
        }
    }
}
