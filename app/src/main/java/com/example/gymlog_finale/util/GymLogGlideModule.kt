package com.example.gymlog_finale.util

// Modulo Glide personalizzato per configurare il caricamento delle immagini.

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream

// Classe GymLogGlideModule: unità principale definita in questo file.
@GlideModule
class GymLogGlideModule : AppGlideModule() {
    // Registra un nuovo utente sulla piattaforma e crea il relativo documento profilo.
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-key", "d29143552bmshe39daea3840bed8p1b8c6fjsn85c5623d3051")
                    .addHeader("x-rapidapi-host", "exercisedb.p.rapidapi.com")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()

        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }
}
