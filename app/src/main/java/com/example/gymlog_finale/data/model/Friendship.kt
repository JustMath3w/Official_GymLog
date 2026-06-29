package com.example.gymlog_finale.data.model

/**
 * Lo stato attuale di una richiesta nella Community.
 */
enum class FriendRequestStatus {
    PENDING,    // In attesa di risposta
    ACCEPTED,   // Accettata
    REJECTED,   // Rifiutata dal destinatario
    CANCELLED   // Annullata dal mittente prima di ricevere risposta
}

/**
 * Indica di che tipo di richiesta stiamo parlando.
 */
enum class FriendRequestType {
    FRIENDSHIP,   // Voglio aggiungerti agli amici
    PT_COACHING   // Voglio che tu sia il mio Personal Trainer
}

/**
 * Rappresenta una richiesta inviata (es. richiesta di amicizia) salvata nel database.
 */
data class FriendRequest(
    val id: String = "",                                          // ID unico della richiesta (mittente_destinatario)
    val senderId: String = "",                                    // ID di chi invia la richiesta
    val receiverId: String = "",                                  // ID di chi riceve la richiesta
    val status: String = FriendRequestStatus.PENDING.name,        // Stato iniziale: PENDING (In attesa)
    val requestType: String = FriendRequestType.FRIENDSHIP.name,  // Tipo di richiesta iniziale: Amicizia
    val createdAt: Long = System.currentTimeMillis()              // Data e ora in cui è stata inviata (in millisecondi)
)

/**
 * Rappresenta due utenti che sono ufficialmente diventati amici.
 */
data class Friendship(
    val id: String = "",                             // ID dell'amicizia
    val users: List<String> = emptyList(),           // Lista contenente i due ID degli utenti diventati amici
    val createdAt: Long = System.currentTimeMillis() // Data in cui sono diventati amici
)

/**
 * Rappresenta il legame ufficiale tra un Personal Trainer e il suo cliente.
 */
data class PtRelationship(
    val id: String = "",                             // ID della relazione
    val ptId: String = "",                           // L'ID del Personal Trainer
    val clientId: String = "",                       // L'ID del cliente
    val createdAt: Long = System.currentTimeMillis() // Data in cui è iniziata la collaborazione
)
