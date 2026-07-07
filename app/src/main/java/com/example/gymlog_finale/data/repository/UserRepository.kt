package com.example.gymlog_finale.data.repository

// Repository che gestisce il documento utente, l'unicità dello username e la foto profilo.

import com.example.gymlog_finale.data.model.User

// Interfaccia UserRepository: contratto pubblico del modulo.
interface UserRepository {

    // Persiste l'entità sulla sorgente dati (creazione o aggiornamento).
    suspend fun saveUser(user: User): Result<Unit>

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    suspend fun getUser(uid: String): Result<User>

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    suspend fun userExists(uid: String): Boolean

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    suspend fun fetchCurrentUser(): Result<User>?

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    fun getCurrentUid(): String?

    // Aggiorna i campi indicati dell'entità sulla sorgente dati.
    suspend fun updateUserFields(uid: String, fields: Map<String, Any?>): Result<Unit>

    // Predicato che verifica una condizione booleana sullo stato.
    suspend fun isUsernameAvailable(username: String, excludeUid: String? = null): Boolean

    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    suspend fun searchUsersByUsername(prefix: String): Result<List<User>>

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    suspend fun reauthenticate(currentPassword: String): Result<Unit>

    // Gestisce operazioni relative alla password (aggiornamento o reset).
    suspend fun changePassword(newPassword: String): Result<Unit>

    // Gestisce operazioni relative alla password (aggiornamento o reset).
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
    suspend fun deleteAccount(): Result<Unit>

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    suspend fun getAllUsersForCommunity(): Result<List<User>>

    // Termina la sessione utente corrente e ripulisce lo stato locale.
    fun logout()
}