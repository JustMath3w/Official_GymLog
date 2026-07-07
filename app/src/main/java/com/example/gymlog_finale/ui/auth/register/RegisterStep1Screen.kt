package com.example.gymlog_finale.ui.auth.register

// Primo step del wizard di registrazione (dati anagrafici).

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog_finale.ui.auth.components.AuthTextField

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun RegisterStep1Screen(
    onNextStep: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crea account", style = MaterialTheme.typography.headlineLarge, color = Color.Black)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Step 1 di 2",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            value = uiState.nome,
            onValueChange = viewModel::onNomeChange,
            label = "Nome"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthTextField(
            value = uiState.cognome,
            onValueChange = viewModel::onCognomeChange,
            label = "Cognome"
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (viewModel.validateStep1()) onNextStep() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Continua")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.Black
            )
        ) {
            Text("Hai già un account? Accedi")
        }
    }
}