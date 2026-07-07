package com.example.gymlog_finale.data.model

// Modello dati per una scheda di allenamento e i suoi esercizi (con serie e ripetizioni).

import com.google.firebase.firestore.PropertyName

// Data class Workout: aggregato immutabile di dati.
data class Workout(
    val id: String = "",
    val userId: String = "",
    val assignedTo: String? = null,
    val name: String = "",
    val exercises: List<Exercise> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),

    val senderId: String? = null,
    val senderName: String? = null,
    val senderIsPersonalTrainer: Boolean = false,
    val splitType: String = "Rest",
    val receiverName: String? = null,

    @get:PropertyName("isReceived")
    @set:PropertyName("isReceived")
    @PropertyName("isReceived")
    var isReceived: Boolean = false
)

// Data class Exercise: aggregato immutabile di dati.
data class Exercise(
    var id: String = "",
    var name: String = "",
    var sets: String = "",
    var reps: String = "",
    var weight: String = "",

    @get:PropertyName("gifUrl")
    @set:PropertyName("gifUrl")
    @PropertyName("gifUrl")
    var gifUrl: String? = null,

    var instructions: List<String> = emptyList(),
    var bodyPart: String? = null,
    var target: String? = null,
    var youtubeVideoId: String? = null
)

// Data class SplitPlan: aggregato immutabile di dati.
data class SplitPlan(
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val split: Map<Int, String> = emptyMap(),
    val overrides: Map<String, String> = emptyMap()
)