package com.example.gymlog_finale.ui.auth.register

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

class RegisterViewModel : ViewModel() {

    private val authSource = FirebaseAuthSource()
    private val userSource = FirebaseUserSource()
    private val bootstrapSource = FirestoreBootstrapSource()
    private val registerWithEmail = RegisterWithEmailUseCase(authSource)
    private val signInWithGoogle = SignInWithGoogleUseCase(authSource)

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var pendingGoogleUid: String? = null

    /**
     * Aggiorna il nome nello stato di registrazione.
     */
    fun onNomeChange(value: String) = _uiState.update { it.copy(nome = value, errorMessage = null) }

    /**
     * Aggiorna il cognome nello stato di registrazione.
     */
    fun onCognomeChange(value: String) = _uiState.update { it.copy(cognome = value, errorMessage = null) }

    /**
     * Aggiorna l'email nello stato di registrazione.
     */
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    /**
     * Aggiorna lo username nello stato di registrazione.
     */
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }

    /**
     * Aggiorna la password nello stato di registrazione.
     */
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    /**
     * Aggiorna la conferma password nello stato di registrazione.
     */
    fun onConfermaPasswordChange(value: String) = _uiState.update {
        it.copy(confermaPassword = value, errorMessage = null)
    }

    /**
     * Aggiorna l'obiettivo nello stato di registrazione.
     */
    fun onObiettivoChange(value: String) = _uiState.update { it.copy(obiettivo = value, errorMessage = null) }

    /**
     * Aggiorna l'anno di nascita nello stato di registrazione.
     */
    fun onAnnoDiNascitaChange(value: String) = _uiState.update {
        it.copy(annoDiNascita = value, errorMessage = null)
    }

    /**
     * Aggiorna l'altezza nello stato di registrazione.
     */
    fun onAltezzaChange(value: String) = _uiState.update { it.copy(altezza = value, errorMessage = null) }

    /**
     * Aggiorna il peso nello stato di registrazione.
     */
    fun onPesoChange(value: String) = _uiState.update { it.copy(peso = value, errorMessage = null) }

    /**
     * Aggiorna il flag personal trainer nello stato di registrazione.
     */
    fun onIsPersonalTrainerChange(value: Boolean) = _uiState.update { it.copy(isPersonalTrainer = value) }

    /**
     * Pre-popola i dati ottenuti dal profilo Google
     * e memorizza l'uid del flusso Google da completare nell'onboarding.
     */
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

    /**
     * Valida i campi dello Step 1 prima di procedere allo Step 2.
     */
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

    /**
     * Avvia la registrazione manuale creando l'account Firebase
     * e salvando poi il profilo completo su Firestore.
     * Il controllo finale di unicità username è delegato a FirebaseUserSource.saveUser().
     */
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

    /**
     * Completa il flusso Google usando l'uid ottenuto dal sign-in
     * e salva il profilo finale.
     * Il controllo finale di unicità username è delegato a FirebaseUserSource.saveUser().
     */
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

    /**
     * Inizializza il documento utente minimo e poi salva il profilo completo,
     * lasciando a FirebaseUserSource la sincronizzazione della collection usernames.
     */
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

    /**
     * Valida i campi obbligatori del secondo step della registrazione classica.
     */
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

    /**
     * Valida i campi obbligatori dell'onboarding Google prima del salvataggio finale.
     */
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

    /**
     * Resetta il flag di successo dopo che la UI ha gestito la navigazione.
     */
    fun onRegisterHandled() {
        _uiState.update { it.copy(isRegisterSuccess = false) }
    }
}