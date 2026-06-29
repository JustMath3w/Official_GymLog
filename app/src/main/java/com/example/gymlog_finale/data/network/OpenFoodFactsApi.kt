package com.example.gymlog_finale.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Modelli Dati ---

data class OpenFoodFactsResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("products") val products: List<Product>
)

data class ProductResponseByBarcode(
    @SerializedName("status") val status: Int,
    @SerializedName("product") val product: Product?
)

data class Product(
    @SerializedName("id") val id: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("product_name_it") val productNameIt: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("nutriments") val nutriments: Nutriments?
) {
    // Ottiene il miglior nome disponibile (preferendo quello in italiano)
    val displayName: String
        get() = productNameIt?.takeIf { it.isNotBlank() } ?: productName ?: "Prodotto sconosciuto"
}

data class Nutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("fat_100g") val fat100g: Double?
)

// --- Retrofit Interface ---

interface OpenFoodFactsApi {
    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): OpenFoodFactsResponse

    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @retrofit2.http.Path("barcode") barcode: String
    ): ProductResponseByBarcode
}

// --- API Client ---

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
