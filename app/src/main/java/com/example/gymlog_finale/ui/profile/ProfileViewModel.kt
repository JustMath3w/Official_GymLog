package com.example.gymlog_finale.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.firebase.FirebaseUserSource
import com.example.gymlog_finale.data.model.User
import com.example.gymlog_finale.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stato della ProfileScreen.
 * - successMessage / errorMessage: feedback transienti consumati dalla UI via clearMessages()
 * - isSaving: blocca i dialog mentre un'operazione è in corso
 */
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel della ProfileScreen: gestisce caricamento profilo, modifiche puntuali
 * (campo singolo via dialog), cambio password con reauth, reset password via mail,
 * eliminazione account. Tutte le operazioni espongono feedback in uiState.
 */
class ProfileViewModel(
    private val userRepository: UserRepository = FirebaseUserSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /** Carica il profilo dell'utente autenticato; gestisce l'assenza di sessione. */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.fetchCurrentUser()
            if (result == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Nessuna sessione attiva") }
                return@launch
            }
            result.fold(
                onSuccess = { user -> _uiState.update { it.copy(user = user, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
            )
        }
    }

    /**
     * Aggiornamento parziale del documento utente. Per il campo 'username'
     * verifica prima l'unicità (escludendo se stesso) e blocca l'update se occupato.
     *
     * 'value' è Any? per supportare i tipi misti: String, Int, Double, Boolean.
     */
    fun updateField(fieldKey: String, value: Any?) {
        val uid = userRepository.getCurrentUid() ?: run {
            _uiState.update { it.copy(errorMessage = "Sessione non valida") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            if (fieldKey == "username") {
                val newUsername = (value as? String)?.trim().orEmpty()
                if (newUsername.isBlank()) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Username non valido") }
                    return@launch
                }
                val available = userRepository.isUsernameAvailable(newUsername, excludeUid = uid)
                if (!available) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Username già in uso") }
                    return@launch
                }
            }
            val res = userRepository.updateUserFields(uid, mapOf(fieldKey to value))
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Dati aggiornati") }
                    loadProfile()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Errore") }
                }
            )
        }
    }

    /**
     * Cambia la password dell'utente corrente reautenticando prima con la password vecchia.
     * Firebase richiede reauth se il login risale a > ~5 minuti.
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "La nuova password deve avere almeno 6 caratteri") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val reauth = userRepository.reauthenticate(oldPassword)
            if (reauth.isFailure) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Password attuale errata") }
                return@launch
            }
            val res = userRepository.changePassword(newPassword)
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Password aggiornata") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Errore") }
                }
            )
        }
    }

    /** Invia mail di reset password all'indirizzo dell'utente corrente. */
    fun sendResetPasswordEmail() {
        val email = _uiState.value.user?.email
        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Email non disponibile") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val res = userRepository.sendPasswordResetEmail(email)
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Email di reset inviata a $email") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Errore") }
                }
            )
        }
    }

    /**
     * Elimina l'account dopo reauth con la password fornita.
     * onSuccess viene invocato dalla UI per navigare al login.
     */
    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val reauth = userRepository.reauthenticate(password)
            if (reauth.isFailure) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Password errata") }
                return@launch
            }
            val res = userRepository.deleteAccount()
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Account eliminato") }
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Errore eliminazione") }
                }
            )
        }
    }

    /** Esegue il logout invalidando la sessione Firebase. */
    fun logout() {
        userRepository.logout()
    }

    /** Reset dei messaggi transienti dopo che la UI li ha mostrati. */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}