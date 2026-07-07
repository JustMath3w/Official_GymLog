package com.example.gymlog_finale.data.network

// Interfaccia Retrofit per l'API ExerciseDB (catalogo esercizi con muscoli coinvolti e GIF).

import com.example.gymlog_finale.data.network.model.ExerciseDBItem
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Interfaccia ExerciseDBApi: contratto pubblico del modulo.
interface ExerciseDBApi {

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    @GET("exercises")
    suspend fun getAllExercises(
        @Query("limit") limit: Int = 20,
        @Header("X-RapidAPI-Key") apiKey: String = RAPID_API_KEY,
        @Header("X-RapidAPI-Host") host: String = HOST
    ): List<ExerciseDBItem>

    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    @GET("exercises/name/{name}")
    suspend fun searchExercisesByName(
        @retrofit2.http.Path("name") name: String,
        @Query("limit") limit: Int = 20,
        @Header("X-RapidAPI-Key") apiKey: String = RAPID_API_KEY,
        @Header("X-RapidAPI-Host") host: String = HOST
    ): List<ExerciseDBItem>

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    @GET("exercises/exercise/{id}")
    suspend fun getExerciseById(
        @retrofit2.http.Path("id") id: String,
        @Header("X-RapidAPI-Key") apiKey: String = RAPID_API_KEY,
        @Header("X-RapidAPI-Host") host: String = HOST
    ): ExerciseDBItem

    // Companion object: raccoglie factory e costanti associate alla classe.
    companion object {
        const val BASE_URL = "https://exercisedb.p.rapidapi.com/"
        const val HOST = "exercisedb.p.rapidapi.com"

        const val RAPID_API_KEY = "d29143552bmshe39daea3840bed8p1b8c6fjsn85c5623d3051"
    }
}