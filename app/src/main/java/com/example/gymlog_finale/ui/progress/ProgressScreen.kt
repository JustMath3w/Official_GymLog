package com.example.gymlog_finale.ui.progress

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


import android.content.Context
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Schermata progressi che mostra andamento peso, confronto foto
 * e area dedicata alla ricerca del progresso di un esercizio.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showAllPhotosDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = { Text("Progressi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true },
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Text("+")
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Black)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    PhotoComparisonSection(
                        firstPhoto = uiState.firstPhoto,
                        lastPhoto = uiState.lastPhoto,
                        hasPhotos = uiState.allPhotos.isNotEmpty(),
                        onViewAllClick = { showAllPhotosDialog = true }
                    )

                    StatsSection(
                        statsItems = uiState.statsItems,
                        weightChartPoints = uiState.weightChartPoints
                    )

                    TopExercisesSection(
                        topExercises = uiState.topExercises
                    )

                    ExerciseProgressSection(
                        exerciseQuery = uiState.exerciseQuery,
                        selectedMetric = uiState.selectedMetric,
                        exerciseProgressPoints = uiState.exerciseProgressPoints,
                        errorMessage = uiState.errorMessage,
                        onExerciseQueryChange = viewModel::onExerciseQueryChange,
                        onMetricSelected = viewModel::onMetricSelected,
                        onSearchClick = {
                            viewModel.searchExerciseProgress()
                            coroutineScope.launch {
                                delay(200)
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddProgressDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onConfirm = { weightText, photoUri ->
                    viewModel.addProgressLog(
                        context = context,
                        weightText = weightText,
                        photoUri = photoUri
                    )
                    showAddDialog = false
                }
            )
        }

        if (showAllPhotosDialog) {
            AllPhotosDialog(
                photos = uiState.allPhotos,
                onDismiss = { showAllPhotosDialog = false },
                onDeletePhoto = { photo ->
                    viewModel.deleteProgressPhoto(photo)
                    if (uiState.allPhotos.size <= 1) {
                        showAllPhotosDialog = false
                    }
                }
            )
        }
    }
}

/**
 * Dialog che consente l'inserimento di un nuovo progresso con peso
 * precompilato dal profilo e foto opzionale da galleria o fotocamera.
 */
@Composable
private fun AddProgressDialog(
    viewModel: ProgressViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    val context = LocalContext.current
    var weightText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentProfileWeight { currentWeight ->
            weightText = if (currentWeight > 0.0) {
                currentWeight.toString()
            } else {
                ""
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedPhotoUri = cameraPhotoUri
        }
    }

    AlertDialog(
        containerColor = Color.White,
        onDismissRequest = onDismiss,
        title = {
            Text("Nuovo progresso")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.DarkGray,
                        cursorColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedTrailingIconColor = Color.Black,
                        unfocusedTrailingIconColor = Color.Black,
                        selectionColors = TextSelectionColors(
                            handleColor = Color.Black,
                            backgroundColor = Color.LightGray
                        )
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Galleria")
                    }

                    Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = {
                            val newUri = createTempImageUri(context)
                            cameraPhotoUri = newUri
                            cameraLauncher.launch(newUri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fotocamera")
                    }
                }

                if (selectedPhotoUri != null) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = "Anteprima foto progresso",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    TextButton(colors = ButtonDefaults.textButtonColors(contentColor = Color.Black), onClick = { selectedPhotoUri = null }
                    ) {
                        Text("Rimuovi foto")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEBEBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessuna foto selezionata",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = {
                    onConfirm(weightText, selectedPhotoUri)
                }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(colors = ButtonDefaults.textButtonColors(contentColor = Color.Black), onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Crea un Uri temporaneo usato dalla fotocamera per salvare lo scatto in un file locale.
 */
private fun createTempImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "progress_images").apply {
        mkdirs()
    }

    val file = File.createTempFile(
        "progress_${System.currentTimeMillis()}",
        ".jpg",
        directory
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/**
 * Mostra il confronto tra prima e ultima foto disponibili.
 */
@Composable
private fun PhotoComparisonSection(
    firstPhoto: ProgressPhotoItem?,
    lastPhoto: ProgressPhotoItem?,
    hasPhotos: Boolean,
    onViewAllClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Confronto foto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProgressPhotoCard(
                    modifier = Modifier.weight(1f),
                    title = "Prima foto",
                    item = firstPhoto
                )

                ProgressPhotoCard(
                    modifier = Modifier.weight(1f),
                    title = "Ultima foto",
                    item = lastPhoto
                )
            }

            if (hasPhotos) {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = onViewAllClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vedi tutte le foto")
                }
            }
        }
    }
}

/**
 * Mostra tutte le foto con possibilità di scorrimento ed eliminazione.
 */
@Composable
private fun AllPhotosDialog(
    photos: List<ProgressPhotoItem>,
    onDismiss: () -> Unit,
    onDeletePhoto: (ProgressPhotoItem) -> Unit
) {
    var currentIndex by remember(photos) { mutableIntStateOf(0) }
    val currentPhoto = photos.getOrNull(currentIndex)
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    AlertDialog(
        containerColor = Color.White,
        onDismissRequest = onDismiss,
        title = {
            Text("Tutte le foto")
        },
        text = {
            if (currentPhoto == null) {
                Text("Nessuna foto disponibile.")
            } else {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = File(currentPhoto.localPhotoUri),
                        contentDescription = "Foto progresso ${currentIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Text(
                        text = "Peso: ${String.format("%.1f", currentPhoto.weightKg)} kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Data: ${currentPhoto.date.format(formatter)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Foto ${currentIndex + 1} di ${photos.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = { currentIndex-- },
                            modifier = Modifier.weight(1f),
                            enabled = currentIndex > 0
                        ) {
                            Text("Precedente")
                        }

                        Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = { currentIndex++ },
                            modifier = Modifier.weight(1f),
                            enabled = currentIndex < photos.lastIndex
                        ) {
                            Text("Successiva")
                        }
                    }

                    Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = { onDeletePhoto(currentPhoto) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Elimina foto")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(colors = ButtonDefaults.textButtonColors(contentColor = Color.Black), onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

/**
 * Mostra una card foto con peso e data.
 * Se è disponibile un'immagine la renderizza dal file locale, altrimenti mostra un placeholder.
 */
@Composable
private fun ProgressPhotoCard(
    modifier: Modifier = Modifier,
    title: String,
    item: ProgressPhotoItem?
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            if (item?.localPhotoUri?.isNotBlank() == true) {
                AsyncImage(
                    model = File(item.localPhotoUri),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBEBEB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna foto",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = item?.let { "${String.format("%.1f", it.weightKg)} kg" } ?: "-",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = item?.date?.format(formatter) ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Mostra le statistiche sintetiche e il grafico peso nel tempo.
 */
@Composable
private fun StatsSection(
    statsItems: List<ProgressStatItem>,
    weightChartPoints: List<WeightChartPoint>
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Statistiche",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Variazione peso nel tempo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            WeightLineChart(points = weightChartPoints)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                statsItems.forEach { item ->
                    StatCard(item = item)
                }
            }
        }
    }
}

/**
 * Mostra un grafico lineare dell'andamento peso nel tempo usando MPAndroidChart.
 */
@Composable
private fun WeightLineChart(
    points: List<WeightChartPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun dato disponibile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val labels = points.map { it.date.format(DateTimeFormatter.ofPattern("dd/MM")) }
    val entries = points.mapIndexed { index, point ->
        Entry(index.toFloat(), point.weightKg.toFloat())
    }

    val lineColor = Color.Black.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f),
        factory = { context ->
            LineChart(context).apply {
                description = Description().apply {
                    text = ""
                }

                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(false)
                setPinchZoom(false)
                setNoDataText("Nessun dato disponibile")
                legend.isEnabled = false
                setDrawGridBackground(false)
                setExtraOffsets(8f, 8f, 8f, 8f)

                axisRight.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    labelRotationAngle = -30f
                    setTextColor(textColor)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return labels.getOrElse(index) { "" }
                        }
                    }
                }

                axisLeft.apply {
                    granularity = 0.5f
                    setTextColor(textColor)
                    setGridColor(gridColor)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format("%.1f kg", value)
                        }
                    }
                }
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, "Peso").apply {
                color = lineColor
                lineWidth = 2.5f
                setCircleColor(lineColor)
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextColor = textColor
                valueTextSize = 10f
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 35
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
/**
 * Mostra una statistica compatta in formato card.
 */
@Composable
private fun StatCard(item: ProgressStatItem) {
    Card(modifier = Modifier.width(160.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Mostra la classifica degli esercizi più eseguiti.
 */
@Composable
private fun TopExercisesSection(
    topExercises: List<TopExerciseItem>
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Top esercizi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (topExercises.isEmpty()) {
                Text(
                    text = "Nessun dato disponibile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                topExercises.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${index + 1}. ${item.exerciseName}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = item.executionCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mostra la sezione per cercare il progresso di un esercizio.
 */
@Composable
private fun ExerciseProgressSection(
    exerciseQuery: String,
    selectedMetric: ExerciseProgressMetric,
    exerciseProgressPoints: List<ExerciseProgressPoint>,
    errorMessage: String?,
    onExerciseQueryChange: (String) -> Unit,
    onMetricSelected: (ExerciseProgressMetric) -> Unit,
    onSearchClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM") }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Progressione esercizio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = exerciseQuery,
                onValueChange = onExerciseQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome esercizio") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearchClick() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.DarkGray,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedTrailingIconColor = Color.Black,
                    unfocusedTrailingIconColor = Color.Black,
                    selectionColors = TextSelectionColors(
                        handleColor = Color.Black,
                        backgroundColor = Color.LightGray
                    )
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMetric == ExerciseProgressMetric.WEIGHT,
                    onClick = { onMetricSelected(ExerciseProgressMetric.WEIGHT) },
                    label = { Text("Peso") },
                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = Color.Gray,
                                    selectedContainerColor = Color.Black,
                                    selectedLabelColor = Color.White
                                )
            )

                FilterChip(
                    selected = selectedMetric == ExerciseProgressMetric.REPS,
                    onClick = { onMetricSelected(ExerciseProgressMetric.REPS) },
                    label = { Text("Reps") },
                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = Color.Gray,
                                    selectedContainerColor = Color.Black,
                                    selectedLabelColor = Color.White
                                )
            )
            }

            Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerca")
            }

            if (!errorMessage.isNullOrBlank() && exerciseProgressPoints.isEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (exerciseProgressPoints.isNotEmpty()) {
                ExerciseProgressChartCard(
                    title = if (selectedMetric == ExerciseProgressMetric.WEIGHT) {
                        "Andamento carico"
                    } else {
                        "Andamento ripetizioni"
                    },
                    points = exerciseProgressPoints.map { it.value },
                    labels = exerciseProgressPoints.map { it.date.format(formatter) },
                    metric = selectedMetric
                )
            }
        }
    }
}

/**
 * Mostra una card testuale per i dati esercizio, mantenendo la stessa UI esistente.
 */
@Composable
private fun ExerciseProgressChartCard(
    title: String,
    points: List<Double>,
    labels: List<String>,
    metric: ExerciseProgressMetric
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (points.isEmpty() || labels.isEmpty()) {
                Text(
                    text = "Nessun dato disponibile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                points.forEachIndexed { index, point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = labels.getOrElse(index) { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (metric == ExerciseProgressMetric.WEIGHT) {
                                String.format("%.1f kg", point)
                            } else {
                                point.toInt().toString()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.padding(end = 80.dp)
                        )
                    }
                }
            }
        }
    }
}