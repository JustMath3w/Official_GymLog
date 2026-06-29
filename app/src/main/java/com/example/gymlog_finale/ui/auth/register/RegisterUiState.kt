package com.example.gymlog_finale.ui.auth.register

data class RegisterUiState(
    // Step 1
    val nome: String = "",
    val cognome: String = "",
    val email: String = "",
    // Step 2
    val username: String = "",
    val password: String = "",
    val confermaPassword: String = "",
    val obiettivo: String = "",
    val annoDiNascita: String = "",
    val altezza: String = "",
    val peso: String = "",
    val isPersonalTrainer: Boolean = false,
    // Flag per distinguere flusso Google da flusso manuale
    val isGoogleFlow: Boolean = false,
    // Stato
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterSuccess: Boolean = false
)