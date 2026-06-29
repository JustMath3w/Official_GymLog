package com.example.gymlog_finale.data.model

/**
 * Modello principale che contiene tutti i dati del profilo di un utente iscritto all'app.
 */
data class User(
    val uid: String = "",                            // L'ID univoco di Firebase (Codice fiscale utente)
    val nome: String = "",                           // Nome
    val cognome: String = "",                        // Cognome
    val username: String = "",                       // Nome utente visibile nella community
    val email: String = "",                          // Indirizzo Email
    val annoDiNascita: Int = 0,                      // Anno in cui è nato
    val altezza: Int = 0,                            // Altezza in cm
    val peso: Double = 0.0,                          // Peso iniziale in Kg
    val obiettivo: String = "",                      // Il suo obiettivo (es. Ipertrofia, Dimagrimento)
    val isPersonalTrainer: Boolean = false,          // True se questo utente è un Personal Trainer, False se è un utente base
    val hasPersonalTrainer: String? = null,          // Contiene l'ID del suo PT, se ne ha scelto uno. Altrimenti è null.
    val photoUrl: String = "",                       // Immagine profilo scaricata da Google o inserita
    val createdAt: Long = System.currentTimeMillis() // Data di iscrizione all'app
)