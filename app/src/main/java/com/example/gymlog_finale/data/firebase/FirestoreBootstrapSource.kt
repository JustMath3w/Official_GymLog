package com.example.gymlog_finale.data.firebase

// Sorgente Firestore che al primo avvio popola le collezioni di base con dati di default (es. alimenti comuni).

import com.example.gymlog_finale.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// Classe FirestoreBootstrapSource: unità principale definita in questo file.
class FirestoreBootstrapSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    suspend fun ensureUserDocument(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Nessun utente autenticato"))

            val uid = currentUser.uid
            val email = currentUser.email.orEmpty()
            val now = System.currentTimeMillis()

            db.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .set(
                    mapOf(
                        "uid" to uid,
                        "nome" to "",
                        "cognome" to "",
                        "email" to email,
                        "username" to "",
                        "annoDiNascita" to 0,
                        "altezza" to 0,
                        "peso" to 0.0,
                        "obiettivo" to "",
                        "personalTrainer" to false,
                        "photoUrl" to "",
                        "createdAt" to now
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}