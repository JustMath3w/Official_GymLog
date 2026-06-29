package com.example.gymlog_finale.data.repository

import com.example.gymlog_finale.data.model.User

/**
 * Contratto per l'accesso ai dati utente e per le operazioni di account
 * (cambio password, reset, eliminazione). Le operazioni di account toccano
 * sia FirebaseAuth sia Firestore: l'implementazione coordina entrambi i lati.
 */
interface UserRepository {

    // === Documento utente Firestore ===

    /** Scrive o sovrascrive il documento utente in users/{uid} su Firestore. */
    suspend fun saveUser(user: User): Result<Unit>

    /** Recupera il documento utente da Firestore convertendolo nel modello User. */
    suspend fun getUser(uid: String): Result<User>

    /** Controlla se esiste già un documento utente su Firestore per l'uid dato. */
    suspend fun userExists(uid: String): Boolean

    /** Recupera il profilo dell'utente attualmente autenticato, null se nessuna sessione. */
    suspend fun fetchCurrentUser(): Result<User>?

    /** Restituisce l'uid dell'utente attualmente loggato, null se nessuno. */
    fun getCurrentUid(): String?

    /**
     * Aggiorna in modo PARZIALE solo i campi indicati senza toccare gli altri.
     * Se 'fields' contiene la chiave "username", l'implementazione si occupa anche
     * di aggiornare la collezione usernames/ per mantenere l'unicità.
     */
    suspend fun updateUserFields(uid: String, fields: Map<String, Any?>): Result<Unit>

    // === Ricerca / unicità username ===

    /**
     * Verifica se uno username (case-insensitive) è disponibile.
     * Se 'excludeUid' è valorizzato, ignora il documento appartenente a quell'uid
     * (caso: l'utente sta confermando il proprio username corrente).
     */
    suspend fun isUsernameAvailable(username: String, excludeUid: String? = null): Boolean

    /**
     * Ricerca utenti il cui username inizia con il prefisso fornito (case-insensitive).
     * Esclude l'utente corrente dai risultati. Massimo 20 risultati.
     */
    suspend fun searchUsersByUsername(prefix: String): Result<List<User>>

    // === Account: password / delete ===

    /**
     * Reautentica l'utente corrente con la sua password. Necessario prima di
     * cambiare password o eliminare l'account (richiesto da Firebase per
     * operazioni "sensibili" se l'ultimo login è troppo vecchio).
     */
    suspend fun reauthenticate(currentPassword: String): Result<Unit>

    /** Imposta una nuova password sull'utente Firebase Auth corrente. */
    suspend fun changePassword(newPassword: String): Result<Unit>

    /** Invia una mail di reset password Firebase all'indirizzo specificato. */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Elimina account: rimuove il documento Firestore + l'utente Firebase Auth.
     * Lato chiamante è consigliato chiamare prima reauthenticate() per evitare
     * l'eccezione "recent login required" di Firebase.
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Restituisce gli utenti visibili nella sezione Community,
     * escludendo l'utente autenticato.
     */
    suspend fun getAllUsersForCommunity(): Result<List<User>>

    /** Invalida la sessione Firebase corrente. */
    fun logout()
}