package com.example.gymlog_finale.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymlog_finale.ui.auth.components.AuthTextField

private val obiettivi = listOf("Perdita di peso", "Aumento massa", "Mantenimento", "Resistenza", "Forza")

/**
 * Schermata di onboarding per utenti che si registrano tramite Google.
 * Nome, cognome ed email sono già pre-popolati dal profilo Google.
 * Raccoglie solo i dati mancanti: username, obiettivo, anno di nascita, altezza, peso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleOnboardingScreen(
    onOnboardingSuccess: () -> Unit,
    viewModel: RegisterViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var obiettivoExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            viewModel.onRegisterHandled()
            onOnboardingSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Completa il profilo", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ciao ${uiState.nome}! Aggiungi gli ultimi dettagli.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        AuthTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChange,
            label = "Username"
        )

        ExposedDropdownMenuBox(
            expanded = obiettivoExpanded,
            onExpandedChange = { obiettivoExpanded = it }
        ) {
            OutlinedTextField(
                value = uiState.obiettivo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Obiettivo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = obiettivoExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = obiettivoExpanded,
                onDismissRequest = { obiettivoExpanded = false }
            ) {
                obiettivi.forEach { obiettivo ->
                    DropdownMenuItem(
                        text = { Text(obiettivo) },
                        onClick = {
                            viewModel.onObiettivoChange(obiettivo)
                            obiettivoExpanded = false
                        }
                    )
                }
            }
        }

        AuthTextField(
            value = uiState.annoDiNascita,
            onValueChange = viewModel::onAnnoDiNascitaChange,
            label = "Anno di nascita",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        AuthTextField(
            value = uiState.altezza,
            onValueChange = viewModel::onAltezzaChange,
            label = "Altezza (cm)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        AuthTextField(
            value = uiState.peso,
            onValueChange = viewModel::onPesoChange,
            label = "Peso (kg)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sono un Personal Trainer", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = uiState.isPersonalTrainer,
                onCheckedChange = viewModel::onIsPersonalTrainerChange
            )
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = viewModel::completeGoogleOnboarding,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Salva e continua")
            }
        }
    }
}