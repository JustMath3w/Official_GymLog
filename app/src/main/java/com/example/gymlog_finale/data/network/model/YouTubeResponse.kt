package com.example.gymlog_finale.data.network.model

// DTO che modella la risposta JSON dell'API YouTube per la ricerca video.

import kotlinx.serialization.Serializable

// Data class YouTubeResponse: aggregato immutabile di dati.
@Serializable
data class YouTubeResponse(
    val items: List<YouTubeVideoItem> = emptyList()
)

// Data class YouTubeVideoItem: aggregato immutabile di dati.
@Serializable
data class YouTubeVideoItem(
    val id: VideoId,
    val snippet: VideoSnippet
)

// Data class VideoId: aggregato immutabile di dati.
@Serializable
data class VideoId(
    val videoId: String = ""
)

// Data class VideoSnippet: aggregato immutabile di dati.
@Serializable
data class VideoSnippet(
    val title: String = "",
    val description: String = "",
    val thumbnails: Thumbnails? = null
)

// Data class Thumbnails: aggregato immutabile di dati.
@Serializable
data class Thumbnails(
    val default: ThumbnailDetails? = null,
    val medium: ThumbnailDetails? = null,
    val high: ThumbnailDetails? = null
)

// Data class ThumbnailDetails: aggregato immutabile di dati.
@Serializable
data class ThumbnailDetails(
    val url: String = ""
)
