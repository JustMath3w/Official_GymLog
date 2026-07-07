package com.example.gymlog_finale.ui.auth.register

// ViewModel che orchestra il flusso di registrazione in due step e l'onboarding post-Google.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.firebase.FirebaseAuthSource
import com.example.gymlog_finale.data.firebase.FirebaseUserSource
import com.example.gymlog_finale.data.firebase.FirestoreBootstrapSource
import com.example.gymlog_finale.data.model.User
import com.example.gymlog_finale.domain.usecase.RegisterWithEmailUseCase
import com.example.gymlog_finale.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Classe RegisterViewModel: unità principale definita in questo file.
class RegisterViewModel : ViewModel() {

    private val authSource = FirebaseAuthSource()
    private val userSource = FirebaseUserSource()
    private val bootstrapSource = FirestoreBootstrapSource()
    private val registerWithEmail = RegisterWithEmailUseCase(authSource)
    private val signInWithGoogle = SignInWithGoogleUseCase(authSource)

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var pendingGoogleUid: String? = null

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onNomeChange(value: String) = _uiState.update { it.copy(nome = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onCognomeChange(value: String) = _uiState.update { it.copy(cognome = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onConfermaPasswordChange(value: String) = _uiState.update {
        it.copy(confermaPassword = value, errorMessage = null)
    }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onObiettivoChange(value: String) = _uiState.update { it.copy(obiettivo = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onAnnoDiNascitaChange(value: String) = _uiState.update {
        it.copy(annoDiNascita = value, errorMessage = null)
    }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onAltezzaChange(value: String) = _uiState.update { it.copy(altezza = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onPesoChange(value: String) = _uiState.update { it.copy(peso = value, errorMessage = null) }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onIsPersonalTrainerChange(value: Boolean) = _uiState.update { it.copy(isPersonalTrainer = value) }

    // Aggiorna il campo indicato nello stato interno.
    fun setGoogleUserData(uid: String, nome: String, cognome: String, email: String) {
        _uiState.update {
            it.copy(
                nome = nome,
                cognome = cognome,
                email = email,
                isGoogleFlow = true,
                errorMessage = null
            )
        }
        pendingGoogleUid = uid
    }

    // Valida l'input e restituisce eventuali errori.
    fun validateStep1(): Boolean {
        val state = _uiState.value
        return when {
            state.nome.isBlank() || state.cognome.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Nome e cognome sono obbligatori") }
                false
            }

            state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> {
                _uiState.update { it.copy(errorMessage = "Inserisci un'email valida") }
                false
            }

            else -> true
        }
    }

    // Registra un nuovo utente sulla piattaforma e crea il relativo documento profilo.
    fun register() {
        val state = _uiState.value
        if (!validateStep2(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = registerWithEmail(state.email.trim(), state.password)
            result.fold(
                onSuccess = { uid ->
                    saveUserToFirestore(uid, state)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Errore durante la registrazione"
                        )
                    }
                }
            )
        }
    }

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    fun completeGoogleOnboarding() {
        val state = _uiState.value
        val uid = pendingGoogleUid

        if (uid == null) {
            _uiState.update { it.copy(errorMessage = "Sessione Google non valida, riprova") }
            return
        }

        if (!validateOnboarding(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            saveUserToFirestore(uid, state)
        }
    }

    // Persiste l'entità sulla sorgente dati (creazione o aggiornamento).
    private suspend fun saveUserToFirestore(uid: String, state: RegisterUiState) {
        val bootstrapResult = bootstrapSource.ensureUserDocument()

        bootstrapResult.fold(
            onSuccess = {
                val user = User(
                    uid = uid,
                    nome = state.nome.trim(),
                    cognome = state.cognome.trim(),
                    username = state.username.trim(),
                    email = state.email.trim(),
                    annoDiNascita = state.annoDiNascita.toIntOrNull() ?: 0,
                    altezza = state.altezza.toIntOrNull() ?: 0,
                    peso = state.peso.toDoubleOrNull() ?: 0.0,
                    obiettivo = state.obiettivo,
                    isPersonalTrainer = state.isPersonalTrainer,
                    hasPersonalTrainer = null
                )

                val saveResult = userSource.saveUser(user)
                saveResult.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRegisterSuccess = true,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Errore durante il salvataggio profilo"
                            )
                        }
                    }
                )
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Errore durante l'inizializzazione del profilo"
                    )
                }
            }
        )
    }

    // Valida l'input e restituisce eventuali errori.
    private fun validateStep2(state: RegisterUiState): Boolean {
        return when {
            state.username.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Username obbligatorio") }
                false
            }

            state.password.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Password di almeno 6 caratteri") }
                false
            }

            state.password != state.confermaPassword -> {
                _uiState.update { it.copy(errorMessage = "Le password non coincidono") }
                false
            }

            state.obiettivo.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Seleziona un obiettivo") }
                false
            }

            else -> true
        }
    }

    // Valida l'input e restituisce eventuali errori.
    private fun validateOnboarding(state: RegisterUiState): Boolean {
        return when {
            state.username.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Username obbligatorio") }
                false
            }

            state.obiettivo.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Seleziona un obiettivo") }
                false
            }

            else -> true
        }
    }

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    fun onRegisterHandled() {
        _uiState.update { it.copy(isRegisterSuccess = false) }
    }
}