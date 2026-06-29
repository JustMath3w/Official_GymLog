package com.example.gymlog_finale.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeResponse(
    val items: List<YouTubeVideoItem> = emptyList()
)

@Serializable
data class YouTubeVideoItem(
    val id: VideoId,
    val snippet: VideoSnippet
)

@Serializable
data class VideoId(
    val videoId: String = ""
)

@Serializable
data class VideoSnippet(
    val title: String = "",
    val description: String = "",
    val thumbnails: Thumbnails? = null
)

@Serializable
data class Thumbnails(
    val default: ThumbnailDetails? = null,
    val medium: ThumbnailDetails? = null,
    val high: ThumbnailDetails? = null
)

@Serializable
data class ThumbnailDetails(
    val url: String = ""
)
