package com.example.gymlog_finale.ui.home

// Schermata Home: riepilogo del giorno (scheda attiva, peso, calorie) e accessi rapidi.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.gymlog_finale.ui.home.components.StatCard
import com.example.gymlog_finale.ui.home.components.ToolsGrid
import com.example.gymlog_finale.ui.home.components.WorkoutTodayCard
import com.example.gymlog_finale.ui.workout.WorkoutViewModel

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWorkout: () -> Unit,
    onNavigateToDiet: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nomeUtente = state.user?.nome?.takeIf { it.isNotBlank() } ?: "Atleta"
    val activeWorkout by WorkoutViewModel.globalActiveWorkout.collectAsStateWithLifecycle(null)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    Scaffold(
        containerColor = Color(0xFFEBEBEB)
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bentornato/a, $nomeUtente",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nomeUtente.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            WorkoutTodayCard(
                nomeWorkout = state.workoutOdierno,
                hasActiveWorkout = activeWorkout != null,
                onAvviaAllenamento = {
                    if (activeWorkout != null) {
                        WorkoutViewModel.setWorkoutMinimized(false)
                    }
                    onNavigateToWorkout()
                }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                StatCard(
                    titolo = "Peso",
                    valore = state.pesoAttuale?.let { "$it kg" } ?: "—",
                    icona = Icons.Default.MonitorWeight,
                    onClick = onNavigateToProgress,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                StatCard(
                    titolo = "Calorie oggi",
                    valore = "${state.kcalAssunte}",
                    valoreObiettivo = "/ ${state.kcalObiettivo}",
                    sottotitolo = null,
                    progress = if (state.kcalObiettivo > 0) (state.kcalAssunte.toFloat() / state.kcalObiettivo.toFloat()).coerceIn(0f, 1f) else 0f,
                    icona = Icons.Default.Restaurant,
                    onClick = onNavigateToDiet,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "I TUOI PROGRESSI DI OGGI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.workoutStreakGiorni}🔥",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Streak allenamenti",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.dietStreakGiorni}🥗",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Streak dieta",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            ToolsGrid(
                onAllenamento = onNavigateToWorkout,
                onDieta = onNavigateToDiet,
                onCommunity = onNavigateToCommunity,
                onProgressi = onNavigateToProgress
            )

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}