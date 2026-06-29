package com.example.gymlog_finale.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.firebase.FirebaseAuthSource
import com.example.gymlog_finale.data.firebase.FirebaseUserSource
import com.example.gymlog_finale.domain.usecase.LoginWithEmailUseCase
import com.example.gymlog_finale.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val authSource = FirebaseAuthSource()
    private val userSource = FirebaseUserSource()
    private val loginWithEmail = LoginWithEmailUseCase(authSource)
    private val signInWithGoogle = SignInWithGoogleUseCase(authSource)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /** Avvia il login con email e password tramite Firebase. */
    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = loginWithEmail(
                email = _uiState.value.email.trim(),
                password = _uiState.value.password
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    /**
     * Dopo il sign-in Google controlla se l'utente esiste già su Firestore.
     * Se esiste naviga alla Home, altrimenti avvia l'onboarding.
     */
    fun loginWithGoogle(idToken: String, onSetGoogleData: (uid: String, nome: String, cognome: String, email: String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signInWithGoogle(idToken)
            result.fold(
                onSuccess = { uid ->
                    val exists = userSource.userExists(uid)
                    if (exists) {
                        _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                    } else {
                        val firebaseUser = authSource.getCurrentFirebaseUser()
                        val nome = firebaseUser?.displayName?.split(" ")?.firstOrNull() ?: ""
                        val cognome = firebaseUser?.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
                        val email = firebaseUser?.email ?: ""
                        onSetGoogleData(uid, nome, cognome, email)
                        _uiState.update { it.copy(isLoading = false, navigateToGoogleOnboarding = true) }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    fun onLoginHandled() {
        _uiState.update { it.copy(isLoginSuccess = false) }
    }

    fun onGoogleOnboardingHandled() {
        _uiState.update { it.copy(navigateToGoogleOnboarding = false) }
    }
}