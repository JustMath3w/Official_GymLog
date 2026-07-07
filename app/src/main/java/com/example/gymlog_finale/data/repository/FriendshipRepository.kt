package com.example.gymlog_finale.data.repository

// Repository per amicizie, richieste PT/amico e ricerca utenti, costruito sopra FirebaseFriendshipSource.

import com.example.gymlog_finale.data.model.FriendRequest
import com.example.gymlog_finale.data.model.Friendship
import com.example.gymlog_finale.data.model.PtRelationship
import com.example.gymlog_finale.data.model.User
import kotlinx.coroutines.flow.Flow

// Interfaccia FriendshipRepository: contratto pubblico del modulo.
interface FriendshipRepository {

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    fun observeFriendships(): Flow<List<Friendship>>

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    suspend fun fetchFriendsAsUsers(): Result<List<User>>

    // Elimina la relazione o l'elemento indicato.
    suspend fun removeFriend(friendUid: String): Result<Unit>

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    fun observeIncomingRequests(): Flow<List<FriendRequest>>

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    fun observeOutgoingRequests(): Flow<List<FriendRequest>>

    // Invia la richiesta o il messaggio indicati.
    suspend fun sendFriendRequest(
        receiverId: String,
        requestType: String
    ): Result<Unit>

    // Accetta la richiesta ricevuta e aggiorna il relativo stato.
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>

    // Rifiuta la richiesta ricevuta.
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>

    // Annulla l'operazione in corso o la richiesta pendente.
    suspend fun cancelFriendRequest(requestId: String): Result<Unit>

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    fun observePtClients(): Flow<List<PtRelationship>>

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    suspend fun fetchPtClientsAsUsers(): Result<List<User>>

    // Elimina la relazione o l'elemento indicato.
    suspend fun removePtClient(clientUid: String): Result<Unit>

    // Richiede al servizio remoto i dati indicati e li restituisce al chiamante.
    suspend fun fetchUserStats(targetUid: String): com.example.gymlog_finale.data.model.FriendStats
}