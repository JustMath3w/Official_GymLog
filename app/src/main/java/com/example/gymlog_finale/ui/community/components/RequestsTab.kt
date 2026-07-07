package com.example.gymlog_finale.ui.community.components

// Tab Richieste: gestione delle richieste di amicizia e di coaching ricevute.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog_finale.ui.community.IncomingRequestUi
import com.example.gymlog_finale.ui.community.OutgoingRequestUi

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun RequestsTab(
    incoming: List<IncomingRequestUi>,
    outgoing: List<OutgoingRequestUi>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    var isIncomingSelected by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isIncomingSelected) Color(0xFF1C1C1E) else Color(0xFFF0F0F0))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isIncomingSelected = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "In entrata (${incoming.size})",
                    color = if (isIncomingSelected) Color.White else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (!isIncomingSelected) Color(0xFF1C1C1E) else Color(0xFFF0F0F0))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isIncomingSelected = false },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "In uscita (${outgoing.size})",
                    color = if (!isIncomingSelected) Color.White else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isIncomingSelected) {
                if (incoming.isEmpty()) {
                    item { EmptyState("Nessuna richiesta ricevuta") }
                } else {
                    items(incoming, key = { it.request.id }) { ui ->
                        UserListItem(
                            user = ui.sender,
                            stats = null,
                            subtitle = if (ui.request.requestType == "PT_COACHING") "Richiesta coaching" else "Richiesta amicizia",
                            trailing = {
                                Row {
                                    IconButton(
                                        onClick = { onAccept(ui.request.id) },
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accetta", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { onReject(ui.request.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFEBEE))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Rifiuta", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                if (outgoing.isEmpty()) {
                    item { EmptyState("Nessuna richiesta inviata") }
                } else {
                    items(outgoing, key = { it.request.id }) { ui ->
                        UserListItem(
                            user = ui.receiver,
                            stats = null,
                            subtitle = if (ui.request.requestType == "PT_COACHING") "Richiesta coaching" else "Richiesta amicizia",
                            trailing = {
                                IconButton(
                                    onClick = { onCancel(ui.request.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFEBEE))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Annulla", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
