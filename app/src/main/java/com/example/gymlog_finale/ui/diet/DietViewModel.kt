package com.example.gymlog_finale.ui.diet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.UUID

import com.example.gymlog_finale.data.model.DailyDietStats
import com.example.gymlog_finale.data.model.FoodItem

class DietViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _weeklyStats = MutableStateFlow<Map<Int, DailyDietStats>>((0..6).associateWith { DailyDietStats() })
    val weeklyStats: StateFlow<Map<Int, DailyDietStats>> = _weeklyStats.asStateFlow()

    private val _currentDayIndex = MutableStateFlow(
        Calendar.getInstance().let { cal ->
            val day = cal.get(Calendar.DAY_OF_WEEK)
            if (day == Calendar.SUNDAY) 6 else day - 2
        }
    )
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentYear = -1
    private var currentWeek = -1

    init {
        val (year, week) = getCurrentYearAndWeek()
        currentYear = year
        currentWeek = week
        loadDataFromFirestore()
    }

    private fun getCurrentYearAndWeek(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        return Pair(year, week)
    }

    private fun getDateForDayIndex(year: Int, week: Int, dayIndex: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.WEEK_OF_YEAR, week)
        val dayOfWeek = if (dayIndex == 6) Calendar.SUNDAY else dayIndex + 2
        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek)
        return cal
    }

    private fun saveToCalendarioDieta(uid: String, dayIndex: Int, stats: DailyDietStats) {
        val cal = getDateForDayIndex(currentYear, currentWeek, dayIndex)
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1 // 1-indexed for readability
        val d = cal.get(Calendar.DAY_OF_MONTH)
        
        val docId = "${y}_${m}_${d}"
        db.collection("CalendarDiet").document(uid).collection("days").document(docId)
            .set(stats, SetOptions.merge())
            .addOnFailureListener { e ->
                android.util.Log.e("DietViewModel", "Errore nel salvataggio del pasto in CalendarDiet: ${e.message}", e)
            }
    }

    private val userId: String? get() = auth.currentUser?.uid

    private fun loadDataFromFirestore() {
        val uid = userId
        if (uid == null) {
            _isLoading.value = false
            return
        }

        // 1. Fetch diet_goals from CalendarDiet
        db.collection("CalendarDiet").document(uid).get().addOnSuccessListener { doc ->
            val goals = doc.get("diet_goals") as? Map<String, Any>
            val cal = (goals?.get("calories") as? Number)?.toInt() ?: 2000
            val carbs = (goals?.get("carbs") as? Number)?.toDouble() ?: 250.0
            val prot = (goals?.get("proteins") as? Number)?.toDouble() ?: 150.0
            val fats = (goals?.get("fats") as? Number)?.toDouble() ?: 70.0

            val defaultStats = DailyDietStats(cal, carbs, prot, fats)

            // 2. Fetch current week data from CalendarDiet/uid/days
            val daysList = (0..6).map { dayIndex ->
                val c = getDateForDayIndex(currentYear, currentWeek, dayIndex)
                val y = c.get(Calendar.YEAR)
                val m = c.get(Calendar.MONTH) + 1
                val d = c.get(Calendar.DAY_OF_MONTH)
                "${y}_${m}_${d}"
            }

            db.collection("CalendarDiet").document(uid).collection("days")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), daysList)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }

                    val newWeeklyStats = mutableMapOf<Int, DailyDietStats>()
                    val docsMap = snapshot?.documents?.associateBy { it.id } ?: emptyMap()

                    for (i in 0..6) {
                        val docId = daysList[i]
                        val dayDoc = docsMap[docId]
                        
                        if (dayDoc != null && dayDoc.exists()) {
                            val stats = dayDoc.toObject(DailyDietStats::class.java)
                            if (stats != null) {
                                val tCal = if (stats.totalCalories > 0) stats.totalCalories else cal
                                val tCarbs = if (stats.totalCarbs > 0.0) stats.totalCarbs else carbs
                                val tProt = if (stats.totalProteins > 0.0) stats.totalProteins else prot
                                val tFats = if (stats.totalFats > 0.0) stats.totalFats else fats
                                
                                newWeeklyStats[i] = stats.copy(
                                    totalCalories = tCal,
                                    totalCarbs = tCarbs,
                                    totalProteins = tProt,
                                    totalFats = tFats
                                )
                            } else {
                                newWeeklyStats[i] = defaultStats.copy()
                            }
                        } else {
                            newWeeklyStats[i] = defaultStats.copy()
                        }
                    }
                    _weeklyStats.value = newWeeklyStats
                    _isLoading.value = false
                }
        }.addOnFailureListener {
            _isLoading.value = false
        }
    }

    fun checkWeekAndResetIfNeeded() {
        val (year, week) = getCurrentYearAndWeek()
        if (year != currentYear || week != currentWeek) {
            currentYear = year
            currentWeek = week
            _isLoading.value = true
            loadDataFromFirestore()
            
            val cal = Calendar.getInstance()
            val day = cal.get(Calendar.DAY_OF_WEEK)
            _currentDayIndex.value = if (day == Calendar.SUNDAY) 6 else day - 2
        }
    }


    fun selectDay(index: Int) {
        checkWeekAndResetIfNeeded()
        _currentDayIndex.value = index
    }

    fun addOrUpdateFood(food: FoodItem) {
        checkWeekAndResetIfNeeded()
        val uid = userId ?: return
        
        var finalStats: DailyDietStats? = null
        var finalDayIndex: Int = -1

        _weeklyStats.update { currentMap ->
            val dayIndex = _currentDayIndex.value
            val currentStats = currentMap[dayIndex] ?: DailyDietStats()
            
            val existingIndex = currentStats.foods.indexOfFirst { it.id == food.id }
            val newFoods = currentStats.foods.toMutableList()
            if (existingIndex >= 0) {
                newFoods[existingIndex] = food
            } else {
                newFoods.add(food)
            }
            
            val updatedStats = currentStats.copy(foods = newFoods)
            finalStats = updatedStats
            finalDayIndex = dayIndex
            
            currentMap.toMutableMap().apply {
                this[dayIndex] = updatedStats
            }
        }
        
        finalStats?.let {
            saveToCalendarioDieta(uid, finalDayIndex, it)
        }
    }

    fun deleteFood(food: FoodItem) {
        checkWeekAndResetIfNeeded()
        val uid = userId ?: return
        
        var finalStats: DailyDietStats? = null
        var finalDayIndex: Int = -1

        _weeklyStats.update { currentMap ->
            val dayIndex = _currentDayIndex.value
            val currentStats = currentMap[dayIndex] ?: DailyDietStats()
            
            val newFoods = currentStats.foods.filter { it.id != food.id }
            val updatedStats = currentStats.copy(foods = newFoods)
            finalStats = updatedStats
            finalDayIndex = dayIndex
            
            currentMap.toMutableMap().apply {
                this[dayIndex] = updatedStats
            }
        }
        
        finalStats?.let {
            saveToCalendarioDieta(uid, finalDayIndex, it)
        }
    }
}
