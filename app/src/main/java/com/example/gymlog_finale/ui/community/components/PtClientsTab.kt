package com.example.gymlog_finale.ui.community.components

// Tab riservato ai Personal Trainer, con la lista dei propri clienti assistiti.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlog_finale.data.model.FriendStats
import com.example.gymlog_finale.data.model.User

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun PtClientsTab(
    clients: List<User>,
    stats: Map<String, FriendStats>,
    onRemove: (String) -> Unit,
    onCreateWorkout: (clientUid: String, clientName: String) -> Unit
) {
    if (clients.isEmpty()) {
        EmptyState("Nessun cliente assegnato")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(clients, key = { it.uid }) { user ->
            UserListItem(
                user = user,
                stats = stats[user.uid],
                trailing = {
                    Row {
                        IconButton(onClick = {
                            val displayName = user.username.ifBlank { "${user.nome} ${user.cognome}".trim() }
                            onCreateWorkout(user.uid, displayName)
                        }) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = "Crea scheda")
                        }
                        IconButton(onClick = { onRemove(user.uid) }) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Rimuovi cliente")
                        }
                    }
                }
            )
        }
    }
}
