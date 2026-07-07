package com.example.gymlog_finale.ui.workout

// Schermata Allenamento con selezione del giorno e gestione delle schede.

import com.example.gymlog_finale.data.model.Workout
import com.example.gymlog_finale.data.model.WorkoutLog
import com.example.gymlog_finale.data.model.Exercise
import com.example.gymlog_finale.data.model.SplitPlan

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog_finale.ui.workout.WorkoutViewModel
import java.util.Calendar

val SPLIT_TYPES = listOf(
    "Push",
    "Pull",
    "Legs",
    "Upper Body",
    "Lower Body",
    "Full Body",
    "Cardio",
    "Addome",
    "Rest"
)

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBackClick: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val workouts by viewModel.workouts.collectAsState()
    val workoutLogs by viewModel.workoutLogs.collectAsState()
    val currentDayIndex by viewModel.currentDayIndex.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val selectedWorkoutForToday by viewModel.selectedWorkoutForToday.collectAsState()
    val splitPlan by viewModel.splitPlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedExerciseForDetails by viewModel.currentExerciseDetails.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSplitSettingsDialog by remember { mutableStateOf(false) }
    var workoutToEdit by remember { mutableStateOf<Workout?>(null) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateForHistory by remember { mutableStateOf<Long?>(null) }
    var showHistoryForDateDialog by remember { mutableStateOf(false) }

    val todayIndex = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    // Predicato che verifica una condizione booleana sullo stato.
    fun isLogOnDayOfWeek(logTimestamp: Long, dayIndex: Int): Boolean {
        val logCal = Calendar.getInstance().apply { timeInMillis = logTimestamp }
        val targetCal = Calendar.getInstance().apply {
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            add(Calendar.DAY_OF_YEAR, -daysToSubtract + dayIndex)
        }
        return logCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
               logCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    }

    val isWorkoutMinimized by viewModel.isWorkoutMinimized.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    if (activeWorkout != null && !isWorkoutMinimized) {

        ActiveWorkoutScreen(
            workout = activeWorkout!!,
            onMinimizeClick = { viewModel.setWorkoutMinimized(true) },
            onCancelClick = { showCancelConfirmDialog = true },
            onComplete = { completedExercises ->
                viewModel.completeWorkout(activeWorkout!!, completedExercises)
            },
            onExerciseClick = { viewModel.loadExerciseDetails(it) }
        )

        if (showCancelConfirmDialog) {
            AlertDialog(
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                onDismissRequest = { showCancelConfirmDialog = false },
                title = { Text("Annullare l'allenamento?") },
                text = { Text("Sei sicuro di voler annullare l'allenamento? Tutti i progressi di questa sessione andranno persi.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cancelWorkout()
                            showCancelConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Si, Annulla", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCancelConfirmDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                    ) {
                        Text("No, Continua")
                    }
                }
            )
        }
    } else {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Allenamenti",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Seleziona Data Storico")
                        }
                        IconButton(onClick = { showSplitSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni Split Settimanale")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            val weekSplits = remember(splitPlan) {
                (0..6).map { index -> viewModel.getSplitForDayIndex(index) }
            }
            val targetSplit = remember(weekSplits, currentDayIndex) { weekSplits.getOrElse(currentDayIndex) { "Rest" } }
            val suggestedWorkouts = remember(workouts, targetSplit) {
                workouts.filter { it.splitType.equals(targetSplit, ignoreCase = true) }
            }
            val isToday = currentDayIndex == todayIndex
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (activeWorkout != null && isWorkoutMinimized) {
                        item {
                            ActiveWorkoutReminderCard(
                                workoutName = activeWorkout!!.name,
                                onClick = { viewModel.setWorkoutMinimized(false) }
                            )
                        }
                    }

                    item {
                        WeekDaysRow(
                            currentDayIndex = currentDayIndex,
                            weekSplits = weekSplits,
                            hasLogForDay = { index ->
                                workoutLogs.any { log -> isLogOnDayOfWeek(log.completedAt, index) }
                            },
                            onDaySelected = { viewModel.selectDay(it) }
                        )
                    }

                    if (currentDayIndex < todayIndex) {

                        item {
                            Text(
                                text = "Storico Allenamenti Svolti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val logsForDay = workoutLogs.filter { isLogOnDayOfWeek(it.completedAt, currentDayIndex) }
                        if (logsForDay.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "Nessun allenamento eseguito in questo giorno.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(logsForDay) { log ->
                                WorkoutLogCard(log = log, onExerciseClick = { viewModel.loadExerciseDetails(it) })
                            }
                        }

                    } else {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isToday) "Allenamento di Oggi" else "Programmazione",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                var showOverrideMenu by remember { mutableStateOf(false) }
                                val hasOverride = remember(splitPlan, currentDayIndex) { viewModel.hasDailyOverride(currentDayIndex) }
                                Box {
                                    SuggestionChip(
                                        onClick = { showOverrideMenu = true },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Split: $targetSplit")
                                                if (hasOverride) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Modificato",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (hasOverride) {
                                                Color.LightGray
                                            } else if (targetSplit == "Rest") {
                                                Color(0xFFF6F5F8)
                                            } else {
                                                Color.LightGray
                                            },
                                            labelColor = if (hasOverride) {
                                                Color.Black
                                            } else if (targetSplit == "Rest") {
                                                Color.DarkGray
                                            } else {
                                                Color.Black
                                            }
                                        )
                                    )
                                    DropdownMenu(
                                        expanded = showOverrideMenu,
                                        onDismissRequest = { showOverrideMenu = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        if (hasOverride) {
                                            DropdownMenuItem(
                                                text = { Text("Ripristina Programmazione", color = Color.Red, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    viewModel.clearDailyOverride(currentDayIndex)
                                                    showOverrideMenu = false
                                                }
                                            )
                                            HorizontalDivider()
                                        }
                                        SPLIT_TYPES.forEach { splitType ->
                                            DropdownMenuItem(
                                                text = { Text(splitType, color = Color.Black) },
                                                onClick = {
                                                    viewModel.saveDailyOverride(currentDayIndex, splitType)
                                                    showOverrideMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (targetSplit.equals("Rest", ignoreCase = true)) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.04f)
                                    ),
                                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bedtime,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = Color.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Giorno di Riposo",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Il riposo è fondamentale per la crescita e il recupero muscolare. Goditi questa pausa e ricarica le energie!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        } else if (suggestedWorkouts.isNotEmpty()) {
                            item {
                                var selectedSuggestedIndex by remember(suggestedWorkouts) { mutableStateOf(0) }
                                val suggestedWorkout = suggestedWorkouts.getOrNull(selectedSuggestedIndex) ?: suggestedWorkouts[0]

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(Color(0xFFF6F5F8))
                                            .padding(24.dp)
                                    ) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.Black
                                            ) {
                                                Text(
                                                    text = "SUGGERITO PER OGGI",
                                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                )
                                            }
                                            if (suggestedWorkouts.size > 1) {
                                                Text(
                                                    text = "${selectedSuggestedIndex + 1} di ${suggestedWorkouts.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.DarkGray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (suggestedWorkouts.size > 1) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                suggestedWorkouts.forEachIndexed { idx, w ->
                                                    val isSelected = idx == selectedSuggestedIndex
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected) Color.Black else Color.Black.copy(alpha = 0.1f),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedSuggestedIndex = idx }
                                                    ) {
                                                        Text(
                                                            text = w.name,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = if (isSelected) Color.White else Color.Black,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = suggestedWorkout.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = "${suggestedWorkout.exercises.size} esercizi",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = "Split: ${suggestedWorkout.splitType}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            suggestedWorkout.exercises.take(3).forEach { ex ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FitnessCenter,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color.Black.copy(alpha = 0.8f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = ex.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            if (suggestedWorkout.exercises.size > 3) {
                                                Text(
                                                    text = "+ altri ${suggestedWorkout.exercises.size - 3} esercizi",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(start = 24.dp)
                                                )
                                            }
                                        }

                                        if (isToday) {
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(
                                                onClick = { viewModel.startWorkout(suggestedWorkout) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.Black,
                                                    contentColor = Color.White
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "AVVIA ALLENAMENTO",
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Nessuna scheda trovata per lo split: $targetSplit",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Crea una scheda contrassegnata come '$targetSplit' nella sezione Modifica Scheda in basso per visualizzarla come suggerita.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        if (isToday) {
                            item {
                                Text(
                                    text = "Scegli un'altra scheda",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            item {
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { dropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                                    ) {
                                        Text(selectedWorkoutForToday?.name ?: "Seleziona una scheda...")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                    ) {
                                        workouts.forEach { w ->
                                            DropdownMenuItem(
                                                text = { Text("${w.name} (${w.splitType})", color = Color.Black) },
                                                onClick = {
                                                    viewModel.selectWorkoutForToday(w)
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedWorkoutForToday != null) {
                                item {
                                    Button(
                                        onClick = {
                                            viewModel.startWorkout(selectedWorkoutForToday!!)
                                            viewModel.selectWorkoutForToday(null)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AVVIA SCHEDA SELEZIONATA", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Modifica Scheda",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Button(
                                onClick = {
                                    workoutToEdit = null
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crea Scheda", color = Color.White)
                            }
                        }
                    }

                    if (workouts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8).copy(alpha = 0.2f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Nessuna scheda creata. Clicca su Crea Scheda per iniziare.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    } else {
                        items(workouts) { workout ->
                            WorkoutCard(
                                workout = workout,
                                onEdit = {
                                    workoutToEdit = workout
                                    showAddDialog = true
                                },
                                onDelete = { viewModel.deleteWorkout(workout.id) },
                                onExerciseClick = { viewModel.loadExerciseDetails(it) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        WorkoutDialog(
            workout = workoutToEdit,
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                workoutToEdit = null
            },
            onSave = { name, exercises, id, splitType ->
                viewModel.saveWorkout(name, exercises, id, splitType)
                showAddDialog = false
                workoutToEdit = null
            }
        )
    }

    if (showSplitSettingsDialog) {
        SplitSettingsDialog(
            currentPlan = splitPlan,
            onDismiss = { showSplitSettingsDialog = false },
            onSave = { startDate, endDate, newSplit ->
                viewModel.saveSplitPlan(startDate, endDate, newSplit)
                showSplitSettingsDialog = false
            }
        )
    }

    if (selectedExerciseForDetails != null) {
        ExerciseDetailsDialog(
            exercise = selectedExerciseForDetails!!,
            onDismiss = { viewModel.clearExerciseDetails() }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            selectedDateForHistory = selectedMillis
                            showHistoryForDateDialog = true
                        }
                    }
                ) {
                    Text("Conferma", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annulla", color = Color.Black)
                }
            }
        ) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Color.Black,
                    onSurfaceVariant = Color.Black,
                    outline = Color.Black
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        headlineContentColor = Color.Black,
                        weekdayContentColor = Color.Black,
                        subheadContentColor = Color.Black,
                        yearContentColor = Color.Black,
                        currentYearContentColor = Color.Black,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color.Black,
                        dayContentColor = Color.Black,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color.Black,
                        todayContentColor = Color.Black,
                        todayDateBorderColor = Color.Black
                    ),
                    title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "STORICO ALLENAMENTI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seleziona una data per vedere gli allenamenti svolti",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray.copy(alpha = 0.8f)
                        )
                    }
                }
            )
            }
        }
    }

    if (showHistoryForDateDialog && selectedDateForHistory != null) {
        val formattedDate = remember(selectedDateForHistory) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateForHistory!! }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            "$day/$month/$year"
        }

        val logsForSelectedDate = remember(workoutLogs, selectedDateForHistory) {
            val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateForHistory!! }
            workoutLogs.filter { log ->
                val logCal = Calendar.getInstance().apply { timeInMillis = log.completedAt }
                logCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                logCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            }
        }

        AlertDialog(
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            onDismissRequest = { showHistoryForDateDialog = false },
            title = {
                Text(
                    text = "Allenamenti del $formattedDate",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            text = {
                if (logsForSelectedDate.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun allenamento eseguito in questa data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        logsForSelectedDate.forEach { log ->
                            WorkoutLogCard(
                                log = log,
                                onExerciseClick = { viewModel.loadExerciseDetails(it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHistoryForDateDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                ) {
                    Text("Chiudi")
                }
            }
        )
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: Workout,
    onMinimizeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onComplete: (List<Exercise>) -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    val mutableExercises = remember(workout) {
        mutableStateListOf<Exercise>().apply {
            addAll(workout.exercises.map { it.copy() })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMinimizeClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Minimizza/Torna Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = onCancelClick) {
                        Icon(Icons.Default.Close, contentDescription = "Annulla Allenamento", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ALLENAMENTO IN CORSO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mutableExercises.size) { index ->
                        val exercise = mutableExercises[index]
                        ActiveExerciseCard(
                            exercise = exercise,
                            onValueChange = { updated ->
                                mutableExercises[index] = updated
                            },
                            onDetailsClick = { onExerciseClick(exercise) }
                        )
                    }
                }

                Button(
                    onClick = { onComplete(mutableExercises.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMPLETA ALLENAMENTO", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun ActiveExerciseCard(
    exercise: Exercise,
    onValueChange: (Exercise) -> Unit,
    onDetailsClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatExerciseDetails(exercise),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDetailsClick) {
                        Icon(Icons.Default.Info, contentDescription = "Dettagli Esercizio", tint = Color.Black.copy(alpha = 0.6f))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Riduci" else "Espandi",
                        tint = Color.DarkGray
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = exercise.sets,
                    onValueChange = { onValueChange(exercise.copy(sets = it)) },
                    label = { Text("Numero di Set (Serie)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                val setsCount = exercise.sets.toIntOrNull() ?: 0
                if (setsCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Specifiche per ogni Set:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val repsList = remember(exercise.reps, setsCount) {
                        parseCsvValues(exercise.reps, setsCount, "0")
                    }
                    val weightList = remember(exercise.weight, setsCount) {
                        parseCsvValues(exercise.weight, setsCount, "0")
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until setsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Set ${i + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(50.dp)
                                )

                                val textFieldColors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    focusedLabelColor = Color.Black,
                                    cursorColor = Color.Black
                                )
                                OutlinedTextField(
                                    value = repsList.getOrNull(i) ?: "",
                                    onValueChange = { newVal ->
                                        val newList = repsList.toMutableList()
                                        if (i < newList.size) {
                                            newList[i] = newVal
                                        } else {
                                            newList.add(newVal)
                                        }
                                        onValueChange(exercise.copy(reps = newList.joinToString(",")))
                                    },
                                    label = { Text("Rep") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = textFieldColors
                                )

                                OutlinedTextField(
                                    value = weightList.getOrNull(i) ?: "",
                                    onValueChange = { newVal ->
                                        val newList = weightList.toMutableList()
                                        if (i < newList.size) {
                                            newList[i] = newVal
                                        } else {
                                            newList.add(newVal)
                                        }
                                        onValueChange(exercise.copy(weight = newList.joinToString(",")))
                                    },
                                    label = { Text("Kg") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = textFieldColors
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSettingsDialog(
    currentPlan: SplitPlan,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Map<Int, String>) -> Unit
) {
    val tempSplit = remember { mutableStateMapOf<Int, String>().apply { putAll(currentPlan.split) } }
    var tempStartDate by remember { mutableStateOf(currentPlan.startDate) }
    var tempEndDate by remember { mutableStateOf(currentPlan.endDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val daysLabels = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")

    // Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
    fun formatMillisToDate(millis: Long): String {
        if (millis == 0L) return "Seleziona data"
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        return String.format("%02d/%02d/%04d", day, month, year)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Programmazione Split",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Data Inizio",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(formatMillisToDate(tempStartDate), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Data Fine",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(formatMillisToDate(tempEndDate), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Pianificazione Settimanale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    daysLabels.forEachIndexed { index, dayName ->
                        var isExpanded by remember { mutableStateOf(false) }
                        val selectedSplit = tempSplit[index] ?: "Rest"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box {
                                OutlinedButton(
                                    onClick = { isExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                                ) {
                                    Text(selectedSplit)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = isExpanded,
                                    onDismissRequest = { isExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    SPLIT_TYPES.forEach { splitType ->
                                        DropdownMenuItem(
                                            text = { Text(splitType, color = Color.Black) },
                                            onClick = {
                                                tempSplit[index] = splitType
                                                isExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                    ) {
                        Text("Annulla")
                    }
                    Button(
                        onClick = { onSave(tempStartDate, tempEndDate, tempSplit.toMap()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                    ) {
                        Text("Salva")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (tempStartDate != 0L) tempStartDate else null
        )
        DatePickerDialog(
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartDatePicker = false
                        tempStartDate = datePickerState.selectedDateMillis ?: 0L
                    }
                ) {
                    Text("Conferma", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annulla", color = Color.Black)
                }
            }
        ) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Color.Black,
                    onSurfaceVariant = Color.Black,
                    outline = Color.Black
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        headlineContentColor = Color.Black,
                        weekdayContentColor = Color.Black,
                        subheadContentColor = Color.Black,
                        yearContentColor = Color.Black,
                        currentYearContentColor = Color.Black,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color.Black,
                        dayContentColor = Color.Black,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color.Black,
                        todayContentColor = Color.Black,
                        todayDateBorderColor = Color.Black
                    )
                )
            }
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (tempEndDate != 0L) tempEndDate else null
        )
        DatePickerDialog(
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndDatePicker = false
                        tempEndDate = datePickerState.selectedDateMillis ?: 0L
                    }
                ) {
                    Text("Conferma", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annulla", color = Color.Black)
                }
            }
        ) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Color.Black,
                    onSurfaceVariant = Color.Black,
                    outline = Color.Black
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        headlineContentColor = Color.Black,
                        weekdayContentColor = Color.Black,
                        subheadContentColor = Color.Black,
                        yearContentColor = Color.Black,
                        currentYearContentColor = Color.Black,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color.Black,
                        dayContentColor = Color.Black,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color.Black,
                        todayContentColor = Color.Black,
                        todayDateBorderColor = Color.Black
                    )
                )
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun WeekDaysRow(
    currentDayIndex: Int,
    weekSplits: List<String>,
    hasLogForDay: (Int) -> Boolean,
    onDaySelected: (Int) -> Unit
) {
    val days = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == currentDayIndex
            val splitText = weekSplits.getOrNull(index) ?: "Rest"
            val hasLog = hasLogForDay(index)

            val splitAbbrev = when (splitText) {
                "Push" -> "Push"
                "Pull" -> "Pull"
                "Legs" -> "Legs"
                "Upper Body" -> "Uppr"
                "Lower Body" -> "Lowr"
                "Full Body" -> "Full"
                "Cardio" -> "Card"
                "Addome" -> "Core"
                "Rest" -> "Rest"
                else -> splitText.take(4)
            }

            val cardColor = if (isSelected) Color.Black else Color(0xFFF6F5F8)
            val contentColor = if (isSelected) Color.White else Color.DarkGray
            val splitColor = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDaySelected(index) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = splitAbbrev,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = splitColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasLog) {
                                    Color(0xFF4CAF50)
                                } else {
                                    Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun WorkoutLogCard(log: WorkoutLog, onExerciseClick: (Exercise) -> Unit) {
    val formattedTime = remember(log.completedAt) {
        val cal = Calendar.getInstance().apply { timeInMillis = log.completedAt }
        val hour = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        hour
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF4CAF50))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Completato ore $formattedTime",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    log.exercises.forEach { ex ->
                        Surface(
                            onClick = { onExerciseClick(ex) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF6F5F8).copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val setsCount = ex.sets.toIntOrNull() ?: 0
                                    val repsText = ex.reps.ifBlank { "0" }
                                    val weightText = ex.weight.ifBlank { "0" }

                                    val detailsSummary = if (setsCount > 0) {
                                        "$setsCount set • $repsText rep @ $weightText kg"
                                    } else {
                                        formatExerciseDetails(ex)
                                    }

                                    Text(
                                        text = detailsSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.DarkGray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun WorkoutCard(
    workout: Workout,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val (tagBg, tagText) = when (workout.splitType) {
        "Push" -> Pair(Color(0xFFFFF0E6), Color(0xFFFF6D00))
        "Pull" -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        "Legs" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "Cardio" -> Pair(Color(0xFFFCE4EC), Color(0xFFC2185B))
        "Full Body" -> Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        "Upper Body" -> Pair(Color(0xFFE0F7FA), Color(0xFF00838F))
        "Lower Body" -> Pair(Color(0xFFEFEBE9), Color(0xFF4E342E))
        "Addome" -> Pair(Color(0xFFFFFDE7), Color(0xFFF57F17))
        else -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = workout.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Riduci" else "Espandi",
                            tint = Color.DarkGray.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tagBg
                        ) {
                            Text(
                                text = workout.splitType.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = tagText,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${workout.exercises.size} esercizi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }

                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isReceivedFromPT = workout.isReceived && workout.senderIsPersonalTrainer && workout.senderId != currentUid

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isReceivedFromPT) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.05f))
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.05f))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                workout.exercises.forEach { exercise ->
                    Surface(
                        onClick = { onExerciseClick(exercise) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF6F5F8).copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatExerciseDetails(exercise),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Dettagli",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (workout.isReceived) {
                Spacer(modifier = Modifier.height(12.dp))

                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isSender = currentUid != null && workout.senderId == currentUid

                val bgColor = if (isSender) Color(0xFFF97316).copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f)
                val tintColor = if (isSender) Color(0xFFF97316) else Color.Black
                val textLabel = if (isSender) "Inviata a: ${workout.receiverName ?: "Cliente"}" else "Ricevuto da: ${workout.senderName ?: "Amico"}"
                val icon = if (isSender) Icons.Default.Send else Icons.Default.Person

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = tintColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = textLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = tintColor
                        )
                    }
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDialog(
    workout: Workout? = null,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit,
    onSave: (String, List<Exercise>, String, String) -> Unit
) {
    var name by remember { mutableStateOf(workout?.name ?: "") }
    val exercises = remember {
        mutableStateListOf<Exercise>().apply {
            if (workout != null && workout.exercises.isNotEmpty()) {
                addAll(workout.exercises)
            } else {
                add(Exercise())
            }
        }
    }

    var splitType by remember { mutableStateOf(workout?.splitType ?: "Push") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (workout == null) "Nuova Scheda" else "Modifica Scheda", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    actions = {
                        TextButton(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(name, exercises.toList(), workout?.id ?: "", splitType)
                                }
                            },
                            enabled = name.isNotBlank() && exercises.any { it.name.isNotBlank() }
                        ) {
                            Text("SALVA", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Allenamento") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            cursorColor = Color.Black,
                            unfocusedContainerColor = Color(0xFFF6F5F8)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Tipologia Split Scheda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    var splitDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { splitDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text("Tipo Split: $splitType")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = splitDropdownExpanded,
                            onDismissRequest = { splitDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                        ) {
                            SPLIT_TYPES.filter { it != "Rest" }.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = Color.Black) },
                                    onClick = {
                                        splitType = type
                                        splitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Esercizi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { exercises.add(Exercise()) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aggiungi")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        exercises.forEachIndexed { index, item ->
                            ExerciseInput(
                                viewModel = viewModel,
                                exercise = item,
                                onValueChange = { updated ->
                                    exercises[index] = updated
                                },
                                onRemove = {
                                    if (exercises.size > 1) exercises.removeAt(index)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseInput(
    viewModel: WorkoutViewModel,
    exercise: Exercise,
    onValueChange: (Exercise) -> Unit,
    onRemove: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val searchResults by viewModel.exerciseSearchResults.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = exercise.name,
                        onValueChange = {
                            onValueChange(exercise.copy(name = it))
                            viewModel.onSearchQueryChange(it)
                            isExpanded = it.length >= 2
                        },
                        label = { Text("Cerca Esercizio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            cursorColor = Color.Black
                        ),
                        trailingIcon = {
                            if (exercise.name.isNotEmpty()) {
                                IconButton(onClick = { onValueChange(exercise.copy(name = "")) }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        }
                    )

                    if (isExpanded && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp)
                                .heightIn(max = 250.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            LazyColumn {
                                items(searchResults) { result ->
                                    ListItem(
                                        colors = ListItemDefaults.colors(containerColor = Color.White),
                                        headlineContent = { Text(result.name ?: "", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("${result.bodyPart} | ${result.equipment}") },
                                        modifier = Modifier.clickable {
                                            onValueChange(exercise.copy(
                                                id = result.exerciseId ?: result.id ?: "",
                                                name = result.name ?: "",
                                                gifUrl = result.gifUrl,
                                                instructions = result.instructions,
                                                bodyPart = result.bodyPart,
                                                target = result.target
                                            ))
                                            isExpanded = false
                                        }
                                    )
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = Color.Red.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    cursorColor = Color.Black
                )
                OutlinedTextField(
                    value = exercise.sets,
                    onValueChange = { onValueChange(exercise.copy(sets = it)) },
                    label = { Text("Set") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = exercise.reps,
                    onValueChange = { onValueChange(exercise.copy(reps = it)) },
                    label = { Text("Rep") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = exercise.weight,
                    onValueChange = { onValueChange(exercise.copy(weight = it)) },
                    label = { Text("Kg") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
            }
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@kotlin.OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailsDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.Black)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = exercise.name.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(exercise.bodyPart?.uppercase() ?: "CORPO") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Black.copy(alpha = 0.1f))
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text(exercise.target?.uppercase() ?: "MUSCOLO") }
                        )

                        if (!exercise.youtubeVideoId.isNullOrEmpty()) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${exercise.youtubeVideoId}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("VIDEO", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Istruzioni",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (exercise.instructions.isNotEmpty()) {
                        exercise.instructions.forEachIndexed { index, step ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.1f), CircleShape)
                                        .wrapContentSize(Alignment.Center)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Nessuna istruzione disponibile per questo esercizio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// Funzione di supporto interna alla classe.
private fun parseCsvValues(rawString: String, targetSize: Int, defaultValue: String): List<String> {
    if (targetSize <= 0) return emptyList()
    val parts = if (rawString.isEmpty()) emptyList() else rawString.split(",")
    val list = mutableListOf<String>()
    for (i in 0 until targetSize) {
        val value = if (i < parts.size) {
            parts[i]
        } else {
            parts.lastOrNull() ?: defaultValue
        }
        list.add(value)
    }
    return list
}

// Espone al chiamante la funzionalità indicata coordinando i livelli sottostanti.
fun formatExerciseDetails(exercise: Exercise): String {
    val s = exercise.sets.ifBlank { "0" }
    return "$s set"
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun ActiveWorkoutReminderCard(
    workoutName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "active_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.Red.copy(alpha = alpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ALLENAMENTO IN CORSO",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = Color.Red,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = workoutName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("RIPRENDI", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
