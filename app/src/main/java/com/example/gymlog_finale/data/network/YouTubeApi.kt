package com.example.gymlog_finale.data.network

import com.example.gymlog_finale.data.network.model.YouTubeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApi {

    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = 1,
        @Query("type") type: String = "video",
        @Query("key") apiKey: String = YOUTUBE_API_KEY
    ): YouTubeResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        // TODO: Replace with your actual YouTube Data API v3 Key
        const val YOUTUBE_API_KEY = "AIzaSyCMDIvY5DQKbI4a5lD2I1hzVmJImN-8zZ8"
    }
}