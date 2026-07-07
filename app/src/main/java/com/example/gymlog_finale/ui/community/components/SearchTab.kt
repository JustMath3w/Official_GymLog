package com.example.gymlog_finale.ui.community.components

// Tab Cerca: ricerca utenti sull'intera piattaforma con filtro per soli Personal Trainer.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymlog_finale.data.model.User

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<User>,
    isSearching: Boolean,
    isCurrentUserPt: Boolean,
    onSendFriend: (String) -> Unit,
    onSendCoaching: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Cerca per username, nome o cognome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                focusedLabelColor = Color.Black,
                cursorColor = Color.Black
            )
        )
        Spacer(Modifier.height(8.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (query.isBlank()) {
            EmptyState("Inizia a digitare per cercare utenti...")
            return
        }

        if (results.isEmpty()) {
            EmptyState("Nessun utente trovato")
            return
        }

        LazyColumn {
            items(results, key = { it.uid }) { user ->
                UserListItem(
                    user = user,
                    stats = null,
                    trailing = {
                        Row {
                            IconButton(onClick = { onSendFriend(user.uid) }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Aggiungi amico")
                            }
                            if (user.isPersonalTrainer && !isCurrentUserPt) {
                                IconButton(onClick = { onSendCoaching(user.uid) }) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = "Richiedi coaching")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
