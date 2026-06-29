package com.example.gymlog_finale.util

/**
 * Costanti centralizzate per la gestione dati
 */
object Constants {
    // Firestore Collections
    const val USERS_COLLECTION = "users"
    const val USER_PROFILES_COLLECTION = "user_profiles"
    const val WORKOUTS_COLLECTION = "workouts"
    const val TRANSLATIONS_COLLECTION = "translations"
    const val FRIEND_REQUESTS_COLLECTION = "friend_requests"
    const val FRIENDS_COLLECTION = "friends"
    const val WORKOUT_LOGS_COLLECTION = "workout_logs"

    // Collezioni community
    const val USERNAMES_COLLECTION = "usernames"
    const val BLOCKS_COLLECTION = "blocks"
    const val PT_RELATIONSHIPS_COLLECTION = "pt_relationships"

    // Valori di default
    const val DEFAULT_AGE = 25
    const val DEFAULT_HEIGHT = 180
    const val DEFAULT_GOAL = "Definizione"
    const val DEFAULT_LEVEL = "Intermedio"

    // Goals disponibili
    val AVAILABLE_GOALS = listOf(
        "Definizione",
        "Massa muscolare",
        "Resistenza",
        "Forza",
        "Flessibilità"
    )

    // Livelli disponibili
    val AVAILABLE_LEVELS = listOf(
        "Principiante",
        "Intermedio",
        "Avanzato"
    )

    // Error messages
    const val ERROR_USER_NOT_FOUND = "Utente non trovato"
    const val ERROR_NETWORK = "Errore di connessione"
    const val ERROR_UNKNOWN = "Errore sconosciuto"
}