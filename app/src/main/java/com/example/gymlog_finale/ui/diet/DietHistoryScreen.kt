package com.example.gymlog_finale.ui.diet

// Schermata di storico della dieta con selezione settimana/giorno.

import com.example.gymlog_finale.data.model.DailyDietStats
import com.example.gymlog_finale.data.model.FoodItem

import com.example.gymlog_finale.ui.diet.components.MealCategoryInfo
import com.example.gymlog_finale.ui.diet.components.mealCategoryDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietHistoryScreen(
    viewModel: DietHistoryViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyStats by viewModel.selectedDayStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storico Alimentare", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Torna indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(imageVector = Icons.Default.EditCalendar, contentDescription = "Modifica Obiettivi")
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
        ) {

            MonthCalendar(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                dailyStats?.let { stats ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN)
                            Text(
                                text = "Riepilogo del ${sdf.format(selectedDate.time)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            SmallCaloriesCard(consumed = stats.consumedCalories, total = stats.totalCalories)
                        }

                        item {
                            SmallMacrosSection(
                                carbsConsumed = stats.consumedCarbs.toInt(), carbsTotal = stats.totalCarbs.toInt(),
                                proteinsConsumed = stats.consumedProteins.toInt(), proteinsTotal = stats.totalProteins.toInt(),
                                fatsConsumed = stats.consumedFats.toInt(), fatsTotal = stats.totalFats.toInt()
                            )
                        }

                        val groupedFoods = stats.foods.groupBy { it.category }

                        MEAL_CATEGORIES.forEach { category ->
                            item {
                                HistoryMealCategoryCard(
                                    categoryName = category,
                                    foods = groupedFoods[category] ?: emptyList()
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        val currentStats = dailyStats ?: DailyDietStats()
        AdvancedEditGoalsDialog(
            initialDate = selectedDate,
            initialStats = currentStats,
            onDismiss = { showEditDialog = false },
            onSave = { start, end, cal, carbs, pro, fats ->
                viewModel.updateGoalsForDateRange(start, end, cal, carbs, pro, fats)
                showEditDialog = false
            }
        )
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun HistoryMealCategoryCard(
    categoryName: String,
    foods: List<FoodItem>
) {
    var expanded by remember { mutableStateOf(false) }
    val details = mealCategoryDetails[categoryName] ?: MealCategoryInfo(categoryName, Icons.Outlined.Restaurant, Color.Gray)
    val totalCalories = foods.sumOf { it.calories }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = details.color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(details.color)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(details.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = details.icon,
                            contentDescription = categoryName,
                            tint = details.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${foods.size} aliment${if(foods.size == 1) "o" else "i"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "$totalCalories kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Comprimi" else "Espandi",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    if (foods.isEmpty()) {
                        Text(
                            text = "Nessun alimento aggiunto.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        foods.forEach { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(food.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${food.grams}${food.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${food.calories} kcal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "C:${food.carbs} P:${food.proteins} G:${food.fats}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (food != foods.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
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
fun MonthCalendar(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    var currentMonthCalendar by remember { mutableStateOf(selectedDate.clone() as Calendar) }

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
    val daysOfWeek = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newCal = currentMonthCalendar.clone() as Calendar
                newCal.add(Calendar.MONTH, -1)
                currentMonthCalendar = newCal
            }) {
                Icon(Icons.Default.ChevronLeft, "Mese precedente")
            }

            Text(
                text = sdfMonthYear.format(currentMonthCalendar.time).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                val newCal = currentMonthCalendar.clone() as Calendar
                newCal.add(Calendar.MONTH, 1)
                currentMonthCalendar = newCal
            }) {
                Icon(Icons.Default.ChevronRight, "Mese successivo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val daysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = currentMonthCalendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)

        var firstDayIndex = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayIndex < 0) firstDayIndex = 6

        var currentDay = 1

        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (col in 0 until 7) {
                    val isWithinMonth = (row == 0 && col >= firstDayIndex) || (row > 0 && currentDay <= daysInMonth)

                    if (isWithinMonth && currentDay <= daysInMonth) {
                        val currentYear = currentMonthCalendar.get(Calendar.YEAR)
                        val currentMonth = currentMonthCalendar.get(Calendar.MONTH)

                        val isSelected = currentYear == selectedDate.get(Calendar.YEAR) &&
                                         currentMonth == selectedDate.get(Calendar.MONTH) &&
                                         currentDay == selectedDate.get(Calendar.DAY_OF_MONTH)

                        val isToday = currentYear == todayYear && currentMonth == todayMonth && currentDay == todayDay
                        val isPast = currentYear < todayYear ||
                                     (currentYear == todayYear && currentMonth < todayMonth) ||
                                     (currentYear == todayYear && currentMonth == todayMonth && currentDay < todayDay)

                        val dayToSelect = currentDay

                        val bgColor = if (isSelected) {
                            Color.Black
                        } else if (isToday) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        } else if (isPast) {
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        }

                        val txtColor = if (isSelected) {
                            Color.White
                        } else if (isToday) {
                            MaterialTheme.colorScheme.tertiary
                        } else if (isPast) {
                            Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable {
                                        val newDate = currentMonthCalendar.clone() as Calendar
                                        newDate.set(Calendar.DAY_OF_MONTH, dayToSelect)
                                        onDateSelected(newDate)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentDay.toString(),
                                    color = txtColor,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        currentDay++
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(32.dp).padding(2.dp))
                    }
                }
            }
            if (currentDay > daysInMonth) break
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun SmallCaloriesCard(consumed: Int, total: Int) {
    val progress = if (total > 0) (consumed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / $total kcal",
                        style = MaterialTheme.typography.bodyMedium,
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
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun SmallMacrosSection(
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
            SmallMacroItem(modifier = Modifier.weight(1f), name = "Carboidrati", consumed = carbsConsumed, total = carbsTotal, color = Color(0xFFFFB74D))
            Spacer(modifier = Modifier.width(16.dp))
            SmallMacroItem(modifier = Modifier.weight(1f), name = "Proteine", consumed = proteinsConsumed, total = proteinsTotal, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(16.dp))
            SmallMacroItem(modifier = Modifier.weight(1f), name = "Grassi", consumed = fatsConsumed, total = fatsTotal, color = Color(0xFFE57373))
        }
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@Composable
fun SmallMacroItem(modifier: Modifier = Modifier, name: String, consumed: Int, total: Int, color: Color) {
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " / $total g",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
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
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

// Composable che disegna una porzione della UI e ne gestisce lo stato locale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedEditGoalsDialog(
    initialDate: Calendar,
    initialStats: DailyDietStats,
    onDismiss: () -> Unit,
    onSave: (Calendar, Calendar, Int, Double, Double, Double) -> Unit
) {
    var cal by remember { mutableStateOf(initialStats.totalCalories.toString()) }
    var carbs by remember { mutableStateOf(initialStats.totalCarbs.toInt().toString()) }
    var pro by remember { mutableStateOf(initialStats.totalProteins.toInt().toString()) }
    var fats by remember { mutableStateOf(initialStats.totalFats.toInt().toString()) }

    var selectedMode by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var startDate by remember { mutableStateOf(initialDate.clone() as Calendar) }
    var endDate by remember { mutableStateOf(initialDate.clone() as Calendar) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Modifica Obiettivi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = cal, onValueChange = { cal = it },
                        colors = textFieldColors,
                    label = { Text("Calorie") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbs, onValueChange = { carbs = it },
                        colors = textFieldColors,
                        label = { Text("Carbo") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = pro, onValueChange = { pro = it },
                        colors = textFieldColors,
                        label = { Text("Pro") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = fats, onValueChange = { fats = it },
                        colors = textFieldColors,
                        label = { Text("Grassi") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Applica a:", fontWeight = FontWeight.SemiBold, color = Color.Black)

                val modes = listOf("Solo questo giorno", "Questa settimana", "Questo mese", "Personalizzato")
                modes.forEachIndexed { index, title ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = index }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedMode == index,
                            onClick = { selectedMode = index },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.Black)
                        )
                        Text(title)
                    }
                }

                if (selectedMode == 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dal: ${sdf.format(startDate.time)}")
                        Button(onClick = {
                            showStartDatePicker = true
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) { Text("Scegli", fontWeight = FontWeight.Bold) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Al: ${sdf.format(endDate.time)}")
                        Button(onClick = {
                            showEndDatePicker = true
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) { Text("Scegli", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.timeInMillis)
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    colors = DatePickerDefaults.colors(containerColor = Color.White),
                    tonalElevation = 0.dp,
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showStartDatePicker = false
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val c = Calendar.getInstance()
                                    c.timeInMillis = millis
                                    startDate = c
                                }
                            }
                        ) { Text("Conferma", color = Color.Black) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("Annulla", color = Color.Black) }
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
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.timeInMillis)
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    colors = DatePickerDefaults.colors(containerColor = Color.White),
                    tonalElevation = 0.dp,
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showEndDatePicker = false
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val c = Calendar.getInstance()
                                    c.timeInMillis = millis
                                    endDate = c
                                }
                            }
                        ) { Text("Conferma", color = Color.Black) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("Annulla", color = Color.Black) }
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
        },
        confirmButton = {
            Button(onClick = {
                val c = cal.toIntOrNull() ?: 2000
                val cb = carbs.toDoubleOrNull() ?: 250.0
                val p = pro.toDoubleOrNull() ?: 150.0
                val f = fats.toDoubleOrNull() ?: 70.0

                val start = Calendar.getInstance()
                val end = Calendar.getInstance()

                when(selectedMode) {
                    0 -> {
                        start.time = initialDate.time
                        end.time = initialDate.time
                    }
                    1 -> {
                        start.time = initialDate.time
                        start.firstDayOfWeek = Calendar.MONDAY
                        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        end.time = start.time
                        end.add(Calendar.DAY_OF_MONTH, 6)
                    }
                    2 -> {
                        start.time = initialDate.time
                        start.set(Calendar.DAY_OF_MONTH, 1)
                        end.time = start.time
                        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    3 -> {
                        start.time = startDate.time
                        end.time = endDate.time
                        if (end.before(start)) {
                            end.time = start.time
                        }
                    }
                }

                onSave(start, end, c, cb, p, f)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Salva", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    )
}
