package com.example.gymlog_finale.ui.auth.register

// Data class immutabile che rappresenta lo stato UI del flusso di registrazione.

data class RegisterUiState(

    val nome: String = "",
    val cognome: String = "",
    val email: String = "",

    val username: String = "",
    val password: String = "",
    val confermaPassword: String = "",
    val obiettivo: String = "",
    val annoDiNascita: String = "",
    val altezza: String = "",
    val peso: String = "",
    val isPersonalTrainer: Boolean = false,

    val isGoogleFlow: Boolean = false,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterSuccess: Boolean = false
)