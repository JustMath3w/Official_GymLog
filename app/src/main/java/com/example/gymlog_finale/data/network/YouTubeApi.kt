package com.example.gymlog_finale.data.network

// Interfaccia Retrofit per YouTube Data API v3, usata per cercare video dimostrativi degli esercizi.

import com.example.gymlog_finale.data.network.model.YouTubeResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Interfaccia YouTubeApi: contratto pubblico del modulo.
interface YouTubeApi {

    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = 1,
        @Query("type") type: String = "video",
        @Query("key") apiKey: String = YOUTUBE_API_KEY
    ): YouTubeResponse

    // Companion object: raccoglie factory e costanti associate alla classe.
    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

        const val YOUTUBE_API_KEY = "AIzaSyCMDIvY5DQKbI4a5lD2I1hzVmJImN-8zZ8"
    }
}