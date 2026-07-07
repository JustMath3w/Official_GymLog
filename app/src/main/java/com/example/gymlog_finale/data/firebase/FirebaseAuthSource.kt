package com.example.gymlog_finale.data.firebase

// Sorgente dati che incapsula FirebaseAuth: registrazione, login email/password, integrazione con Credential Manager per il login Google e reset password.

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.gymlog_finale.data.repository.AuthRepository
import kotlinx.coroutines.tasks.await

// Classe FirebaseAuthSource: unità principale definita in questo file.
class FirebaseAuthSource : AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Registra un nuovo utente sulla piattaforma e crea il relativo documento profilo.
    override suspend fun register(email: String, password: String): Result<String> {
        return try {

            val result = auth.createUserWithEmailAndPassword(email, password).await()

            val uid = result.user?.uid ?: return Result.failure(Exception("UID non disponibile"))
            Result.success(uid)
        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // Esegue il login dell'utente con le credenziali fornite.
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {

            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // Esegue l'autenticazione tramite Google ID token e restituisce l'UID Firebase.
    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {

            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val result = auth.signInWithCredential(credential).await()

            val uid = result.user?.uid ?: return Result.failure(Exception("UID non disponibile"))
            Result.success(uid)
        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    // Restituisce l'UID dell'utente attualmente autenticato, se presente.
    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Predicato: indica se esiste una sessione utente attiva.
    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Termina la sessione utente corrente e ripulisce lo stato locale.
    override fun logout() = auth.signOut()
}