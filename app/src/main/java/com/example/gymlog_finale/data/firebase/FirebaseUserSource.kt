package com.example.gymlog_finale.data.firebase

// Sorgente Firestore per il documento users/{uid}, con gestione unicità username e aggiornamento dati profilo.

import com.example.gymlog_finale.data.model.User
import com.example.gymlog_finale.util.Constants
import com.example.gymlog_finale.data.repository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Classe FirebaseUserSource: unità principale definita in questo file.
class FirebaseUserSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : UserRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCol get() = db.collection(Constants.USERS_COLLECTION)
    private val usernamesCol get() = db.collection(Constants.USERNAMES_COLLECTION)

    // Funzione di supporto interna alla classe.
    private fun documentToUser(document: DocumentSnapshot): User? {
        if (!document.exists()) return null

        return User(
            uid = document.getString("uid") ?: document.id,
            nome = document.getString("nome") ?: "",
            cognome = document.getString("cognome") ?: "",
            username = document.getString("username") ?: "",
            email = document.getString("email") ?: "",
            annoDiNascita = document.getLong("annoDiNascita")?.toInt() ?: 0,
            altezza = document.getLong("altezza")?.toInt() ?: 0,
            peso = document.getDouble("peso") ?: 0.0,
            obiettivo = document.getString("obiettivo") ?: "",
            isPersonalTrainer = document.getBoolean("personalTrainer") ?: false,
            hasPersonalTrainer = document.getString("hasPersonalTrainer"),
            photoUrl = document.getString("photoUrl") ?: "",
            createdAt = document.getLong("createdAt") ?: 0L
        )
    }

    // Funzione di supporto interna alla classe.
    private fun normalizeUsername(username: String): String {
        return username.trim().lowercase()
    }

    // Persiste l'entità sulla sorgente dati (creazione o aggiornamento).
    private suspend fun saveUsernameIndex(uid: String, username: String) {
        val normalized = normalizeUsername(username)
        if (normalized.isBlank()) return

        usernamesCol.document(normalized)
            .set(
                mapOf(
                    "uid" to uid,
                    "username" to username.trim(),
                    "usernameLowercase" to normalized
                )
            )
            .await()
    }

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
    private suspend fun deleteUsernameIndex(username: String?) {
        val normalized = username?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return
        usernamesCol.document(normalized).delete().await()
    }

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    override suspend fun getAllUsersForCommunity(): Result<List<User>> {
        return try {
            val currentUid = auth.currentUser?.uid

            val snapshot = usersCol
                .orderBy("username")
                .get()
                .await()

            val users = snapshot.documents
                .mapNotNull { documentToUser(it) }
                .filter { it.uid != currentUid }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Persiste l'entità sulla sorgente dati (creazione o aggiornamento).
    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val cleanUsername = user.username.trim()

            val oldSnap = usersCol.document(user.uid).get().await()
            val oldUsername = oldSnap.getString("username")

            val userMap = hashMapOf(
                "uid" to user.uid,
                "nome" to user.nome,
                "cognome" to user.cognome,
                "email" to user.email,
                "username" to cleanUsername,
                "annoDiNascita" to user.annoDiNascita,
                "altezza" to user.altezza,
                "peso" to user.peso,
                "obiettivo" to user.obiettivo,
                "personalTrainer" to user.isPersonalTrainer,
                "hasPersonalTrainer" to user.hasPersonalTrainer,
                "photoUrl" to user.photoUrl,
                "createdAt" to user.createdAt
            )

            usersCol.document(user.uid).set(userMap).await()

            if (!oldUsername.equals(cleanUsername, ignoreCase = true)) {
                deleteUsernameIndex(oldUsername)
            }

            if (cleanUsername.isNotBlank()) {
                saveUsernameIndex(user.uid, cleanUsername)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    override suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = usersCol.document(uid).get().await()
            val user = documentToUser(snapshot)
                ?: return Result.failure(Exception(Constants.ERROR_USER_NOT_FOUND))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    override suspend fun userExists(uid: String): Boolean {
        return try {
            usersCol.document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    override suspend fun fetchCurrentUser(): Result<User>? {
        val uid = auth.currentUser?.uid ?: return null
        return getUser(uid)
    }

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    override fun getCurrentUid(): String? = auth.currentUser?.uid

    // Aggiorna i campi indicati dell'entità sulla sorgente dati.
    override suspend fun updateUserFields(
        uid: String,
        fields: Map<String, Any?>
    ): Result<Unit> {
        return try {
            val remapped = fields.mapKeys { (key, _) ->
                if (key == "isPersonalTrainer") "personalTrainer" else key
            }.toMutableMap()

            val newUsername = (remapped["username"] as? String)?.trim()

            if (!newUsername.isNullOrBlank()) {
                val oldSnap = usersCol.document(uid).get().await()
                val oldUsername = oldSnap.getString("username")

                if (!oldUsername.equals(newUsername, ignoreCase = true)) {
                    deleteUsernameIndex(oldUsername)
                }

                saveUsernameIndex(uid, newUsername)
                remapped["username"] = newUsername
            }

            val firestoreFields = remapped.filterValues { it != null }
            usersCol.document(uid).update(firestoreFields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Predicato che verifica una condizione booleana sullo stato.
    override suspend fun isUsernameAvailable(username: String, excludeUid: String?): Boolean {
        val normalized = normalizeUsername(username)
        if (normalized.isBlank()) return false

        return try {
            val usernameDoc = usernamesCol.document(normalized).get().await()
            if (!usernameDoc.exists()) {
                true
            } else {
                val ownerUid = usernameDoc.getString("uid")

                when {
                    ownerUid.isNullOrBlank() -> true
                    excludeUid != null && ownerUid == excludeUid -> true
                    else -> {
                        val ownerUserDoc = usersCol.document(ownerUid).get().await()
                        !ownerUserDoc.exists().not()
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    override suspend fun searchUsersByUsername(prefix: String): Result<List<User>> {
        return try {
            val normalized = normalizeUsername(prefix)
            if (normalized.isBlank()) return Result.success(emptyList())

            val currentUid = auth.currentUser?.uid

            val usernameSnapshot = usernamesCol
                .orderBy("usernameLowercase")
                .startAt(normalized)
                .endAt(normalized + "\uf8ff")
                .get()
                .await()

            val uids = usernameSnapshot.documents
                .mapNotNull { it.getString("uid") }
                .distinct()
                .take(20)

            val users = uids.mapNotNull { uid ->
                val doc = usersCol.document(uid).get().await()
                documentToUser(doc)
            }.filter { it.uid != currentUid }
                .sortedBy { it.username.lowercase() }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    override suspend fun reauthenticate(currentPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("Nessuna sessione attiva"))
            val email = user.email
                ?: return Result.failure(Exception("Email non disponibile sull'account"))

            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gestisce operazioni relative alla password (aggiornamento o reset).
    override suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("Nessuna sessione attiva"))
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gestisce operazioni relative alla password (aggiornamento o reset).
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
                ?: return Result.failure(Exception("Nessuna sessione attiva"))
            val uid = user.uid

            val userDoc = usersCol.document(uid).get().await()
            val username = userDoc.getString("username")

            try {
                deleteUsernameIndex(username)
            } catch (_: Exception) {
            }

            try {
                usersCol.document(uid).delete().await()
            } catch (_: Exception) {
            }

            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Termina la sessione utente corrente e ripulisce lo stato locale.
    override fun logout() = auth.signOut()
}