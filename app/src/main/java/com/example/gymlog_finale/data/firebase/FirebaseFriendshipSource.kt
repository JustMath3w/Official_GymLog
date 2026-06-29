package com.example.gymlog_finale.data.firebase

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

/**
 * Gestisce tutte le operazioni relative alla Community (Amicizie, Personal Trainer).
 * Si collega direttamente alle collezioni Firestore.
 */
class FirebaseFriendshipSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FriendshipRepository {

    // Riferimenti diretti alle varie "cartelle" (collezioni) nel database
    private val friendshipsCol get() = db.collection(Constants.FRIENDS_COLLECTION)
    private val requestsCol get() = db.collection(Constants.FRIEND_REQUESTS_COLLECTION)
    private val usersCol get() = db.collection(Constants.USERS_COLLECTION)

    // Ottiene l'ID dell'utente attualmente loggato
    private fun currentUid(): String? = auth.currentUser?.uid

    /**
     * Percorso per trovare la lista dei clienti di un Personal Trainer specifico.
     */
    private fun ptClientsSubcollection(userId: String) =
        usersCol.document(userId).collection("clients")

    /**
     * Crea un ID univoco per un'amicizia unendo i due ID in ordine alfabetico.
     */
    private fun friendshipId(a: String, b: String): String =
        if (a < b) "${a}_${b}" else "${b}_${a}"

    /**
     * Crea un ID per la richiesta che va dal mittente al destinatario.
     */
    private fun requestId(sender: String, receiver: String): String = "${sender}_${receiver}"

    /**
     * Crea un ID univoco per la relazione tra il PT e il suo cliente.
     */
    private fun ptRelationshipId(userId: String, clientId: String): String = "${userId}_${clientId}"

    /**
     * Converte in modo sicuro la data di creazione in millisecondi (per ordinare liste).
     */
    private fun anyCreatedAtToMillis(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Timestamp -> value.toDate().time
            else -> 0L
        }
    }

    /**
     * Ascolta in tempo reale se vengono aggiunti o rimossi amici.
     * @return Un flusso continuo (Flow) di aggiornamenti.
     */
    override fun observeFriendships(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Si iscrive ai cambiamenti del database dove l'utente compare nella lista "users"
        val registration = friendshipsCol
            .whereArrayContains("users", uid)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Friendship::class.java)?.copy(id = document.id)
                } ?: emptyList()

                trySend(items) // Invia la nuova lista alla UI
            }

        // Rimuove l'ascolto se l'utente chiude la pagina
        awaitClose { registration.remove() }
    }

    /**
     * Scarica i profili completi di tutti gli amici dell'utente.
     */
    override suspend fun fetchFriendsAsUsers(): Result<List<User>> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            // Prende tutti i documenti amicizia dell'utente
            val snapshot = friendshipsCol
                .whereArrayContains("users", uid).get().await()

            // Estrae solo gli ID degli amici (ignorando l'ID dell'utente stesso)
            val friendUids = snapshot.documents.mapNotNull { document ->
                (document.get("users") as? List<*>)?.firstOrNull { it != uid } as? String
            }

            if (friendUids.isEmpty()) return Result.success(emptyList())

            // Recupera e restituisce i dati completi degli amici visibili
            val users = fetchUsersByIds(friendUids)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rimuove un amico eliminando il documento dal database.
     */
    override suspend fun removeFriend(friendUid: String): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))
            friendshipsCol.document(friendshipId(uid, friendUid)).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ascolta in tempo reale le richieste di amicizia/PT ricevute.
     */
    override fun observeIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = requestsCol
            .whereEqualTo("receiverId", uid) // Cerca dove siamo noi i destinatari
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Filtra solo le richieste ancora in attesa (PENDING) e le ordina per data
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

    /**
     * Ascolta in tempo reale le richieste inviate (per poterle annullare se si vuole).
     */
    override fun observeOutgoingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUid() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = requestsCol
            .whereEqualTo("senderId", uid) // Cerca le richieste partite da noi
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

    /**
     * Controlla se l'utente specificato è un Personal Trainer leggendo il suo documento.
     */
    private fun isPersonalTrainerOf(doc: com.google.firebase.firestore.DocumentSnapshot): Boolean {
        return doc.getBoolean("personalTrainer")
            ?: doc.getBoolean("isPersonalTrainer")
            ?: false
    }

    /**
     * Crea e invia una nuova richiesta (Amicizia o PT Coaching).
     */
    override suspend fun sendFriendRequest(
        receiverId: String,
        requestType: String
    ): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            if (uid == receiverId) return Result.failure(Exception("Non puoi inviare una richiesta a te stesso"))

            // Se siamo già amici, blocca
            val friendshipDoc = friendshipsCol.document(friendshipId(uid, receiverId)).get().await()
            if (friendshipDoc.exists() && requestType == FriendRequestType.FRIENDSHIP.name) {
                return Result.failure(Exception("Siete già amici"))
            }

            // Se ho già inviato una richiesta in attesa, blocca
            val existing = requestsCol.document(requestId(uid, receiverId)).get().await()
            if (existing.exists() && existing.getString("status") == FriendRequestStatus.PENDING.name) {
                return Result.failure(Exception("Richiesta già inviata"))
            }

            // Se lui mi ha già inviato una richiesta in attesa, blocca
            val opposite = requestsCol.document(requestId(receiverId, uid)).get().await()
            if (opposite.exists() && opposite.getString("status") == FriendRequestStatus.PENDING.name) {
                return Result.failure(Exception("Esiste già una richiesta pendente tra voi"))
            }

            // Controlli speciali se la richiesta è per avere un PT
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

            // Se supera tutti i controlli, crea l'oggetto richiesta e lo salva
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

    /**
     * Scarica e calcola quante volte e con che costanza si è allenato un utente (Stats).
     */
    override suspend fun fetchUserStats(targetUid: String): com.example.gymlog_finale.data.model.FriendStats {
        return try {
            // Scarica tutti i log degli allenamenti conclusi da questo utente
            val workoutLogsSnap = db.collection(Constants.WORKOUT_LOGS_COLLECTION)
                .whereEqualTo("userId", targetUid)
                .get()
                .await()

            // Prende solo i timestamp dei giorni completati
            val completionDays: List<Long> = workoutLogsSnap.documents.mapNotNull {
                it.getLong("completedAt")
            }

            val workoutStreak = streakFromTimestamps(completionDays) // Giorni di fila
            val totalTrainingDays = completionDays.map { dayBucket(it) }.toSet().size

            // Conta quale esercizio ha fatto più volte
            val exerciseCounts = mutableMapOf<String, Int>()
            workoutLogsSnap.documents.forEach { doc ->
                val exercises = doc.get("exercises") as? List<*> ?: return@forEach
                exercises.forEach { ex ->
                    val name = (ex as? Map<*, *>)?.get("name") as? String ?: return@forEach
                    if (name.isNotBlank()) exerciseCounts[name] = (exerciseCounts[name] ?: 0) + 1
                }
            }
            val favoriteExercise = exerciseCounts.maxByOrNull { it.value }?.key

            // Fa la stessa cosa per la dieta (per contare la costanza)
            val dietLogsSnap = db.collection("dietLogs")
                .whereEqualTo("userId", targetUid)
                .get()
                .await()

            val dietDays = dietLogsSnap.documents.mapNotNull {
                it.getLong("data") ?: it.getLong("date") ?: it.getLong("createdAt")
            }
            val dietStreak = streakFromTimestamps(dietDays)

            // Controlla il nome del suo Personal Trainer, se ne ha uno
            val targetUserDoc = usersCol.document(targetUid).get().await()
            val ptUid = targetUserDoc.getString("hasPersonalTrainer")
            val ptName = if (!ptUid.isNullOrBlank()) {
                val ptDoc = usersCol.document(ptUid).get().await()
                ptDoc.getString("username")
            } else null

            // Restituisce le statistiche imballate pronte per la UI
            com.example.gymlog_finale.data.model.FriendStats(
                workoutStreakDays = workoutStreak,
                dietStreakDays = dietStreak,
                favoriteExercise = favoriteExercise,
                totalTrainingDays = totalTrainingDays,
                personalTrainerName = ptName
            )
        } catch (e: Exception) {
            com.example.gymlog_finale.data.model.FriendStats() // In caso di errore restituisce zeri
        }
    }

    /**
     * Arrotonda il tempo in millisecondi a "giorno esatto" per calcolare le serie.
     */
    private fun dayBucket(tsMs: Long): Long = tsMs / (24L * 60L * 60L * 1000L)

    /**
     * Calcola la "streak" (i giorni di fuoco consecutivi) contando a ritroso da oggi.
     */
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

    /**
     * L'utente corrente accetta una richiesta in arrivo.
     */
    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))

            val requestDoc = requestsCol.document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return Result.failure(Exception("Richiesta non trovata"))

            if (request.receiverId != uid) {
                return Result.failure(Exception("Non sei il destinatario della richiesta"))
            }

            // In base al tipo di richiesta fa operazioni diverse
            when (request.requestType) {
                FriendRequestType.FRIENDSHIP.name -> {
                    // Crea l'amicizia ufficiale
                    val pairId = friendshipId(request.senderId, request.receiverId)
                    val friendship = Friendship(
                        id = pairId,
                        users = listOf(request.senderId, request.receiverId),
                        createdAt = System.currentTimeMillis()
                    )

                    // Usa il "batch" per fare più scritture insieme in modo sicuro (tutte o nessuna)
                    val batch = db.batch()
                    batch.set(friendshipsCol.document(pairId), friendship) // Salva l'amicizia
                    batch.update(requestsCol.document(requestId), "status", FriendRequestStatus.ACCEPTED.name) // Segna accettata
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

                    // Scrive in modo sicuro che l'utente ora ha questo PT, crea la relazione e segna la richiesta accettata
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

    /**
     * Rifiuta una richiesta e le cambia lo stato in REJECTED.
     */
    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            requestsCol.document(requestId).update("status", FriendRequestStatus.REJECTED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Annulla una richiesta inviata per sbaglio (CANCELLED).
     */
    override suspend fun cancelFriendRequest(requestId: String): Result<Unit> {
        return try {
            requestsCol.document(requestId).update("status", FriendRequestStatus.CANCELLED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ascolta in tempo reale se si aggiungono o tolgono clienti per il PT corrente.
     */
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

    /**
     * Scarica i profili utente completi di tutti i clienti attuali del PT.
     */
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

    /**
     * Il PT rimuove un cliente (interrompendo il coaching).
     */
    override suspend fun removePtClient(clientUid: String): Result<Unit> {
        return try {
            val uid = currentUid() ?: return Result.failure(Exception("Non autenticato"))
            val clientDoc = usersCol.document(clientUid).get().await()

            // Elimina il cliente e resetta il suo "PT" nel profilo in contemporanea
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

    /**
     * Scarica profili utente suddividendoli a gruppi di 10 (limite Firebase per il whereIn).
     */
    private suspend fun fetchUsersByIds(uids: List<String>): List<User> {
        return uids
            .distinct()
            .chunked(10) // Evita il crash dividendo in mini-gruppi da 10
            .flatMap { chunk ->
                usersCol
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { documentToUser(it) }
            }
    }

    /**
     * Converte i dati grezzi del database nell'oggetto User di Kotlin.
     */
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