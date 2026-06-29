package com.example.gymlog_finale.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaccia Retrofit per il servizio MyMemory Translation API.
 * Servizio gratuito che non richiede API key per un uso moderato.
 */
interface TranslationApi {

    /**
     * Effettua la chiamata GET all'endpoint /get di MyMemory passando il testo
     * da tradurre e la coppia di lingue (es. "en|it").
     */
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langPair: String = "en|it"
    ): TranslationResponse

    companion object {
        const val BASE_URL = "https://api.mymemory.translated.net/"
    }
}

/**
 * Modello che rappresenta la risposta JSON dell'API MyMemory.
 * Contiene il testo tradotto all'interno del campo responseData.
 */
@Serializable
data class TranslationResponse(
    @SerialName("responseData") val responseData: TranslationData? = null,
    @SerialName("responseStatus") val responseStatus: Int = 0
)

/**
 * Sotto-oggetto che contiene il testo effettivamente tradotto.
 */
@Serializable
data class TranslationData(
    @SerialName("translatedText") val translatedText: String = "",
    @SerialName("match") val match: Double = 0.0
)