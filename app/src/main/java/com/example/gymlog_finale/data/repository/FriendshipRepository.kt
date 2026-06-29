package com.example.gymlog_finale.data.repository


import com.example.gymlog_finale.data.model.FriendRequest
import com.example.gymlog_finale.data.model.Friendship
import com.example.gymlog_finale.data.model.PtRelationship
import com.example.gymlog_finale.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contratto per la gestione della community: amicizie, richieste, clienti PT e blocchi.
 */
interface FriendshipRepository {

    /**
     * Osserva in tempo reale le amicizie dell'utente corrente.
     */
    fun observeFriendships(): Flow<List<Friendship>>

    /**
     * Recupera i profili completi degli amici dell'utente corrente.
     */
    suspend fun fetchFriendsAsUsers(): Result<List<User>>

    /**
     * Rimuove l'amicizia tra utente corrente e amico indicato.
     */
    suspend fun removeFriend(friendUid: String): Result<Unit>

    /**
     * Osserva in tempo reale le richieste ricevute in stato PENDING.
     */
    fun observeIncomingRequests(): Flow<List<FriendRequest>>

    /**
     * Osserva in tempo reale le richieste inviate in stato PENDING.
     */
    fun observeOutgoingRequests(): Flow<List<FriendRequest>>

    /**
     * Invia una richiesta all'utente indicato specificandone il tipo.
     */
    suspend fun sendFriendRequest(
        receiverId: String,
        requestType: String
    ): Result<Unit>

    /**
     * Accetta una richiesta ricevuta e crea le entità conseguenti.
     */
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>

    /**
     * Rifiuta una richiesta ricevuta.
     */
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>

    /**
     * Annulla una richiesta inviata ancora pendente.
     */
    suspend fun cancelFriendRequest(requestId: String): Result<Unit>

    /**
     * Osserva in tempo reale le relazioni PT dell'utente corrente se è un personal trainer.
     */
    fun observePtClients(): Flow<List<PtRelationship>>

    /**
     * Recupera i profili completi dei clienti del PT corrente.
     */
    suspend fun fetchPtClientsAsUsers(): Result<List<User>>

    /**
     * Rimuove la relazione PT-cliente tra utente corrente PT e cliente indicato.
     */
    suspend fun removePtClient(clientUid: String): Result<Unit>



    /**
     * Restituisce le statistiche aggregate di un utente per la card community.
     */
    suspend fun fetchUserStats(targetUid: String): com.example.gymlog_finale.data.model.FriendStats
}