package com.example.gymlog_finale.data.repository

import android.util.Log
import com.example.gymlog_finale.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Repository che si occupa di richiamare l'API di traduzione, esporre il risultato come stringa
 * e ottimizzare le performance tramite cache e traduzioni in batch.
 */
class TranslationRepository {

    private val api = NetworkModule.translationApi
    private val cache = mutableMapOf<String, String>()
    private val tag = "TranslationRepository"

    private val localFitnessTranslations = mapOf(
        "panca" to "bench",
        "petto" to "chest",
        "spalle" to "shoulder",
        "schiena" to "back",
        "gambe" to "leg",
        "cosce" to "thigh",
        "polpacci" to "calf",
        "braccia" to "arm",
        "bicipiti" to "bicep",
        "tricipiti" to "tricep",
        "addome" to "crunch",
        "addominali" to "crunch",
        "fianchi" to "waist",
        "glutei" to "glute",
        "dorsali" to "lat",
        "trapezi" to "trap",
        "trazioni" to "pull up",
        "flessioni" to "push up",
        "piegamenti" to "push up",
        "affondi" to "lunge",
        "alzate" to "raise",
        "distensioni" to "press",
        "tirate" to "row",
        "iperestensioni" to "hyperextension",
        "avambracci" to "forearm",
        "collo" to "neck"
    )

    /**
     * Esegue la traduzione locale di termini noti in italiano.
     */
    fun getLocalTranslation(query: String): String {
        val lowercaseQuery = query.lowercase().trim()
        localFitnessTranslations[lowercaseQuery]?.let { return it }
        
        val words = lowercaseQuery.split("\\s+".toRegex())
        val translatedWords = words.map { word ->
            localFitnessTranslations[word] ?: word
        }
        return translatedWords.joinToString(" ")
    }

    /**
     * Pulisce una query tradotta in inglese per adattarla al database ExerciseDB (es. rimuove plurali e parentesi).
     */
    fun cleanEnglishQuery(translated: String): String {
        var cleaned = translated
            .replace(Regex("\\(.*?\\)"), "") // Rimuove parentesi tipo "(exercise)"
            .replace(Regex("[^a-zA-Z0-9\\s]"), "") // Rimuove punteggiatura
            .trim()
            .lowercase()
        
        // Rimuove la 's' plurale finale se la parola non termina in 'ss' (press) o 'us'
        if (cleaned.endsWith("s") && !cleaned.endsWith("ss") && !cleaned.endsWith("us") && cleaned.length > 3) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }
        return cleaned
    }

    /**
     * Traduce il testo passato in input dall'inglese all'italiano (o viceversa in base a [langPair]).
     */
    suspend fun translateText(text: String, langPair: String = "en|it"): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        val cacheKey = "$langPair:$text"
        cache[cacheKey]?.let { return@withContext it }

        try {
            val response = api.translate(text, langPair)
            val translated = response.responseData?.translatedText?.takeIf { it.isNotBlank() } ?: text
            cache[cacheKey] = translated
            translated
        } catch (e: Exception) {
            Log.e(tag, "Errore traduzione di '$text' con langPair '$langPair'", e)
            text
        }
    }

    /**
     * Traduce una lista di testi in una sola chiamata di rete unendoli con un ritorno a capo '\n'.
     * In caso di disallineamento nel parsing, ripiega sulla traduzione parallela dei soli testi mancanti in cache.
     */
    suspend fun translateTexts(texts: List<String>, langPair: String = "en|it"): List<String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()

        val results = MutableList(texts.size) { "" }
        val uncachedIndices = mutableListOf<Int>()
        val uncachedTexts = mutableListOf<String>()

        // 1. Controlla la cache
        texts.forEachIndexed { index, text ->
            val cacheKey = "$langPair:$text"
            val cachedValue = cache[cacheKey]
            if (cachedValue != null) {
                results[index] = cachedValue
            } else {
                uncachedIndices.add(index)
                uncachedTexts.add(text)
            }
        }

        if (uncachedTexts.isEmpty()) {
            return@withContext results
        }

        // 2. Tenta la traduzione batch
        try {
            val joinedText = uncachedTexts.joinToString("\n")
            val response = api.translate(joinedText, langPair)
            val translatedJoined = response.responseData?.translatedText
            
            if (!translatedJoined.isNullOrBlank()) {
                val translatedLines = translatedJoined.split("\n").map { it.trim() }
                if (translatedLines.size == uncachedTexts.size) {
                    translatedLines.forEachIndexed { i, translatedLine ->
                        val origText = uncachedTexts[i]
                        val cacheKey = "$langPair:$origText"
                        cache[cacheKey] = translatedLine
                        
                        val origIndex = uncachedIndices[i]
                        results[origIndex] = translatedLine
                    }
                    return@withContext results
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore nella traduzione batch, procedo individualmente", e)
        }

        // 3. Fallback: traduzione individuale in parallelo
        coroutineScope {
            val deferreds = uncachedTexts.map { text ->
                async {
                    translateText(text, langPair)
                }
            }
            val translatedUncached = deferreds.awaitAll()
            translatedUncached.forEachIndexed { i, translatedText ->
                val origIndex = uncachedIndices[i]
                results[origIndex] = translatedText
            }
        }

        results
    }

    /**
     * Mappatura statica istantanea delle parti del corpo.
     */
    fun translateBodyPart(bodyPart: String?): String {
        if (bodyPart.isNullOrBlank()) return ""
        return when (bodyPart.lowercase().trim()) {
            "back" -> "Schiena"
            "cardio" -> "Cardio"
            "chest" -> "Petto"
            "lower arms" -> "Avambracci"
            "lower legs" -> "Polpacci"
            "neck" -> "Collo"
            "shoulders" -> "Spalle"
            "upper arms" -> "Braccia"
            "upper legs" -> "Cosce"
            "waist" -> "Addome"
            else -> bodyPart.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Mappatura statica istantanea dei muscoli target.
     */
    fun translateTarget(target: String?): String {
        if (target.isNullOrBlank()) return ""
        return when (target.lowercase().trim()) {
            "abductors" -> "Abduttori"
            "abs" -> "Addominali"
            "adductors" -> "Adduttori"
            "biceps" -> "Bicipiti"
            "calves" -> "Polpacci"
            "cardiovascular system" -> "Cardio"
            "delts" -> "Deltoidi"
            "forearms" -> "Avambracci"
            "glutes" -> "Glutei"
            "hamstrings" -> "Femorali"
            "lats" -> "Dorsali"
            "levator scapulae" -> "Elevatore della scapola"
            "pectorals" -> "Pettorali"
            "quads" -> "Quadricipiti"
            "serratus anterior" -> "Gran dentato"
            "spine" -> "Lombari"
            "traps" -> "Trapezi"
            "triceps" -> "Tricipiti"
            "upper back" -> "Dorsali Alti"
            else -> target.replaceFirstChar { it.uppercase() }
        }
    }
}