package com.example.gymlog_finale.domain.usecase

// Use case per il login Google via Credential Manager e verifica del profilo su Firestore.

import com.example.gymlog_finale.data.repository.AuthRepository

// Classe SignInWithGoogleUseCase: unità principale definita in questo file.
class SignInWithGoogleUseCase(private val repository: AuthRepository) {

    // Operator che consente di usare l'istanza come funzione (pattern use case).
    suspend operator fun invoke(idToken: String): Result<String> {
        if (idToken.isBlank()) {
            return Result.failure(Exception("Token Google non valido"))
        }
        return repository.signInWithGoogle(idToken)
    }
}