package com.example.gymlog_finale.ui.profile

// Schermata Profilo utente: foto, dati anagrafici e fisici, azioni account.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog_finale.util.Constants
import com.example.gymlog_finale.ui.profile.components.ProfileCard
import com.example.gymlog_finale.ui.profile.components.ProfileInfoRow
import com.example.gymlog_finale.ui.profile.components.SectionHeader

// Enum ProfileDialog: insieme finito di valori usati nell'app.
private enum class ProfileDialog {
    NONE, USERNAME, NOME, COGNOME, ANNO, ALTEZZA, PESO, OBIETTIVO, PT,
    CAMBIO_PASSWORD, RESET_PASSWORD, ELIMINA_ACCOUNT
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user = state.user

    val snackbarHostState = remember { SnackbarHostState() }
    var openDialog by remember { mutableStateOf(ProfileDialog.NONE) }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        val msg = state.successMessage ?: state.errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    titleContentColor = androidx.compose.ui.graphics.Color.Black,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.Black
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Foto profilo",
                    modifier = Modifier.size(40.dp),
                    tint = androidx.compose.ui.graphics.Color.Black
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = user?.username?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black
            )
            Text(
                text = listOfNotNull(user?.nome, user?.cognome).joinToString(" ").ifBlank { "" },
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray
            )

            if (user?.isPersonalTrainer == true) {
                Spacer(Modifier.height(6.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Personal Trainer") },
                    leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader("Account", modifier = Modifier.align(Alignment.Start))
            ProfileCard {
                ProfileInfoRow(
                    icona = Icons.Default.Person,
                    iconColor = androidx.compose.ui.graphics.Color.Black,
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                    etichetta = "Username",
                    valore = user?.username,
                    onClick = { openDialog = ProfileDialog.USERNAME }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.Email,
                    iconColor = androidx.compose.ui.graphics.Color(0xFF16A085),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFE8F8F5),
                    etichetta = "Email (non modificabile)",
                    valore = user?.email
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.Lock,
                    iconColor = androidx.compose.ui.graphics.Color(0xFFE67E22),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFFFF4E6),
                    etichetta = "Cambia password",
                    valore = "••••••••",
                    onClick = { openDialog = ProfileDialog.CAMBIO_PASSWORD }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.LockReset,
                    iconColor = androidx.compose.ui.graphics.Color(0xFF2980B9),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFEBF5FB),
                    etichetta = "Reset password",
                    valore = "Invia email di reset",
                    onClick = { openDialog = ProfileDialog.RESET_PASSWORD }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.DeleteForever,
                    iconColor = androidx.compose.ui.graphics.Color(0xFFC0392B),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFFDEDEC),
                    etichetta = "Elimina account",
                    valore = "Cancellazione definitiva",
                    onClick = { openDialog = ProfileDialog.ELIMINA_ACCOUNT }
                )
            }

            SectionHeader("Dati personali", modifier = Modifier.align(Alignment.Start))
            ProfileCard {
                ProfileInfoRow(
                    icona = Icons.Default.Badge,
                    iconColor = androidx.compose.ui.graphics.Color.Black,
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                    etichetta = "Nome",
                    valore = user?.nome,
                    onClick = { openDialog = ProfileDialog.NOME }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.Badge,
                    iconColor = androidx.compose.ui.graphics.Color(0xFF16A085),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFE8F8F5),
                    etichetta = "Cognome",
                    valore = user?.cognome,
                    onClick = { openDialog = ProfileDialog.COGNOME }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.Cake,
                    iconColor = androidx.compose.ui.graphics.Color(0xFFF1C40F),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFFEF9E7),
                    etichetta = "Anno di nascita",
                    valore = user?.annoDiNascita?.takeIf { it > 0 }?.toString(),
                    onClick = { openDialog = ProfileDialog.ANNO }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.Height,
                    iconColor = androidx.compose.ui.graphics.Color(0xFF8E44AD),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFF4ECF8),
                    etichetta = "Altezza",
                    valore = user?.altezza?.takeIf { it > 0 }?.let { "$it cm" },
                    onClick = { openDialog = ProfileDialog.ALTEZZA }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.MonitorWeight,
                    iconColor = androidx.compose.ui.graphics.Color(0xFFD35400),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFFCEBE6),
                    etichetta = "Peso",
                    valore = user?.peso?.takeIf { it > 0.0 }?.let { "$it kg" },
                    onClick = { openDialog = ProfileDialog.PESO }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.Flag,
                    iconColor = androidx.compose.ui.graphics.Color(0xFF27AE60),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFEAFAF1),
                    etichetta = "Obiettivo",
                    valore = user?.obiettivo,
                    onClick = { openDialog = ProfileDialog.OBIETTIVO }
                )
                com.example.gymlog_finale.ui.profile.components.ProfileDivider()
                ProfileInfoRow(
                    icona = Icons.Default.WorkspacePremium,
                    iconColor = androidx.compose.ui.graphics.Color(0xFFF39C12),
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFFEF5E7),
                    etichetta = "Personal Trainer",
                    valore = if (user?.isPersonalTrainer == true) "Sì" else "No",
                    onClick = { openDialog = ProfileDialog.PT }
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }

            Spacer(Modifier.height(16.dp))
        }

        when (openDialog) {
            ProfileDialog.USERNAME -> TextEditDialog(
                titolo = "Username",
                valoreIniziale = user?.username.orEmpty(),
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = {
                    viewModel.updateField("username", it.trim())
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.NOME -> TextEditDialog(
                titolo = "Nome",
                valoreIniziale = user?.nome.orEmpty(),
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = {
                    viewModel.updateField("nome", it.trim())
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.COGNOME -> TextEditDialog(
                titolo = "Cognome",
                valoreIniziale = user?.cognome.orEmpty(),
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = {
                    viewModel.updateField("cognome", it.trim())
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.ANNO -> NumberEditDialog(
                titolo = "Anno di nascita",
                valoreIniziale = user?.annoDiNascita?.takeIf { it > 0 }?.toString().orEmpty(),
                isDouble = false,
                suffix = "",
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = { intVal, _ ->
                    if (intVal != null) viewModel.updateField("annoDiNascita", intVal)
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.ALTEZZA -> NumberEditDialog(
                titolo = "Altezza",
                valoreIniziale = user?.altezza?.takeIf { it > 0 }?.toString().orEmpty(),
                isDouble = false,
                suffix = "cm",
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = { intVal, _ ->
                    if (intVal != null) viewModel.updateField("altezza", intVal)
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.PESO -> NumberEditDialog(
                titolo = "Peso",
                valoreIniziale = user?.peso?.takeIf { it > 0.0 }?.toString().orEmpty(),
                isDouble = true,
                suffix = "kg",
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = { _, dblVal ->
                    if (dblVal != null) viewModel.updateField("peso", dblVal)
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.OBIETTIVO -> DropdownEditDialog(
                titolo = "Obiettivo",
                opzioni = Constants.AVAILABLE_GOALS,
                valoreIniziale = user?.obiettivo.orEmpty(),
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = {
                    viewModel.updateField("obiettivo", it)
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.PT -> SwitchEditDialog(
                titolo = "Sei un Personal Trainer?",
                valoreIniziale = user?.isPersonalTrainer == true,
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = {
                    viewModel.updateField("PersonalTrainer", it)
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.CAMBIO_PASSWORD -> ChangePasswordDialog(
                isSaving = state.isSaving,
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = { old, new ->
                    viewModel.changePassword(old, new)
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.RESET_PASSWORD -> AlertDialog(
        onDismissRequest = { openDialog = ProfileDialog.NONE },
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
                title = { Text("Reset password", color = Color.Black) },
                text = { Text("Invieremo una mail con il link di reset a ${user?.email ?: "—"}. Continuare?", color = Color.Black) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.sendResetPasswordEmail()
                        openDialog = ProfileDialog.NONE
                    }) { Text("Invia", color = Color.Black) }
                },
                dismissButton = {
                    TextButton(onClick = { openDialog = ProfileDialog.NONE }) { Text("Annulla", color = Color.Black) }
                }
            )
            ProfileDialog.ELIMINA_ACCOUNT -> DeleteAccountDialog(
                isSaving = state.isSaving,
                onDismiss = { openDialog = ProfileDialog.NONE },
                onConfirm = { pwd ->
                    viewModel.deleteAccount(pwd) {
                        viewModel.logout()
                        onLogout()
                    }
                    openDialog = ProfileDialog.NONE
                }
            )
            ProfileDialog.NONE -> Unit
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun TextEditDialog(
    titolo: String,
    valoreIniziale: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(valoreIniziale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text("Modifica $titolo", color = Color.Black) },
        text = {
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black)) {
                OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(titolo) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank() && value != valoreIniziale
            ) { Text("Salva", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black) } }
    )
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun NumberEditDialog(
    titolo: String,
    valoreIniziale: String,
    isDouble: Boolean,
    suffix: String,
    onDismiss: () -> Unit,
    onConfirm: (Int?, Double?) -> Unit
) {
    var value by remember { mutableStateOf(valoreIniziale) }
    val keyboard = if (isDouble) KeyboardType.Decimal else KeyboardType.Number
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text("Modifica $titolo", color = Color.Black) },
        text = {
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black)) {
                OutlinedTextField(
                value = value,
                onValueChange = { v -> value = v.filter { it.isDigit() || (isDouble && (it == '.' || it == ',')) } },
                label = { Text(titolo) },
                suffix = { if (suffix.isNotEmpty()) Text(suffix) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth()
            )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isDouble) {
                        val dbl = value.replace(',', '.').toDoubleOrNull()
                        onConfirm(null, dbl)
                    } else {
                        val intVal = value.toIntOrNull()
                        onConfirm(intVal, null)
                    }
                },
                enabled = value.isNotBlank()
            ) { Text("Salva", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black) } }
    )
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun DropdownEditDialog(
    titolo: String,
    opzioni: List<String>,
    valoreIniziale: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selected by remember { mutableStateOf(valoreIniziale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text("Modifica $titolo", color = Color.Black) },
        text = {
            Column {
                opzioni.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = opt }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == opt,
                            onClick = { selected = opt },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.Black)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(opt, color = Color.Black)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotBlank()
            ) { Text("Salva", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black) } }
    )
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun SwitchEditDialog(
    titolo: String,
    valoreIniziale: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(valoreIniziale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text(titolo, color = Color.Black) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Black
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(if (checked) "Sì" else "No", color = Color.Black)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(checked) }) { Text("Salva", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black) } }
    )
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun ChangePasswordDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    val mismatch = newPwd.isNotEmpty() && confirmPwd.isNotEmpty() && newPwd != confirmPwd
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text("Cambia password", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black)) {
                OutlinedTextField(
                    value = oldPwd,
                    onValueChange = { oldPwd = it },
                    label = { Text("Password attuale") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black)) {
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it },
                    label = { Text("Nuova password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black)) {
                OutlinedTextField(
                    value = confirmPwd,
                    onValueChange = { confirmPwd = it },
                    label = { Text("Conferma nuova password") },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
                if (mismatch) {
                    Text(
                        "Le due password non coincidono",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(oldPwd, newPwd) },
                enabled = !isSaving && oldPwd.isNotEmpty() && newPwd.length >= 6 && newPwd == confirmPwd
            ) { Text("Salva", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black) } }
    )
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
private fun DeleteAccountDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text("Elimina account", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Questa azione è IRREVERSIBILE. Tutti i tuoi dati saranno cancellati.", color = Color.Black)
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black)) {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("Conferma con password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pwd) },
                enabled = !isSaving && pwd.isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Elimina") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black) } }
    )
}