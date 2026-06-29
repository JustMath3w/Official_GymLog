package com.example.gymlog_finale.domain.usecase

import com.example.gymlog_finale.data.model.ProgressLog
import com.example.gymlog_finale.data.repository.ProgressRepository

/**
 * Recupera tutti i log di progresso di un utente.
 * La logica di accesso ai dati resta nel repository.
 */
class GetProgressLogsUseCase(
    private val repository: ProgressRepository
) {

    /**
     * Esegue il recupero dei log validando l'identificativo utente.
     */
    suspend operator fun invoke(userId: String): Result<List<ProgressLog>> {
        if (userId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("L'userId non può essere vuoto.")
            )
        }

        return repository.getProgressLogs(userId)
    }
}