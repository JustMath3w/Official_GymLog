package com.example.gymlog_finale.data.model

/**
 * Salva i progressi fisici (misurazioni) dell'utente nel tempo.
 */
data class ProgressLog(
    val id: String = "",                               // ID unico di questa misurazione
    val userId: String = "",                           // ID dell'utente che ha fatto la misurazione
    val timestamp: Long = System.currentTimeMillis(),  // La data e ora esatta della misurazione
    val weightKg: Double = 0.0,                        // Il peso corporeo registrato in Kg
    val photoUrl: String = ""                          // L'eventuale link alla foto dei progressi (fisico)
)