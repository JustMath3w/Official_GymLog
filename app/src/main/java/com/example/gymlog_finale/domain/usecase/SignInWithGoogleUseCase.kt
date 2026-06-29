package com.example.gymlog_finale.domain.usecase

import com.example.gymlog_finale.data.repository.AuthRepository

/**
 * Delega il sign-in Google al repository passando il token già ottenuto lato UI.
 */
class SignInWithGoogleUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(idToken: String): Result<String> {
        if (idToken.isBlank()) {
            return Result.failure(Exception("Token Google non valido"))
        }
        return repository.signInWithGoogle(idToken)
    }
}