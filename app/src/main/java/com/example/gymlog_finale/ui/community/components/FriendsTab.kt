package com.example.gymlog_finale.ui.community.components

// Tab Amici della sezione Community: mostra la lista amici e le loro statistiche.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymlog_finale.data.model.FriendStats
import com.example.gymlog_finale.data.model.User

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun FriendsTab(
    friends: List<User>,
    stats: Map<String, FriendStats>,
    onRemove: (String) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyState("Nessun amico ancora")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(friends, key = { it.uid }) { user ->
            UserListItem(
                user = user,
                stats = stats[user.uid],
                trailing = {
                    Row {
                        IconButton(onClick = { onRemove(user.uid) }) {
                            Icon(Icons.Default.PersonRemove, contentDescription = "Rimuovi", tint = Color.Black)
                        }
                    }
                }
            )
        }
    }
}
