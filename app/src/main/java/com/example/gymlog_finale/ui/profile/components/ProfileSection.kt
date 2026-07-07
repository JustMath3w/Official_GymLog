package com.example.gymlog_finale.ui.profile.components

// Sezione Compose riutilizzabile per raggruppare campi omogenei nel Profilo.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun SectionHeader(testo: String, modifier: Modifier = Modifier) {
    Text(
        text = testo.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = Color.DarkGray,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun ProfileInfoRow(
    icona: ImageVector,
    iconColor: Color = Color(0xFF6C5CE7),
    iconBgColor: Color = Color(0xFFEBE5FF),
    etichetta: String,
    valore: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) modifier.fillMaxWidth().clickable { onClick() } else modifier.fillMaxWidth()
    Row(
        modifier = rowModifier.padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = etichetta,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = valore?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Black
            )
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8)),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Color(0xFFEEEEEE),
        thickness = 1.dp
    )
}