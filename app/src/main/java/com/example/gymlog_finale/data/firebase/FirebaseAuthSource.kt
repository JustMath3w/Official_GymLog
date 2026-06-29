package com.example.gymlog_finale.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.gymlog_finale.data.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class FirebaseAuthSource : AuthRepository {

    // Istanza principale per comunicare con Firebase Auth
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** 
     * Registra un nuovo utente con email e password.
     * @return Result con l'UID generato in caso di successo.
     */
    override suspend fun register(email: String, password: String): Result<String> {
        return try {
            // Invia i dati a Firebase e attende la risposta
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            // Estrae l'UID. Se è null, lancia un'eccezione
            val uid = result.user?.uid ?: return Result.failure(Exception("UID non disponibile"))
            Result.success(uid)
        } catch (e: Exception) {
            // Cattura errori come "email già in uso" o "password troppo corta"
            Result.failure(e)
        }
    }

    /** 
     * Esegue il login con email e password.
     * @return Result<Unit> (successo senza dati extra) o errore.
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // Autentica l'utente e attende la risposta
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Cattura errori come "credenziali errate"
            Result.failure(e)
        }
    }

    /** 
     * Effettua il login utilizzando un token fornito da Google.
     * @return Result con l'UID dell'utente.
     */
    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            // Converte il token di Google in credenziali Firebase
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            // Effettua l'accesso con le credenziali generate
            val result = auth.signInWithCredential(credential).await()
            // Estrae l'UID utile per la registrazione nel database
            val uid = result.user?.uid ?: return Result.failure(Exception("UID non disponibile"))
            Result.success(uid)
        } catch (e: Exception) {
            // Cattura errori legati a token scaduti o non validi
            Result.failure(e)
        }
    }

    /** Restituisce l'oggetto FirebaseUser completo dell'utente attualmente loggato. */
    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    /** Restituisce solo l'ID (UID) dell'utente corrente. */
    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Verifica se c'è una sessione utente attiva. */
    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /** Effettua il logout cancellando la sessione corrente. */
    override fun logout() = auth.signOut()
}