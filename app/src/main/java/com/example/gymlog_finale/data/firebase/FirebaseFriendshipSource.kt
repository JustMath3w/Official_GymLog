package com.example.gymlog_finale.data.firebase

// Sorgente Firestore per amicizie, richieste di amicizia e relazione Personal Trainer/cliente; espone flussi realtime tramite callbackFlow.

import com.example.gymlog_finale.data.model.FriendRequest
import com.example.gymlog_finale.data.model.FriendRequestStatus
import com.example.gymlog_finale.data.model.FriendRequestType
import com.example.gymlog_finale.data.model.Friendship
import com.example.gymlog_finale.data.model.PtRelationship
import com.example.gymlog_finale.data.model.User
import com.example.gymlog_finale.util.Constants
import com.example.gymlog_finale.data.repository.FriendshipRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Classe FirebaseFriendshipSource: unità principale definita in questo file.
class FirebaseFriendshipSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FriendshipRepository {

    private val friendshipsCol get() = db.collection(Constants.FRIENDS_COLLECTION)
    private val requestsCol get() = db.collection(Constants.FRIEND_REQUESTS_COLLECTION)
    private val usersCol get() = db.collection(Constants.USERS_COLLECTION)

    // Funzione di supporto interna alla classe.
    private fun currentUid(): String? = auth.currentUser?.uid

    // Funzione di supporto interna alla classe.
    private fun ptClientsSubcollection(userId: String) =
        usersCol.document(userId).collection("clients")

    // Funzione di supporto interna alla classe.
    private fun friendshipId(a: String, b: String): String =
        if (a < b) "${a}_${b}" else "${b}_${a}"

    // Funzione di supporto interna alla classe.
    private fun requestId(sender: String, receiver: String): String = "${sender}_${receiver}"

    // Funzione di supporto interna alla classe.
    private fun ptRelationshipId(userId: String, clientId: String): String = "${userId}_${clientId}"

    // Funzione di supporto interna alla classe.
    private fun anyCreatedAtToMillis(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Timestamp -> value.toDate().time
            else -> 0L
        }
    }

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    override fun observeFriendships(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = friendshipsCol
            .whereArrayContains("users", uid)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Friendship::class.java)?.copy(id = document.id)
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    override suspend fun fetchFriendsAsUsers(): Result<List<User>> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            val snapshot = friendshipsCol
                .whereArrayContains("users", uid).get().await()

            val friendUids = snapshot.documents.mapNotNull { document ->
                (document.get("users") as? List<*>)?.firstOrNull { it != uid } as? String
            }

            if (friendUids.isEmpty()) return Result.success(emptyList())

            val users = fetchUsersByIds(friendUids)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Elimina la relazione o l'elemento indicato.
    override suspend fun removeFriend(friendUid: String): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))
            friendshipsCol.document(friendshipId(uid, friendUid)).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    override fun observeIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = requestsCol
            .whereEqualTo("receiverId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(FriendRequest::class.java)?.copy(id = document.id)
                    }
                    ?.filter { it.status == FriendRequestStatus.PENDING.name }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()

                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    override fun observeOutgoingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = requestsCol
            .whereEqualTo("senderId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.toObject(FriendRequest::class.java)?.copy(id = document.id)
                    }
                    ?.filter { it.status == FriendRequestStatus.PENDING.name }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()

                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    // Predicato che verifica una condizione booleana sullo stato.
    private fun isPersonalTrainerOf(doc: com.google.firebase.firestore.DocumentSnapshot): Boolean {
        return doc.getBoolean("personalTrainer")
            ?: doc.getBoolean("isPersonalTrainer")
            ?: false
    }

    // Invia la richiesta o il messaggio indicati.
    override suspend fun sendFriendRequest(
        receiverId: String,
        requestType: String
    ): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            if (uid == receiverId) return Result.failure(Exception("Non puoi inviare una richiesta a te stesso"))

            val friendshipDoc = friendshipsCol.document(friendshipId(uid, receiverId)).get().await()
            if (friendshipDoc.exists() && requestType == FriendRequestType.FRIENDSHIP.name) {
                return Result.failure(Exception("Siete già amici"))
            }

            val existing = requestsCol.document(requestId(uid, receiverId)).get().await()
            if (existing.exists() && existing.getString("status") == FriendRequestStatus.PENDING.name) {
                return Result.failure(Exception("Richiesta già inviata"))
            }

            val opposite = requestsCol.document(requestId(receiverId, uid)).get().await()
            if (opposite.exists() && opposite.getString("status") == FriendRequestStatus.PENDING.name) {
                return Result.failure(Exception("Esiste già una richiesta pendente tra voi"))
            }

            if (requestType == FriendRequestType.PT_COACHING.name) {
                val receiverDoc = usersCol.document(receiverId).get().await()
                if (!receiverDoc.exists()) return Result.failure(Exception("Utente non trovato"))

                if (!isPersonalTrainerOf(receiverDoc)) {
                    return Result.failure(Exception("Puoi inviare questa richiesta solo a un personal trainer"))
                }

                val senderDoc = usersCol.document(uid).get().await()
                if (!senderDoc.getString("hasPersonalTrainer").isNullOrBlank()) {
                    return Result.failure(Exception("Sei già seguito da un personal trainer"))
                }

                val existingClientDoc = ptClientsSubcollection(receiverId).document(uid).get().await()
                if (existingClientDoc.exists()) {
                    return Result.failure(Exception("Sei già seguito da questo personal trainer"))
                }
            }

            val request = FriendRequest(
                id = requestId(uid, receiverId),
                senderId = uid,
                receiverId = receiverId,
                status = FriendRequestStatus.PENDING.name,
                requestType = requestType,
                createdAt = System.currentTimeMillis()
            )

            requestsCol.document(request.id).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    override suspend fun fetchUserStats(targetUid: String): com.example.gymlog_finale.data.model.FriendStats {
        return try {

            val workoutLogsSnap = db.collection(Constants.WORKOUT_LOGS_COLLECTION)
                .whereEqualTo("userId", targetUid)
                .get()
                .await()

            val completionDays: List<Long> = workoutLogsSnap.documents.mapNotNull {
                it.getLong("completedAt")
            }

            val workoutStreak = streakFromTimestamps(completionDays)
            val totalTrainingDays = completionDays.map { dayBucket(it) }.toSet().size

            val exerciseCounts = mutableMapOf<String, Int>()
            workoutLogsSnap.documents.forEach { doc ->
                val exercises = doc.get("exercises") as? List<*> ?: return@forEach
                exercises.forEach { ex ->
                    val name = (ex as? Map<*, *>)?.get("name") as? String ?: return@forEach
                    if (name.isNotBlank()) exerciseCounts[name] = (exerciseCounts[name] ?: 0) + 1
                }
            }
            val favoriteExercise = exerciseCounts.maxByOrNull { it.value }?.key

            val dietLogsSnap = db.collection("dietLogs")
                .whereEqualTo("userId", targetUid)
                .get()
                .await()

            val dietDays = dietLogsSnap.documents.mapNotNull {
                it.getLong("data") ?: it.getLong("date") ?: it.getLong("createdAt")
            }
            val dietStreak = streakFromTimestamps(dietDays)

            val targetUserDoc = usersCol.document(targetUid).get().await()
            val ptUid = targetUserDoc.getString("hasPersonalTrainer")
            val ptName = if (!ptUid.isNullOrBlank()) {
                val ptDoc = usersCol.document(ptUid).get().await()
                ptDoc.getString("username")
            } else null

            com.example.gymlog_finale.data.model.FriendStats(
                workoutStreakDays = workoutStreak,
                dietStreakDays = dietStreak,
                favoriteExercise = favoriteExercise,
                totalTrainingDays = totalTrainingDays,
                personalTrainerName = ptName
            )
        } catch (e: Exception) {
            com.example.gymlog_finale.data.model.FriendStats()
        }
    }

    // Funzione di supporto interna alla classe.
    private fun dayBucket(tsMs: Long): Long = tsMs / (24L * 60L * 60L * 1000L)

    // Funzione di supporto interna alla classe.
    private fun streakFromTimestamps(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { dayBucket(it) }.toSortedSet()
        val today = dayBucket(System.currentTimeMillis())
        var streak = 0
        var cursor = today
        while (days.contains(cursor)) {
            streak++
            cursor -= 1
        }
        return streak
    }

    // Accetta la richiesta ricevuta e aggiorna il relativo stato.
    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            val requestDoc = requestsCol.document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return Result.failure(Exception("Richiesta non trovata"))

            if (request.receiverId != uid) {
                return Result.failure(Exception("Non sei il destinatario della richiesta"))
            }

            when (request.requestType) {
                FriendRequestType.FRIENDSHIP.name -> {

                    val pairId = friendshipId(request.senderId, request.receiverId)
                    val friendship = Friendship(
                        id = pairId,
                        users = listOf(request.senderId, request.receiverId),
                        createdAt = System.currentTimeMillis()
                    )

                    val batch = db.batch()
                    batch.set(friendshipsCol.document(pairId), friendship)
                    batch.update(requestsCol.document(requestId), "status", FriendRequestStatus.ACCEPTED.name)
                    batch.commit().await()
                }

                FriendRequestType.PT_COACHING.name -> {
                    val trainerUid = request.receiverId
                    val clientUid = request.senderId

                    val trainerDoc = usersCol.document(trainerUid).get().await()
                    if (!isPersonalTrainerOf(trainerDoc)) return Result.failure(Exception("Solo un PT può accettare"))

                    val clientDoc = usersCol.document(clientUid).get().await()
                    if (!clientDoc.getString("hasPersonalTrainer").isNullOrBlank()) {
                        return Result.failure(Exception("Utente già seguito da un PT"))
                    }

                    val batch = db.batch()
                    batch.update(usersCol.document(clientUid), "hasPersonalTrainer", trainerUid)
                    batch.set(
                        ptClientsSubcollection(trainerUid).document(clientUid),
                        mapOf("clientId" to clientUid, "createdAt" to FieldValue.serverTimestamp())
                    )
                    batch.update(requestsCol.document(requestId), "status", FriendRequestStatus.ACCEPTED.name)
                    batch.commit().await()
                }
                else -> return Result.failure(Exception("Tipo richiesta non supportato"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Rifiuta la richiesta ricevuta.
    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            requestsCol.document(requestId).update("status", FriendRequestStatus.REJECTED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Annulla l'operazione in corso o la richiesta pendente.
    override suspend fun cancelFriendRequest(requestId: String): Result<Unit> {
        return try {
            requestsCol.document(requestId).update("status", FriendRequestStatus.CANCELLED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    override fun observePtClients(): Flow<List<PtRelationship>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = ptClientsSubcollection(uid)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { document ->
                    val clientId = document.getString("clientId") ?: return@mapNotNull null
                    PtRelationship(
                        id = ptRelationshipId(uid, clientId),
                        ptId = uid,
                        clientId = clientId,
                        createdAt = anyCreatedAtToMillis(document.get("createdAt"))
                    )
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    override suspend fun fetchPtClientsAsUsers(): Result<List<User>> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            val snapshot = ptClientsSubcollection(uid).get().await()
            val clientUids = snapshot.documents.mapNotNull { it.getString("clientId") }

            if (clientUids.isEmpty()) return Result.success(emptyList())

            val users = fetchUsersByIds(clientUids)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Elimina la relazione o l'elemento indicato.
    override suspend fun removePtClient(clientUid: String): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))
            val clientDoc = usersCol.document(clientUid).get().await()

            val batch = db.batch()
            batch.delete(ptClientsSubcollection(uid).document(clientUid))
            if (clientDoc.getString("hasPersonalTrainer") == uid) {
                batch.update(usersCol.document(clientUid), "hasPersonalTrainer", null)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    private suspend fun fetchUsersByIds(uids: List<String>): List<User> {
        return uids
            .distinct()
            .chunked(10)
            .flatMap { chunk ->
                usersCol
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { documentToUser(it) }
            }
    }

    // Funzione di supporto interna alla classe.
    private fun documentToUser(document: com.google.firebase.firestore.DocumentSnapshot): User? {
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
            isPersonalTrainer = document.getBoolean("personalTrainer") ?: document.getBoolean("isPersonalTrainer") ?: false,
            hasPersonalTrainer = document.getString("hasPersonalTrainer"),
            photoUrl = document.getString("photoUrl") ?: "",
            createdAt = document.getLong("createdAt") ?: 0L
        )
    }
}