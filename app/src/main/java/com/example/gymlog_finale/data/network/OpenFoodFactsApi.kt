package com.example.gymlog_finale.data.network

// Interfaccia Retrofit per OpenFoodFacts: ricerca alimenti per nome e per codice a barre.

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data class OpenFoodFactsResponse: aggregato immutabile di dati.
data class OpenFoodFactsResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("products") val products: List<Product>
)

// Data class ProductResponseByBarcode: aggregato immutabile di dati.
data class ProductResponseByBarcode(
    @SerializedName("status") val status: Int,
    @SerializedName("product") val product: Product?
)

// Data class Product: aggregato immutabile di dati.
data class Product(
    @SerializedName("id") val id: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("product_name_it") val productNameIt: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("nutriments") val nutriments: Nutriments?
) {

    val displayName: String
        get() = productNameIt?.takeIf { it.isNotBlank() } ?: productName ?: "Prodotto sconosciuto"
}

// Data class Nutriments: aggregato immutabile di dati.
data class Nutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("fat_100g") val fat100g: Double?
)

// Interfaccia OpenFoodFactsApi: contratto pubblico del modulo.
interface OpenFoodFactsApi {
    // Esegue una ricerca sull'insieme dati indicato in base ai criteri forniti.
    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): OpenFoodFactsResponse

    // Recupera l'entità o il valore richiesto dalla sorgente dati.
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @retrofit2.http.Path("barcode") barcode: String
    ): ProductResponseByBarcode
}

// Singleton OpenFoodFactsClient: raccoglie funzioni/costanti condivise.
object OpenFoodFactsClient {
    private const val BASE_URL = "https://it.openfoodfacts.org/"

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
