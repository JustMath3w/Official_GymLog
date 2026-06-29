package com.example.gymlog_finale.ui.diet.components

import com.example.gymlog_finale.data.model.FoodItem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MealCategoryInfo(val name: String, val icon: ImageVector, val color: Color)

val mealCategoryDetails = mapOf(
    "Colazione" to MealCategoryInfo("Colazione", Icons.Outlined.LocalCafe, Color(0xFFFFB74D)),
    "Spuntino Mattutino" to MealCategoryInfo("Spuntino Mattutino", Icons.Outlined.Eco, Color(0xFF81C784)),
    "Pranzo" to MealCategoryInfo("Pranzo", Icons.Outlined.Restaurant, Color(0xFF64B5F6)),
    "Merenda" to MealCategoryInfo("Merenda", Icons.Outlined.BakeryDining, Color(0xFFBA68C8)),
    "Cena" to MealCategoryInfo("Cena", Icons.Outlined.DinnerDining, Color(0xFFE57373)),
    "Spuntino Prenanna" to MealCategoryInfo("Spuntino Prenanna", Icons.Outlined.Nightlight, Color(0xFF90A4AE))
)

@Composable
fun MealCategoryCard(
    categoryName: String,
    foods: List<FoodItem>,
    currentDayIndex: Int = 0,
    onAddClick: (() -> Unit)? = null,
    onEditClick: ((FoodItem) -> Unit)? = null,
    onDeleteClick: ((FoodItem) -> Unit)? = null
) {
    var expanded by remember(currentDayIndex) { mutableStateOf(false) }
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
                // Barra laterale
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

            // Content
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
                                if (onEditClick != null && onDeleteClick != null) {
                                    Row {
                                        IconButton(onClick = { onEditClick(food) }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifica", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { onDeleteClick(food) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            if (food != foods.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                    if (onAddClick != null) {
                        TextButton(
                            onClick = onAddClick,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aggiungi Alimento")
                        }
                    }
                }
            }
        }
    }
}
