package com.example.gymlog_finale.ui.profile

// ViewModel del Profilo: gestisce aggiornamento dati, foto profilo e logout.

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

// Data class ProfileUiState: aggregato immutabile di dati.
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

// Classe ProfileViewModel: unità principale definita in questo file.
class ProfileViewModel(
    private val userRepository: UserRepository = FirebaseUserSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    // Carica i dati necessari per la schermata o il caso d'uso.
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

    // Aggiorna i campi indicati dell'entità sulla sorgente dati.
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

    // Gestisce operazioni relative alla password (aggiornamento o reset).
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

    // Avvia il flusso di reset password inviando la mail di ripristino.
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

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
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

    // Termina la sessione utente corrente e ripulisce lo stato locale.
    fun logout() {
        userRepository.logout()
    }

    // Ripulisce lo stato o la collezione indicati.
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}