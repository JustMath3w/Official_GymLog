package com.example.gymlog_finale.ui.diet

import com.example.gymlog_finale.data.model.DailyDietStats
import com.example.gymlog_finale.data.model.FoodItem

import com.example.gymlog_finale.ui.diet.components.MealCategoryCard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog_finale.data.network.OpenFoodFactsClient
import com.example.gymlog_finale.data.network.Product
import com.example.gymlog_finale.ui.theme.GymLogFinaleTheme
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    viewModel: DietViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    // Stati per gestire l'apertura del menu di inserimento e la categoria selezionata
    var showAddMealSheet by remember { mutableStateOf(false) }
    var selectedMealCategory by remember { mutableStateOf<String?>(null) }
    var foodToEdit by remember { mutableStateOf<FoodItem?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Osserviamo lo stato del viewmodel
    val currentDayIndex by viewModel.currentDayIndex.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val currentStats = weeklyStats[currentDayIndex] ?: DailyDietStats()

    LaunchedEffect(Unit) {
        viewModel.checkWeekAndResetIfNeeded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Piano Alimentare", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Torna indietro")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Storico Alimentare")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Giorni della settimana
            WeekDaysRow(
                currentDayIndex = currentDayIndex,
                onDaySelected = { viewModel.selectDay(it) }
            )

            // 2. Calorie Totali
            CaloriesCard(consumed = currentStats.consumedCalories, total = currentStats.totalCalories)

            // 3. Macronutrienti
            MacrosSection(
                carbsConsumed = currentStats.consumedCarbs.toInt(), carbsTotal = currentStats.totalCarbs.toInt(),
                proteinsConsumed = currentStats.consumedProteins.toInt(), proteinsTotal = currentStats.totalProteins.toInt(),
                fatsConsumed = currentStats.consumedFats.toInt(), fatsTotal = currentStats.totalFats.toInt()
            )

            // 4. Lista dei pasti inseriti (Tutti i pasti)
            val groupedFoods = currentStats.foods.groupBy { it.category }
            MEAL_CATEGORIES.forEach { category ->
                MealCategoryCard(
                    categoryName = category,
                    foods = groupedFoods[category] ?: emptyList(),
                    currentDayIndex = currentDayIndex,
                    onAddClick = {
                        foodToEdit = null
                        selectedMealCategory = category
                        showAddMealSheet = true
                    },
                    onEditClick = { food ->
                        foodToEdit = food
                        selectedMealCategory = food.category
                        showAddMealSheet = true
                    },
                    onDeleteClick = { food ->
                        viewModel.deleteFood(food)
                    }
                )
            }
        }
    }


    // Menu a tendina inferiore (Bottom Sheet) per l'inserimento
    if (showAddMealSheet && selectedMealCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showAddMealSheet = false 
                selectedMealCategory = null
                foodToEdit = null
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Form Inserimento Alimento Singolo
                Text("Aggiungi a $selectedMealCategory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                AddFoodForm(
                    initialFood = foodToEdit,
                    category = selectedMealCategory!!,
                    onCancel = { 
                        showAddMealSheet = false
                        selectedMealCategory = null
                        foodToEdit = null
                    },
                    onSave = { updatedFood -> 
                        viewModel.addOrUpdateFood(updatedFood)
                        showAddMealSheet = false 
                        selectedMealCategory = null
                        foodToEdit = null
                    }
                )
            }
        }
    }
}

@Composable
fun WeekDaysRow(currentDayIndex: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == currentDayIndex
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.Black else Color(0xFFF6F5F8))
                    .clickable { onDaySelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else Color.DarkGray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CaloriesCard(consumed: Int, total: Int) {
    val progress = if (total > 0) (consumed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Calorie",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$consumed",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / $total kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color.Black,
                trackColor = Color.Black.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun MacrosSection(
    carbsConsumed: Int, carbsTotal: Int,
    proteinsConsumed: Int, proteinsTotal: Int,
    fatsConsumed: Int, fatsTotal: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MacroItem(modifier = Modifier.weight(1f), name = "Carboidrati", consumed = carbsConsumed, total = carbsTotal, color = Color(0xFFFFB74D))
            Spacer(modifier = Modifier.width(16.dp))
            MacroItem(modifier = Modifier.weight(1f), name = "Proteine", consumed = proteinsConsumed, total = proteinsTotal, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(16.dp))
            MacroItem(modifier = Modifier.weight(1f), name = "Grassi", consumed = fatsConsumed, total = fatsTotal, color = Color(0xFFE57373))
        }
    }
}

@Composable
fun MacroItem(modifier: Modifier = Modifier, name: String, consumed: Int, total: Int, color: Color) {
    val progress = if (total > 0) (consumed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = name, 
            style = MaterialTheme.typography.labelMedium, 
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$consumed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " / $total g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

val MEAL_CATEGORIES = listOf(
    "Colazione", 
    "Spuntino Mattutino", 
    "Pranzo", 
    "Merenda", 
    "Cena", 
    "Spuntino Prenanna"
)

// FoodListSection removed in favor of MealCategoryCard

@Composable
fun AddFoodForm(
    initialFood: FoodItem? = null,
    category: String,
    onCancel: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    var foodName by remember { mutableStateOf(initialFood?.name ?: "") }
    var grams by remember { mutableStateOf(initialFood?.grams?.toString() ?: "100") }
    var unit by remember { mutableStateOf(initialFood?.unit ?: "g") }
    
    // Valori nutritivi per 100g (ottenuti dall'API)
    var baseCalories by remember { mutableStateOf(0.0) }
    var baseCarbs by remember { mutableStateOf(0.0) }
    var baseProteins by remember { mutableStateOf(0.0) }
    var baseFats by remember { mutableStateOf(0.0) }

    // Campi modificabili (calcolati in base ai grammi o inseriti a mano)
    var calories by remember { mutableStateOf(initialFood?.calories?.toString() ?: "") }
    var carbs by remember { mutableStateOf(initialFood?.carbs?.toString() ?: "") }
    var proteins by remember { mutableStateOf(initialFood?.proteins?.toString() ?: "") }
    var fats by remember { mutableStateOf(initialFood?.fats?.toString() ?: "") }

    // Ricerca API
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }
    // Evita la ricerca automatica all'apertura se stiamo modificando un pasto esistente
    var justSelectedFromDropdown by remember { mutableStateOf(initialFood != null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }

    // Aggiorna i valori quando cambiano i grammi o i valori base
    LaunchedEffect(grams, baseCalories, baseCarbs, baseProteins, baseFats) {
        val g = grams.toDoubleOrNull()
        if (g != null && baseCalories > 0) {
            val ratio = g / 100.0
            calories = String.format("%.0f", baseCalories * ratio)
            carbs = String.format("%.1f", baseCarbs * ratio).replace(",", ".")
            proteins = String.format("%.1f", baseProteins * ratio).replace(",", ".")
            fats = String.format("%.1f", baseFats * ratio).replace(",", ".")
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            OutlinedTextField(
                value = foodName,
                onValueChange = { 
                    foodName = it
                    showDropdown = false 
                },
                label = { Text("Cerca o inserisci nome alimento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                colors = textFieldColors,
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        if (foodName.isNotBlank()) {
                            isSearching = true
                            coroutineScope.launch {
                                try {
                                    val response = OpenFoodFactsClient.api.searchProducts(query = foodName)
                                    searchResults = response.products
                                    showDropdown = searchResults.isNotEmpty()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    }
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = Color.Black)
                        }
                        IconButton(onClick = {
                            if (foodName.isNotBlank()) {
                                isSearching = true
                                coroutineScope.launch {
                                    try {
                                        val response = OpenFoodFactsClient.api.searchProducts(query = foodName)
                                        searchResults = response.products
                                        showDropdown = searchResults.isNotEmpty()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Cerca", modifier = Modifier.size(24.dp), tint = Color.Black)
                        }
                        IconButton(onClick = {
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val rawValue = barcode.rawValue
                                    if (rawValue != null) {
                                        isSearching = true
                                        coroutineScope.launch {
                                            try {
                                                val response = OpenFoodFactsClient.api.getProductByBarcode(rawValue)
                                                if (response.status == 1 && response.product != null) {
                                                    val p = response.product
                                                    justSelectedFromDropdown = true
                                                    foodName = p.displayName
                                                    baseCalories = p.nutriments?.energyKcal100g ?: 0.0
                                                    baseCarbs = p.nutriments?.carbohydrates100g ?: 0.0
                                                    baseProteins = p.nutriments?.proteins100g ?: 0.0
                                                    baseFats = p.nutriments?.fat100g ?: 0.0
                                                } else {
                                                    Toast.makeText(context, "Prodotto non trovato", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Errore di connessione", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isSearching = false
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    // Scansione fallita o annullata dall'utente
                                }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scansiona codice a barre", modifier = Modifier.size(24.dp), tint = Color.Black)
                        }
                    }
                }
            )

            // Dropdown Risultati
            if (showDropdown) {
                Popup(
                    alignment = Alignment.TopStart,
                    properties = PopupProperties(focusable = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(top = 64.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        LazyColumn {
                            items(searchResults) { product ->
                                val nut = product.nutriments
                                val kcal = nut?.energyKcal100g ?: 0.0
                                val c = nut?.carbohydrates100g ?: 0.0
                                val p = nut?.proteins100g ?: 0.0
                                val f = nut?.fat100g ?: 0.0

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            justSelectedFromDropdown = true
                                            foodName = product.displayName
                                            baseCalories = kcal
                                            baseCarbs = c
                                            baseProteins = p
                                            baseFats = f
                                            showDropdown = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(product.displayName, fontWeight = FontWeight.Bold, color = Color.Black)
                                    val brand = product.brands?.takeIf { it.isNotBlank() } ?: "Sconosciuto"
                                    Text("$brand • ${kcal} kcal / 100g", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                }
                                HorizontalDivider(color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var expandedUnit by remember { mutableStateOf(false) }
            val units = listOf("g", "ml", "pz", "oz", "lb")

            OutlinedTextField(
                value = grams,
                onValueChange = { grams = it },
                label = { Text("Quantità") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
                trailingIcon = {
                    Box {
                        TextButton(onClick = { expandedUnit = true }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)) {
                            Text(unit)
                        }
                        DropdownMenu(
                            expanded = expandedUnit,
                            onDismissRequest = { expandedUnit = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u, color = Color.Black) },
                                    onClick = {
                                        unit = u
                                        expandedUnit = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calorie (kcal)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = carbs,
                onValueChange = { carbs = it },
                label = { Text("Carb. (g)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = proteins,
                onValueChange = { proteins = it },
                label = { Text("Prot. (g)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = fats,
                onValueChange = { fats = it },
                label = { Text("Grassi (g)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(
                onClick = onCancel, 
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
            ) {
                Text("Annulla", fontWeight = FontWeight.Bold)
            }
            Button(onClick = {
                val cal = calories.toDoubleOrNull()?.toInt() ?: 0
                val c = carbs.replace(",", ".").toDoubleOrNull() ?: 0.0
                val p = proteins.replace(",", ".").toDoubleOrNull() ?: 0.0
                val f = fats.replace(",", ".").toDoubleOrNull() ?: 0.0
                val g = grams.toIntOrNull() ?: 100
                
                val updatedFood = FoodItem(
                    id = initialFood?.id ?: UUID.randomUUID().toString(),
                    name = foodName.takeIf { it.isNotBlank() } ?: "Alimento",
                    category = category,
                    grams = g,
                    unit = unit,
                    calories = cal,
                    carbs = c,
                    proteins = p,
                    fats = f
                )
                onSave(updatedFood)
            }, 
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Salva", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DietScreenPreview() {
    GymLogFinaleTheme {
        // In preview we can't easily provide an Application to AndroidViewModel, 
        // so we just call it without explicit viewmodel if possible, or leave it.
        // Usually, viewModel() inside DietScreen will throw an error in Preview,
        // so for simplicity we just remove the preview content or comment it.
        // DietScreen()
    }
}
