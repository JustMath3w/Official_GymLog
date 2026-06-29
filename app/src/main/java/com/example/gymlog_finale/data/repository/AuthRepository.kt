package com.example.gymlog_finale.data.repository

interface AuthRepository {

    /** Crea un nuovo account Firebase con email e password, restituisce l'uid. */
    suspend fun register(email: String, password: String): Result<String>

    /** Autentica un utente esistente con email e password. */
    suspend fun login(email: String, password: String): Result<Unit>

    /** Autentica tramite token Google ottenuto dal client Android. */
    suspend fun signInWithGoogle(idToken: String): Result<String>

    /** Restituisce l'uid dell'utente attualmente loggato, null se nessuno. */
    fun getCurrentUserId(): String?

    /** Controlla se esiste una sessione Firebase attiva. */
    fun isUserLoggedIn(): Boolean

    /** Invalida la sessione Firebase corrente. */
    fun logout()
}