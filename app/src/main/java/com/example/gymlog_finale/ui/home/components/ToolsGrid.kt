package com.example.gymlog_finale.ui.home.components

// Griglia Compose di scorciatoie verso le sezioni principali dell'applicazione.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Data class ToolItem: aggregato immutabile di dati.
private data class ToolItem(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun ToolsGrid(
    onAllenamento: () -> Unit,
    onDieta: () -> Unit,
    onCommunity: () -> Unit,
    onProgressi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        ToolItem("Allenamento", Icons.Rounded.FitnessCenter, Color(0xFFEBE5FF), Color(0xFF6C5CE7), onAllenamento),
        ToolItem("Dieta", Icons.Rounded.Restaurant, Color(0xFFE8F8F5), Color(0xFF16A085), onDieta),
        ToolItem("Community", Icons.Rounded.Group, Color(0xFFFFF4E6), Color(0xFFE67E22), onCommunity),
        ToolItem("Progressi", Icons.Rounded.ShowChart, Color(0xFFFCEBE6), Color(0xFFD35400), onProgressi)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tools.chunked(2).forEach { riga ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                riga.forEach { tool ->
                    ToolCard(tool = tool, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun ToolCard(tool: ToolItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = tool.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = tool.containerColor, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    tint = tool.iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tool.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}