package com.example.gymlog_finale.data.firebase

// Sorgente Firestore + Storage dedicata ai log di progresso (peso e foto) dell'utente.

import android.content.Context
import android.net.Uri
import com.example.gymlog_finale.data.model.ProgressLog
import com.example.gymlog_finale.data.repository.ProgressRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File

// Classe FirebaseProgressSource: unità principale definita in questo file.
class FirebaseProgressSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProgressRepository {

    private val progressLogsCollection = firestore.collection("progressLogs")

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
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

    // Aggiunge un elemento alla collezione o allo stato correnti.
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

    // Persiste l'entità sulla sorgente dati (creazione o aggiornamento).
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

    // Aggiunge un elemento alla collezione o allo stato correnti.
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

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
    override suspend fun deleteProgressLog(logId: String): Result<Unit> {
        return try {
            progressLogsCollection.document(logId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
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