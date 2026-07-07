package com.example.gymlog_finale.data.repository

// Repository che astrae FirebaseAuthSource verso i ViewModel di login e registrazione.

interface AuthRepository {

    // Registra un nuovo utente sulla piattaforma e crea il relativo documento profilo.
    suspend fun register(email: String, password: String): Result<String>

    // Esegue il login dell'utente con le credenziali fornite.
    suspend fun login(email: String, password: String): Result<Unit>

    // Esegue l'autenticazione tramite Google ID token e restituisce l'UID Firebase.
    suspend fun signInWithGoogle(idToken: String): Result<String>

    // Restituisce l'UID dell'utente attualmente autenticato, se presente.
    fun getCurrentUserId(): String?

    // Predicato: indica se esiste una sessione utente attiva.
    fun isUserLoggedIn(): Boolean

    // Termina la sessione utente corrente e ripulisce lo stato locale.
    fun logout()
}