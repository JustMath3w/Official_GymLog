package com.example.gymlog_finale.data.firebase

import android.content.Context
import android.net.Uri
import com.example.gymlog_finale.data.model.ProgressLog
import com.example.gymlog_finale.data.repository.ProgressRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Implementazione di ProgressRepository che usa Cloud Firestore
 * per leggere e scrivere i log di progresso dell'utente.
 * Le foto progresso vengono copiate nella memoria interna privata dell'app.
 */
class FirebaseProgressSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProgressRepository {

    private val progressLogsCollection = firestore.collection("progressLogs")

    /**
     * Recupera tutti i log di progresso dell'utente ordinati per timestamp crescente.
     */
    override suspend fun getProgressLogs(userId: String): Result<List<ProgressLog>> {
        return try {
            val snapshot = progressLogsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp")
                .get()
                .await()

            val logs = snapshot.documents.map { document ->
                ProgressLog(
                    id = document.id,
                    userId = document.getString("userId").orEmpty(),
                    timestamp = document.getLong("timestamp") ?: 0L,
                    weightKg = document.getDouble("weightKg") ?: 0.0,
                    photoUrl = document.getString("photoUrl").orEmpty()
                )
            }

            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva un nuovo log di progresso su Firestore.
     * Se l'id è vuoto, Firestore genera automaticamente il documento.
     */
    override suspend fun addProgressLog(progressLog: ProgressLog): Result<Unit> {
        return try {
            val data = hashMapOf(
                "userId" to progressLog.userId,
                "timestamp" to progressLog.timestamp,
                "weightKg" to progressLog.weightKg,
                "photoUrl" to progressLog.photoUrl
            )

            if (progressLog.id.isBlank()) {
                progressLogsCollection.add(data).await()
            } else {
                progressLogsCollection.document(progressLog.id).set(data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copia una foto selezionata dall'utente nella cartella privata dell'app
     * e restituisce il path assoluto del file salvato.
     */
    private fun savePhotoToInternalStorage(context: Context, userId: String, photoUri: Uri, timestamp: Long): Result<String> {

        return try {
            val progressPhotosDir = File(context.filesDir, "progress_photos/$userId")

            if (!progressPhotosDir.exists()) {
                progressPhotosDir.mkdirs()
            }

            val destinationFile = File(progressPhotosDir, "$timestamp.jpg")

            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Impossibile leggere il file selezionato.")

            Result.success(destinationFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva opzionalmente la foto nella memoria interna dell'app
     * e registra poi il log completo su Firestore.
     */
    suspend fun addProgressLogWithPhoto(
        context: Context,
        userId: String,
        weightKg: Double,
        localPhotoUri: Uri?
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()

            val localPhotoPath = if (localPhotoUri != null) {
                savePhotoToInternalStorage(
                    context = context,
                    userId = userId,
                    photoUri = localPhotoUri,
                    timestamp = timestamp
                ).getOrElse { throw it }
            } else {
                ""
            }

            val progressLog = ProgressLog(
                userId = userId,
                timestamp = timestamp,
                weightKg = weightKg,
                photoUrl = localPhotoPath
            )

            addProgressLog(progressLog)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un log di progresso esistente tramite id documento.
     */
    override suspend fun deleteProgressLog(logId: String): Result<Unit> {
        return try {
            progressLogsCollection.document(logId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un log di progresso e prova a rimuovere anche il file foto locale associato.
     */
    suspend fun deleteProgressLogWithPhoto(
        logId: String,
        localPhotoPath: String
    ): Result<Unit> {
        return try {
            if (localPhotoPath.isNotBlank()) {
                val localFile = File(localPhotoPath)
                if (localFile.exists()) {
                    localFile.delete()
                }
            }

            progressLogsCollection.document(logId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}