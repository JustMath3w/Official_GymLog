package com.example.gymlog_finale.domain.usecase

import com.example.gymlog_finale.data.repository.AuthRepository

/**
 * Valida i campi e delega il login al repository.
 * Restituisce Result.failure con messaggio leggibile se i campi sono vuoti.
 */
class LoginWithEmailUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email e password sono obbligatori"))
        }
        return repository.login(email, password)
    }
}