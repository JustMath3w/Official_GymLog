package com.example.gymlog_finale.domain.usecase

// Use case che orchestra registrazione Firebase Auth e creazione del documento utente Firestore.

import com.example.gymlog_finale.data.repository.AuthRepository

// Classe RegisterWithEmailUseCase: unità principale definita in questo file.
class RegisterWithEmailUseCase(private val repository: AuthRepository) {

    // Operator che consente di usare l'istanza come funzione (pattern use case).
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