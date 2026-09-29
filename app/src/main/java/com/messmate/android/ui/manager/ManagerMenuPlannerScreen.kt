package com.messmate.android.ui.manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messmate.android.domain.model.MealSession
import com.messmate.android.domain.model.MenuItem
import com.messmate.android.domain.model.displayName
import com.messmate.android.domain.model.getActiveSessionList
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.Terracotta

@Composable
fun ManagerMenuPlannerScreen(
    state: ManagerUiState,
    onSelectDay: (Int) -> Unit,
    onCreateDish: (
        dayOfWeek: Int,
        session: MealSession,
        itemName: String,
        description: String?,
        category: String?,
        dietaryTags: List<String>,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteDish: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    // Active session filtering (same logic as Phase 1 Student Weekly Menu screen)
    val activeSessions = remember(state.weeklyMenu, state.diningConfig) {
        state.weeklyMenu?.activeSessions?.takeIf { it.isNotEmpty() }
            ?: state.diningConfig?.getActiveSessionList()
            ?: listOf(MealSession.BREAKFAST, MealSession.LUNCH, MealSession.DINNER)
    }

    val dayItems = remember(state.weeklyMenu, state.selectedMenuDay) {
        state.weeklyMenu?.items?.filter { it.dayOfWeek == state.selectedMenuDay } ?: emptyList()
    }

    var addingSlotSession by remember { mutableStateOf<MealSession?>(null) }
    var dishToDelete by remember { mutableStateOf<MenuItem?>(null) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 7-day scrollable tab row
        ScrollableTabRow(
            selectedTabIndex = daysOfWeek.indexOfFirst { it.first == state.selectedMenuDay }.coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Terracotta
        ) {
            daysOfWeek.forEach { (dayNum, dayName) ->
                val isSelected = state.selectedMenuDay == dayNum
                Tab(
                    selected = isSelected,
                    onClick = { onSelectDay(dayNum) },
                    text = {
                        Text(
                            text = dayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Active Sessions List for the selected day
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(activeSessions) { session ->
                val dishesForSession = dayItems.filter { it.session == session }
                SessionMenuSection(
                    session = session,
                    dishes = dishesForSession,
                    deletingDishIds = state.deletingDishIds,
                    onAddDish = { addingSlotSession = session },
                    onDeleteDish = { dishToDelete = it }
                )
            }
        }
    }

    // Add Dish Dialog (Minimal by default, focused on dish name with quick-pick chips)
    if (addingSlotSession != null) {
        val session = addingSlotSession!!
        AddDishDialog(
            dayName = daysOfWeek.firstOrNull { it.first == state.selectedMenuDay }?.second ?: "Day",
            session = session,
            isLoading = state.isAddingDish,
            onDismiss = { addingSlotSession = null },
            onSave = { name, desc, cat, tags ->
                onCreateDish(state.selectedMenuDay, session, name, desc, cat, tags) {
                    addingSlotSession = null
                }
            }
        )
    }

    // Delete Dish Confirmation Dialog
    if (dishToDelete != null) {
        val dish = dishToDelete!!
        AlertDialog(
            onDismissRequest = { dishToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Dish?") },
            text = { Text("Are you sure you want to remove '${dish.itemName}' from ${dish.session.displayName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDish(dish.id)
                        dishToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { dishToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionMenuSection(
    session: MealSession,
    dishes: List<MenuItem>,
    deletingDishIds: Set<String>,
    onAddDish: () -> Unit,
    onDeleteDish: (MenuItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (session) {
                            MealSession.BREAKFAST -> Icons.Rounded.FreeBreakfast
                            MealSession.LUNCH -> Icons.Rounded.WbSunny
                            MealSession.DINNER -> Icons.Rounded.NightsStay
                        },
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = session.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onAddDish, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircle,
                        contentDescription = "Add Dish",
                        tint = Terracotta
                    )
                }
            }

            if (dishes.isEmpty()) {
                // Empty state per day/session slot
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No dish scheduled yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onAddDish) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Dish", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dishes.forEach { dish ->
                        val isDeleting = deletingDishIds.contains(dish.id)
                        DishItemRow(
                            dish = dish,
                            isDeleting = isDeleting,
                            onDelete = { onDeleteDish(dish) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DishItemRow(
    dish: MenuItem,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dish.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!dish.description.isNullOrBlank()) {
                    Text(
                        text = dish.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dish.category != null || dish.dietaryTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dish.category?.let { cat ->
                            StatusBadge(text = cat, color = Terracotta)
                        }
                        dish.dietaryTags.forEach { tag ->
                            StatusBadge(text = tag, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                enabled = !isDeleting,
                modifier = Modifier.size(32.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete Dish",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddDishDialog(
    dayName: String,
    session: MealSession,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, category: String?, tags: List<String>) -> Unit
) {
    var dishName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("MAIN") }
    var tagsString by remember { mutableStateOf("") }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Quick pick chips for Bangladeshi mess cuisine
    val quickPicks = listOf(
        "Rice", "Dal", "Chicken Curry", "Fish Curry", "Beef Curry",
        "Egg Bhuna", "Mixed Vegetable", "Salad", "Khichuri", "Paratha"
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text("Add Dish to $dayName", fontWeight = FontWeight.Bold)
                Text(
                    text = session.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Terracotta
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary focused dish name field
                OutlinedTextField(
                    value = dishName,
                    onValueChange = {
                        dishName = it
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("Dish Name *") },
                    isError = hasError,
                    supportingText = if (hasError) { { Text("Dish name is required") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick-pick suggestion chips
                Text("Quick suggestions:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickPicks) { pick ->
                        SuggestionChip(
                            onClick = {
                                dishName = if (dishName.isBlank()) pick else "$dishName, $pick"
                                hasError = false
                            },
                            label = { Text(pick) }
                        )
                    }
                }

                // Collapsed Advanced Options (Course/Category & Dietary Tags)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advanced options",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = isAdvancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (e.g. Spicy gravy)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category (e.g. MAIN, SIDE, DESSERT)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tagsString,
                            onValueChange = { tagsString = it },
                            label = { Text("Dietary tags (comma separated, e.g. HALAL, VEG)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dishName.trim().isBlank()) {
                        hasError = true
                    } else {
                        val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onSave(
                            dishName.trim(),
                            description.trim().takeIf { it.isNotBlank() },
                            category.trim().takeIf { it.isNotBlank() },
                            tags
                        )
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Add Dish")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
