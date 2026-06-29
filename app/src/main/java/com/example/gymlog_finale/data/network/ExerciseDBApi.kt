package com.example.gymlog_finale.data.network

import com.example.gymlog_finale.data.network.model.ExerciseDBItem
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ExerciseDBApi {

    @GET("exercises")
    suspend fun getAllExercises(
        @Query("limit") limit: Int = 20,
        @Header("X-RapidAPI-Key") apiKey: String = RAPID_API_KEY,
        @Header("X-RapidAPI-Host") host: String = HOST
    ): List<ExerciseDBItem>

    @GET("exercises/name/{name}")
    suspend fun searchExercisesByName(
        @retrofit2.http.Path("name") name: String,
        @Query("limit") limit: Int = 20,
        @Header("X-RapidAPI-Key") apiKey: String = RAPID_API_KEY,
        @Header("X-RapidAPI-Host") host: String = HOST
    ): List<ExerciseDBItem>

    @GET("exercises/exercise/{id}")
    suspend fun getExerciseById(
        @retrofit2.http.Path("id") id: String,
        @Header("X-RapidAPI-Key") apiKey: String = RAPID_API_KEY,
        @Header("X-RapidAPI-Host") host: String = HOST
    ): ExerciseDBItem

    companion object {
        const val BASE_URL = "https://exercisedb.p.rapidapi.com/"
        const val HOST = "exercisedb.p.rapidapi.com"
        // TODO: Replace with your actual RapidAPI Key
        const val RAPID_API_KEY = "d29143552bmshe39daea3840bed8p1b8c6fjsn85c5623d3051"
    }
}