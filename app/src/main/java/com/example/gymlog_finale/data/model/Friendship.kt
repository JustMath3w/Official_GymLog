package com.example.gymlog_finale.data.model

// Modello dati per amicizie e richieste (mittente, destinatario, stato, tipo FRIENDSHIP o PT_COACHING).

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}

// Enum FriendRequestType: insieme finito di valori usati nell'app.
enum class FriendRequestType {
    FRIENDSHIP,
    PT_COACHING
}

// Data class FriendRequest: aggregato immutabile di dati.
data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: String = FriendRequestStatus.PENDING.name,
    val requestType: String = FriendRequestType.FRIENDSHIP.name,
    val createdAt: Long = System.currentTimeMillis()
)

// Data class Friendship: aggregato immutabile di dati.
data class Friendship(
    val id: String = "",
    val users: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// Data class PtRelationship: aggregato immutabile di dati.
data class PtRelationship(
    val id: String = "",
    val ptId: String = "",
    val clientId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
