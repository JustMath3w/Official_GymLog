package com.example.gymlog_finale.data.repository

import android.util.Log
import com.example.gymlog_finale.data.model.Workout
import com.example.gymlog_finale.data.model.Exercise
import com.example.gymlog_finale.util.Constants
import com.example.gymlog_finale.data.model.WorkoutLog
import com.example.gymlog_finale.data.model.SplitPlan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository dedicato alla gestione delle schede di allenamento e dei log allenamento.
 * Le schede possono essere proprie (userId == utente) o assegnate da un PT (assignedTo == utente).
 */
class WorkoutRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tag = "WorkoutRepository"

    private val workoutsCollection = firestore.collection(Constants.WORKOUTS_COLLECTION)
    private val workoutLogsCollection = firestore.collection(Constants.WORKOUT_LOGS_COLLECTION)
    private val usersCollection = firestore.collection(Constants.USERS_COLLECTION)

    suspend fun saveWorkout(workout: Workout): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utente non autenticato"))

            val workoutId = if (workout.id.isEmpty()) {
                workoutsCollection.document().id
            } else {
                workout.id
            }

            val newWorkout = workout.copy(
                id = workoutId,
                userId = uid,
                createdAt = if (workout.createdAt == 0L) System.currentTimeMillis() else workout.createdAt
            )

            workoutsCollection.document(workoutId).set(newWorkout).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Il PT crea una scheda assegnata al cliente.
     * Il documento resta di proprietà del PT (userId == auth.uid) e referenzia il cliente via assignedTo
     * così le rules workouts consentono la create e la read sia al PT sia al cliente assegnato.
     */
    suspend fun sendWorkoutToFriend(workout: Workout, friendId: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Non loggato"))

            val senderDoc = usersCollection.document(currentUid).get().await()
            val senderName = senderDoc.getString("nome") ?: "Personal Trainer"
            val senderIsPT = senderDoc.getBoolean("personalTrainer")
                ?: senderDoc.getBoolean("isPersonalTrainer")
                ?: false

            val newWorkoutId = workoutsCollection.document().id

            val receiverDoc = usersCollection.document(friendId).get().await()
            val receiverName = receiverDoc.getString("nome") ?: "Amico"

            val workoutCopy = workout.copy(
                id = newWorkoutId,
                userId = currentUid,
                assignedTo = friendId,
                senderId = currentUid,
                senderName = senderName,
                senderIsPersonalTrainer = senderIsPT,
                receiverName = receiverName,
                isReceived = true,
                createdAt = System.currentTimeMillis()
            )

            workoutsCollection.document(newWorkoutId).set(workoutCopy).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Errore invio scheda", e)
            Result.failure(e)
        }
    }

    /**
     * Crea una nuova scheda dal PT assegnandola a un cliente sotto coaching.
     * userId resta del PT (consente create), assignedTo abilita la read al cliente.
     * Differisce da sendWorkoutToFriend perché parte dai dati raw del dialog (nome + esercizi + split).
     */
    suspend fun saveWorkoutForClient(
        clientUid: String,
        name: String,
        exercises: List<Exercise>,
        splitType: String
    ): Result<Unit> {
        return try {
            val ptUid = auth.currentUser?.uid ?: return Result.failure(Exception("Non loggato"))

            // Nome leggibile per la targhetta "Creato da PT: Nome"
            val ptDoc = usersCollection.document(ptUid).get().await()
            val ptName = ptDoc.getString("nome") ?: "Personal Trainer"

            val newWorkoutId = workoutsCollection.document().id

            val clientDoc = usersCollection.document(clientUid).get().await()
            val clientName = clientDoc.getString("nome") ?: "Cliente"

            val workout = Workout(
                id = newWorkoutId,
                userId = ptUid,
                assignedTo = clientUid,
                name = name,
                exercises = exercises.filter { it.name.isNotBlank() },
                splitType = splitType,
                senderId = ptUid,
                senderName = ptName,
                senderIsPersonalTrainer = true,
                receiverName = clientName,
                isReceived = true,
                createdAt = System.currentTimeMillis()
            )

            workoutsCollection.document(newWorkoutId).set(workout).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Errore creazione scheda per cliente", e)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /**
     * Emette in realtime l'unione delle schede di proprietà dell'utente
     * con quelle che gli sono state assegnate da un PT.
     * I due snapshot sono gestiti separatamente e uniti deduplicando per id.
     */
    fun getWorkoutsRealtime(): Flow<List<Workout>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            return@callbackFlow
        }

        var owned: List<Workout> = emptyList()
        var assigned: List<Workout> = emptyList()

        fun emitMerged() {
            val merged = (owned + assigned)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            trySend(merged)
        }

        val listenerOwned = workoutsCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                owned = snapshot.documents.mapNotNull {
                    try { it.toObject(Workout::class.java) } catch (_: Exception) { null }
                }
                emitMerged()
            }

        val listenerAssigned = workoutsCollection
            .whereEqualTo("assignedTo", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                assigned = snapshot.documents.mapNotNull {
                    try { it.toObject(Workout::class.java) } catch (_: Exception) { null }
                }
                emitMerged()
            }

        awaitClose {
            listenerOwned.remove()
            listenerAssigned.remove()
        }
    }

    suspend fun saveWorkoutLog(log: WorkoutLog): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utente non autenticato"))
            val logId = workoutLogsCollection.document().id
            val newLog = log.copy(id = logId, userId = uid)
            workoutLogsCollection.document(logId).set(newLog).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Errore salvataggio log allenamento", e)
            Result.failure(e)
        }
    }

    fun getWorkoutLogsRealtime(): Flow<List<WorkoutLog>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = workoutLogsCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val logs = snapshot.documents.mapNotNull {
                    try { it.toObject(WorkoutLog::class.java) } catch (_: Exception) { null }
                }
                trySend(logs.sortedByDescending { it.completedAt })
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveSplitPlan(plan: SplitPlan): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utente non autenticato"))
            val splitStringMap = plan.split.mapKeys { it.key.toString() }
            val data = mapOf(
                "startDate" to plan.startDate,
                "endDate" to plan.endDate,
                "split" to splitStringMap,
                "overrides" to plan.overrides
            )
            firestore.collection("user_splits")
                .document(uid)
                .set(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Errore salvataggio piano split", e)
            Result.failure(e)
        }
    }

    suspend fun getSplitPlan(): Result<SplitPlan> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Utente non autenticato"))
            val doc = firestore.collection("user_splits").document(uid).get().await()
            if (doc.exists()) {
                val startDate = doc.getLong("startDate") ?: 0L
                val endDate = doc.getLong("endDate") ?: 0L
                val rawSplit = (doc.get("split") as? Map<*, *>)
                    ?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap()
                val split = rawSplit.mapNotNull {
                    val keyInt = it.key.toIntOrNull()
                    if (keyInt != null) keyInt to it.value else null
                }.toMap()
                val overrides = (doc.get("overrides") as? Map<*, *>)
                    ?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap()
                Result.success(SplitPlan(startDate, endDate, split, overrides))
            } else {
                val defaultSplit = mapOf(
                    0 to "Push", 1 to "Pull", 2 to "Rest",
                    3 to "Legs", 4 to "Cardio", 5 to "Addome", 6 to "Rest"
                )
                Result.success(SplitPlan(0L, 0L, defaultSplit, emptyMap()))
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore recupero piano split", e)
            Result.failure(e)
        }
    }
}