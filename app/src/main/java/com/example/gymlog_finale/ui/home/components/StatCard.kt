package com.example.gymlog_finale.ui.home.components

// Card Compose che mostra una singola statistica riassuntiva nella Home.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun StatCard(
    titolo: String,
    valore: String,
    icona: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valoreObiettivo: String? = null,
    sottotitolo: String? = null,
    progress: Float? = null
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icona,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                titolo,
                style = MaterialTheme.typography.labelLarge,
                color = Color.DarkGray
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text(
                    valore,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (valoreObiettivo != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        valoreObiettivo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.Black,
                    trackColor = Color.Black.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
            }
            if (sottotitolo != null) {
                Text(
                    sottotitolo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}