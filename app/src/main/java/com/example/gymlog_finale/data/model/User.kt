package com.example.gymlog_finale.data.model

// Modello dati dell'utente: dati anagrafici, dati fisici, flag Personal Trainer e riferimento al PT associato.

data class User(
    val uid: String = "",
    val nome: String = "",
    val cognome: String = "",
    val username: String = "",
    val email: String = "",
    val annoDiNascita: Int = 0,
    val altezza: Int = 0,
    val peso: Double = 0.0,
    val obiettivo: String = "",
    val isPersonalTrainer: Boolean = false,
    val hasPersonalTrainer: String? = null,
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)