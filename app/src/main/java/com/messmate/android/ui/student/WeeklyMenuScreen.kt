package com.messmate.android.ui.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messmate.android.domain.model.MealSession
import com.messmate.android.domain.model.displayName
import com.messmate.android.domain.model.getActiveSessionList
import com.messmate.android.ui.common.EmptyState
import com.messmate.android.ui.common.LoadingScreen
import com.messmate.android.ui.theme.Terracotta

@Composable
fun WeeklyMenuScreen(
    state: StudentUiState,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoadingMenu) {
        LoadingScreen(message = "Loading weekly menu...", modifier = modifier)
        return
    }

    val daysOfWeek = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    val menuItems = state.weeklyMenu?.items?.filter { it.dayOfWeek == state.selectedDayOfWeek } ?: emptyList()

    val activeSessions = state.weeklyMenu?.activeSessions?.takeIf { it.isNotEmpty() }
        ?: state.diningConfig?.getActiveSessionList()
        ?: listOf(MealSession.BREAKFAST, MealSession.LUNCH, MealSession.DINNER)

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 7-day Tab Row
        ScrollableTabRow(
            selectedTabIndex = daysOfWeek.indexOfFirst { it.first == state.selectedDayOfWeek }.coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Terracotta
        ) {
            daysOfWeek.forEach { (dayNum, dayName) ->
                val isSelected = state.selectedDayOfWeek == dayNum
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Day Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = null, tint = Terracotta)
                        Column {
                            Text(
                                text = "${daysOfWeek.firstOrNull { it.first == state.selectedDayOfWeek }?.second ?: "Selected Day"} Menu Schedule",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            state.weeklyMenu?.title?.let { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (menuItems.isEmpty()) {
                item {
                    EmptyState(
                        title = "No dishes scheduled",
                        description = "The mess manager or chef has not assigned any dishes for this day yet.",
                        icon = Icons.Rounded.RestaurantMenu
                    )
                }
            } else {
                // Group by active session
                activeSessions.forEach { session ->
                    val sessionDishes = menuItems.filter { it.session == session }
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${sessionDishes.size} item(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (sessionDishes.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No items for ${session.displayName.lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                sessionDishes.forEach { dish ->
                                    DishItemCard(dish = dish)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
