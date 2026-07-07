package com.example.gymlog_finale

// Activity principale dell'applicazione: inizializza Firebase, imposta il tema Compose e monta il grafo di navigazione radice.

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.gymlog_finale.navigation.AppNavGraph

// Classe MainActivity: unità principale definita in questo file.
class MainActivity : ComponentActivity() {
    // Punto di ingresso Android: inizializza la Activity e monta la UI Compose.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}