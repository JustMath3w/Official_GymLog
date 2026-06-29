package com.example.gymlog_finale.ui.community.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog_finale.data.model.FriendStats
import com.example.gymlog_finale.data.model.User

@Composable
fun UserListItem(
    user: User,
    stats: FriendStats?,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF97316).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.username.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316),
                        fontSize = 24.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            user.username.ifBlank { "${user.nome} ${user.cognome}" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        if (user.isPersonalTrainer) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF97316).copy(alpha = 0.1f),
                            ) {
                                Text(
                                    "PT", 
                                    color = Color(0xFFF97316), 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    val sub = subtitle ?: "${user.nome} ${user.cognome}".trim()
                    if (sub.isNotBlank()) {
                        Text(sub, color = Color.Gray, fontSize = 15.sp)
                    }
                }
                trailing()
            }

            if (stats != null && stats.hasAnyData()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stats.workoutStreakDays > 0) {
                        StatChip(Icons.Default.LocalFireDepartment, "${stats.workoutStreakDays} gg tot")
                    }
                    if (stats.dietStreakDays > 0) {
                        StatChip(Icons.Default.Restaurant, "${stats.dietStreakDays} gg dieta")
                    }
                    if (stats.totalTrainingDays > 0) {
                        StatChip(Icons.Default.CalendarMonth, "${stats.totalTrainingDays} gg tot")
                    }
                    stats.favoriteExercise?.let {
                        StatChip(Icons.Default.Star, it)
                    }
                    stats.personalTrainerName?.let {
                        StatChip(Icons.Default.FitnessCenter, "PT: $it")
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF97316))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
    }
}

fun FriendStats.hasAnyData(): Boolean =
    workoutStreakDays > 0 ||
            dietStreakDays > 0 ||
            totalTrainingDays > 0 ||
            !favoriteExercise.isNullOrBlank() ||
            !personalTrainerName.isNullOrBlank()

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}
