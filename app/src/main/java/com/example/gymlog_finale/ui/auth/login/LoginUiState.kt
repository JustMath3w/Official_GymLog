package com.example.gymlog_finale.ui.auth.login

// Data class immutabile che rappresenta lo stato UI della schermata di login.

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val navigateToGoogleOnboarding: Boolean = false
)