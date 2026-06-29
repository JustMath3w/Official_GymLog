package com.example.gymlog_finale.ui.workout

import com.example.gymlog_finale.data.model.Workout
import com.example.gymlog_finale.data.model.Exercise
import com.example.gymlog_finale.data.model.SplitPlan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Schermata di creazione scheda lato PT per un cliente specifico.
 * Riusa WorkoutDialog (identico a quello dell'utente) e dirotta il salvataggio
 * su saveWorkoutForClient così la scheda finisce con assignedTo = clientUid.
 */
@Composable
fun PtCreateWorkoutScreen(
    clientUid: String,
    clientName: String,
    onDone: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Chiude la schermata appena il salvataggio va a buon fine e resetta il flag.
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