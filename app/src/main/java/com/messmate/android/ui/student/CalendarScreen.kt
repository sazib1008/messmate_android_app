package com.messmate.android.ui.student

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messmate.android.domain.model.MealSession
import com.messmate.android.domain.model.MealStatus
import com.messmate.android.ui.common.LoadingScreen
import com.messmate.android.ui.theme.Terracotta

@Composable
fun CalendarScreen(
    state: StudentUiState,
    onToggleMeal: (String, MealSession, MealStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoadingCalendar) {
        LoadingScreen(message = "Loading meal calendar...", modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Meal Schedule (14-Day Window)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Past dates are locked. Future dates are editable until cutoff time (Asia/Dhaka).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(state.calendarDays, key = { it.date }) { day ->
            CalendarDayCard(
                day = day,
                onToggleSession = { session, newStatus ->
                    onToggleMeal(day.date, session, newStatus)
                }
            )
        }
    }
}

@Composable
fun CalendarDayCard(
    day: CalendarDayState,
    onToggleSession: (MealSession, MealStatus) -> Unit
) {
    val borderColor = if (day.isToday) Terracotta else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                day.isToday -> MaterialTheme.colorScheme.surface
                day.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (day.isToday) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (day.isToday) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                Text(
                    text = day.dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (day.isToday) Terracotta else MaterialTheme.colorScheme.onSurface
                )
                if (day.isToday) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Terracotta.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else if (day.isPast) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Past date",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Session chips / toggles
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                day.sessions.forEach { sessionState ->
                    SessionPill(
                        sessionState = sessionState,
                        onToggle = { newStatus ->
                            if (sessionState.isEditable) {
                                onToggleSession(sessionState.session, newStatus)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionPill(
    sessionState: DaySessionState,
    onToggle: (MealStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = sessionState.status == MealStatus.ON
    val isEditable = sessionState.isEditable

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isOn && isEditable -> Terracotta.copy(alpha = 0.12f)
            isOn && !isEditable -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        },
        border = if (isOn && isEditable) androidx.compose.foundation.BorderStroke(1.dp, Terracotta.copy(alpha = 0.5f)) else null,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = isEditable && !sessionState.isToggling) {
                onToggle(if (isOn) MealStatus.OFF else MealStatus.ON)
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = when (sessionState.session) {
                    MealSession.BREAKFAST -> "B'fast"
                    MealSession.LUNCH -> "Lunch"
                    MealSession.DINNER -> "Dinner"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isOn && isEditable -> Terracotta
                    isOn && !isEditable -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (sessionState.isToggling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Terracotta
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!isEditable) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = if (isOn) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOn && isEditable -> Terracotta
                            isOn && !isEditable -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.outline
                        },
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
