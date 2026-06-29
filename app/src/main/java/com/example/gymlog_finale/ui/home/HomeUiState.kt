package com.example.gymlog_finale.ui.home

import com.example.gymlog_finale.data.model.User

/**
 * Stato immutabile della HomeScreen.
 * Contiene il riepilogo utente, calorie giornaliere e streak principali mostrate nella home.
 */
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