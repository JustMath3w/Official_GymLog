package com.example.gymlog_finale.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Rappresenta una singola scheda di allenamento.
 */
data class Workout(
    val id: String = "",                             // ID della scheda
    val userId: String = "",                         // Chi è il proprietario o chi ha creato la scheda
    val assignedTo: String? = null,                  // Se la scheda è stata creata da un PT per un cliente, qui c'è l'ID del cliente
    val name: String = "",                           // Nome della scheda (es. "Spinta Petto", "Gambe Pesanti")
    val exercises: List<Exercise> = emptyList(),     // Lista degli esercizi che compongono la scheda
    val createdAt: Long = System.currentTimeMillis(),// Quando è stata creata
    
    // Dati aggiuntivi usati per la condivisione delle schede tra amici
    val senderId: String? = null,                    // Chi mi ha inviato questa scheda
    val senderName: String? = null,                  // Il nome di chi me l'ha inviata
    val senderIsPersonalTrainer: Boolean = false,    // Era un PT a inviarmela?
    val splitType: String = "Rest",                  // Giorno della settimana o split (es. Push, Pull, Legs)
    val receiverName: String? = null,                // Nome di chi riceve la scheda

    // Queste annotazioni ("@PropertyName") servono ad assicurarsi che Firebase legga correttamente 
    // i campi booleani (che iniziano per "is"). Indica se la scheda è stata ricevuta da un altro utente.
    @get:PropertyName("isReceived")
    @set:PropertyName("isReceived")
    @PropertyName("isReceived")
    var isReceived: Boolean = false
)

/**
 * Rappresenta un singolo esercizio all'interno di una scheda di allenamento.
 */
data class Exercise(
    var id: String = "",                    // L'ID dell'esercizio nel database globale
    var name: String = "",                  // Il nome (es. "Panca piana")
    var sets: String = "",                  // Numero di serie (es. "4")
    var reps: String = "",                  // Numero di ripetizioni (es. "10-12")
    var weight: String = "",                // Peso sollevato (es. "80 kg")

    // Anche qui servono le annotazioni per mappare correttamente i nomi con Firebase
    @get:PropertyName("gifUrl")
    @set:PropertyName("gifUrl")
    @PropertyName("gifUrl")
    var gifUrl: String? = null,             // Link all'animazione GIF che fa vedere come si esegue

    var instructions: List<String> = emptyList(), // Passaggi testuali per l'esecuzione
    var bodyPart: String? = null,                 // Parte del corpo (es. "Chest")
    var target: String? = null,                   // Muscolo target specifico (es. "Pectoralis major")
    var youtubeVideoId: String? = null            // ID di un eventuale video YouTube che spiega l'esercizio
)

/**
 * Rappresenta la pianificazione settimanale (Split) degli allenamenti.
 */
data class SplitPlan(
    val startDate: Long = 0L,                     // Data di inizio validità del piano (Lunedì)
    val endDate: Long = 0L,                       // Data di fine (Domenica)
    val split: Map<Int, String> = emptyMap(),     // Una mappa (Giorno(1-7) -> NomeWorkout)
    val overrides: Map<String, String> = emptyMap() // Eventuali modifiche fatte a giorni specifici
)