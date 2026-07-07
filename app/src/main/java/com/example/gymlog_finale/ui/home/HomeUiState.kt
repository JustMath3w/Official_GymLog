package com.example.gymlog_finale.ui.home

// Data class immutabile che rappresenta lo stato UI della schermata Home.

import com.example.gymlog_finale.data.model.User

// Data class HomeUiState: aggregato immutabile di dati.
data class HomeUiState(
    val user: User? = null,
    val workoutOdierno: String? = null,
    val pesoAttuale: Double? = null,
    val kcalAssunte: Int = 0,
    val kcalObiettivo: Int = 0,
    val streakGiorni: Int = 0,
    val workoutStreakGiorni: Int = 0,
    val dietStreakGiorni: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)