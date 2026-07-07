package com.example.gymlog_finale.data.network

// Interfaccia Retrofit per l'API MyMemory di traduzione automatica IT/EN.

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// Interfaccia TranslationApi: contratto pubblico del modulo.
interface TranslationApi {

    // Esegue la traduzione del testo tra italiano e inglese.
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langPair: String = "en|it"
    ): TranslationResponse

    // Companion object: raccoglie factory e costanti associate alla classe.
    companion object {
        const val BASE_URL = "https://api.mymemory.translated.net/"
    }
}

// Data class TranslationResponse: aggregato immutabile di dati.
@Serializable
data class TranslationResponse(
    @SerialName("responseData") val responseData: TranslationData? = null,
    @SerialName("responseStatus") val responseStatus: Int = 0
)

// Data class TranslationData: aggregato immutabile di dati.
@Serializable
data class TranslationData(
    @SerialName("translatedText") val translatedText: String = "",
    @SerialName("match") val match: Double = 0.0
)