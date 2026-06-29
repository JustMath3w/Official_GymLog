package com.example.gymlog_finale.data.repository

import com.example.gymlog_finale.data.model.ProgressLog

/**
 * Definisce le operazioni di accesso ai dati per la sezione progressi.
 * Il repository espone solo i dati, mentre la logica di business resta nei UseCase.
 */
interface ProgressRepository {

    /**
     * Restituisce tutti i log di progresso dell'utente ordinati per data.
     */
    suspend fun getProgressLogs(userId: String): Result<List<ProgressLog>>

    /**
     * Salva un nuovo log di progresso con peso e foto opzionale.
     */
    suspend fun addProgressLog(progressLog: ProgressLog): Result<Unit>

    /**
     * Elimina un log di progresso esistente tramite il suo identificativo.
     */
    suspend fun deleteProgressLog(logId: String): Result<Unit>
}