package com.example.gymlog_finale.ui.workout

// Schermata riservata ai Personal Trainer per creare o modificare la scheda di un cliente.

import com.example.gymlog_finale.data.model.Workout
import com.example.gymlog_finale.data.model.Exercise
import com.example.gymlog_finale.data.model.SplitPlan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun PtCreateWorkoutScreen(
    clientUid: String,
    clientName: String,
    onDone: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSaveSuccess()
            onDone()
        }
    }

    WorkoutDialog(
        workout = null,
        viewModel = viewModel,
        onDismiss = onDone,
        onSave = { name, exercises, _, splitType ->
            viewModel.saveWorkoutForClient(clientUid, name, exercises, splitType)
        }
    )
}