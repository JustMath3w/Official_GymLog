package com.example.gymlog_finale.domain.usecase

// Use case per il login email/password con validazione input minima.

import com.example.gymlog_finale.data.repository.AuthRepository

// Classe LoginWithEmailUseCase: unità principale definita in questo file.
class LoginWithEmailUseCase(private val repository: AuthRepository) {

    // Operator che consente di usare l'istanza come funzione (pattern use case).
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email e password sono obbligatori"))
        }
        return repository.login(email, password)
    }
}