package com.example.gymlog_finale.ui.auth.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog_finale.ui.auth.components.AuthTextField

private val obiettivi = listOf("Perdita di peso", "Aumento massa", "Mantenimento", "Resistenza", "Forza")

/**
 * Secondo step di registrazione: raccoglie username, password, dati fisici e obiettivo.
 * Al completamento crea l'account Firebase e salva il profilo su Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep2Screen(
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confermaVisible by remember { mutableStateOf(false) }
    var obiettivoExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            viewModel.onRegisterHandled()
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crea account", style = MaterialTheme.typography.headlineLarge, color = Color.Black)

        Text(
            "Step 2 di 2",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        AuthTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChange,
            label = "Username"
        )

        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            }
        )

        AuthTextField(
            value = uiState.confermaPassword,
            onValueChange = viewModel::onConfermaPasswordChange,
            label = "Conferma password",
            visualTransformation = if (confermaVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confermaVisible = !confermaVisible }) {
                    Icon(
                        imageVector = if (confermaVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            }
        )

        // Dropdown per la selezione obiettivo
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
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Color.Black,
                                    unfocusedLabelColor = Color.DarkGray,
                                    cursorColor = Color.Black,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedTrailingIconColor = Color.Black,
                                    unfocusedTrailingIconColor = Color.Black
                                )
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

        // Toggle Personal Trainer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sono un Personal Trainer",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Switch(
                checked = uiState.isPersonalTrainer,
                onCheckedChange = viewModel::onIsPersonalTrainerChange,
                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color.Black,
                                    uncheckedThumbColor = Color.DarkGray,
                                    uncheckedTrackColor = Color.LightGray
                                )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBackClick,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Indietro")
            }

            Button(
                onClick = viewModel::register,
                enabled = !uiState.isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Registrati")
                }
            }
        }
    }
    }
