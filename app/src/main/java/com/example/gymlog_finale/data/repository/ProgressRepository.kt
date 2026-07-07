package com.example.gymlog_finale.data.repository

// Repository dei log di progresso: espone flussi Firestore e gestisce upload foto su Storage.

import com.example.gymlog_finale.data.model.ProgressLog

// Interfaccia ProgressRepository: contratto pubblico del modulo.
interface ProgressRepository {

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    suspend fun getProgressLogs(userId: String): Result<List<ProgressLog>>

    // Aggiunge un elemento alla collezione o allo stato correnti.
    suspend fun addProgressLog(progressLog: ProgressLog): Result<Unit>

    // Rimuove definitivamente l'entità indicata dalla sorgente dati.
    suspend fun deleteProgressLog(logId: String): Result<Unit>
}