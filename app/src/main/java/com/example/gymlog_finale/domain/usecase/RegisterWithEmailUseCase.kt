package com.example.gymlog_finale.domain.usecase

import com.example.gymlog_finale.data.repository.AuthRepository

/**
 * Valida i campi di registrazione e crea l'account Firebase.
 * Restituisce l'uid del nuovo utente in caso di successo.
 */
class RegisterWithEmailUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Result<String> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email e password sono obbligatori"))
        }
        if (password.length < 6) {
            return Result.failure(Exception("La password deve essere di almeno 6 caratteri"))
        }
        return repository.register(email, password)
    }
}