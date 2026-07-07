package com.example.gymlog_finale.domain.usecase

// Use case che restituisce il flusso ordinato dei log di progresso per l'utente autenticato.

import com.example.gymlog_finale.data.model.ProgressLog
import com.example.gymlog_finale.data.repository.ProgressRepository

// Classe GetProgressLogsUseCase: unità principale definita in questo file.
class GetProgressLogsUseCase(
    private val repository: ProgressRepository
) {

    // Operator che consente di usare l'istanza come funzione (pattern use case).
    suspend operator fun invoke(userId: String): Result<List<ProgressLog>> {
        if (userId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("L'userId non può essere vuoto.")
            )
        }

        return repository.getProgressLogs(userId)
    }
}