package com.messmate.android.ui.manager

import androidx.compose.foundation.background
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
import com.messmate.android.domain.model.Deposit
import com.messmate.android.domain.model.DepositStatus
import com.messmate.android.domain.model.PaymentMethod
import com.messmate.android.domain.model.displayNameWithRoom
import com.messmate.android.ui.common.EmptyState
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.SageGreen
import com.messmate.android.ui.theme.Terracotta
import com.messmate.android.ui.theme.WarningAmber
import com.messmate.android.util.DhakaDateUtils

@Composable
fun ManagerDepositsScreen(
    state: ManagerUiState,
    onTabSelected: (Int) -> Unit,
    onApproveDeposit: (String) -> Unit,
    onRejectDeposit: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingDeposits = remember(state.deposits) {
        state.deposits.filter { it.status == DepositStatus.PENDING }
    }
    val historyDeposits = remember(state.deposits) {
        state.deposits.filter { it.status != DepositStatus.PENDING }
    }

    val approvedTotal = remember(state.deposits) {
        state.deposits.filter { it.status == DepositStatus.APPROVED }.sumOf { it.amount }
    }
    val pendingTotal = remember(pendingDeposits) {
        pendingDeposits.sumOf { it.amount }
    }
    val rejectedTotal = remember(state.deposits) {
        state.deposits.filter { it.status == DepositStatus.REJECTED }.sumOf { it.amount }
    }

    var historySearchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf<PaymentMethod?>(null) }

    // Dialog state for Rejection
    var rejectingDeposit by remember { mutableStateOf<Deposit?>(null) }
    var viewingReceiptDeposit by remember { mutableStateOf<Deposit?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // 1. Top Summary Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = "Verified",
                amount = approvedTotal,
                count = state.deposits.count { it.status == DepositStatus.APPROVED },
                color = SageGreen,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Pending",
                amount = pendingTotal,
                count = pendingDeposits.size,
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Rejected",
                amount = rejectedTotal,
                count = state.deposits.count { it.status == DepositStatus.REJECTED },
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Tab Navigation (Requests vs History)
        TabRow(
            selectedTabIndex = state.selectedDepositTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Terracotta
        ) {
            Tab(
                selected = state.selectedDepositTab == 0,
                onClick = { onTabSelected(0) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Requests",
                            fontWeight = if (state.selectedDepositTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                        if (pendingDeposits.isNotEmpty()) {
                            Badge(containerColor = Terracotta) {
                                Text("${pendingDeposits.size}", color = Color.White)
                            }
                        }
                    }
                }
            )
            Tab(
                selected = state.selectedDepositTab == 1,
                onClick = { onTabSelected(1) },
                text = {
                    Text(
                        text = "History (${historyDeposits.size})",
                        fontWeight = if (state.selectedDepositTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        // 3. Tab Contents
        if (state.selectedDepositTab == 0) {
            // === REQUESTS TAB ===
            if (pendingDeposits.isEmpty()) {
                EmptyState(
                    title = "No pending requests — all caught up",
                    description = "When students submit bank or mobile wallet deposits, they will appear here for verification.",
                    icon = Icons.Rounded.CheckCircleOutline
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(pendingDeposits, key = { it.id }) { deposit ->
                        // Scoped loading state: strictly checks if this deposit id is in loading set
                        val isActionLoading = state.depositActionLoadingIds.contains(deposit.id)
                        PendingDepositCard(
                            deposit = deposit,
                            isLoading = isActionLoading,
                            onApprove = { onApproveDeposit(deposit.id) },
                            onReject = { rejectingDeposit = deposit }
                        )
                    }
                }
            }
        } else {
            // === HISTORY TAB ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = historySearchQuery,
                    onValueChange = { historySearchQuery = it },
                    placeholder = { Text("Search by student, room, or Trx ID...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (historySearchQuery.isNotBlank()) {
                            IconButton(onClick = { historySearchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Method filter chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMethodFilter == null,
                            onClick = { selectedMethodFilter = null },
                            label = { Text("All Methods") }
                        )
                    }
                    items(PaymentMethod.values()) { method ->
                        FilterChip(
                            selected = selectedMethodFilter == method,
                            onClick = {
                                selectedMethodFilter = if (selectedMethodFilter == method) null else method
                            },
                            label = { Text(method.name) }
                        )
                    }
                }

                val filteredHistory = remember(historyDeposits, historySearchQuery, selectedMethodFilter) {
                    historyDeposits.filter { dep ->
                        val matchesSearch = historySearchQuery.isBlank() ||
                            dep.userFullName.contains(historySearchQuery, ignoreCase = true) ||
                            dep.roomNumber?.contains(historySearchQuery, ignoreCase = true) == true ||
                            dep.transactionRef?.contains(historySearchQuery, ignoreCase = true) == true
                        val matchesMethod = selectedMethodFilter == null || dep.paymentMethod == selectedMethodFilter
                        matchesSearch && matchesMethod
                    }
                }

                if (filteredHistory.isEmpty()) {
                    EmptyState(
                        title = "No deposits match your search",
                        description = "Try changing your search terms or filter.",
                        icon = Icons.Rounded.SearchOff
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredHistory, key = { it.id }) { deposit ->
                            HistoryDepositRow(
                                deposit = deposit,
                                onClick = { viewingReceiptDeposit = deposit }
                            )
                        }
                    }
                }
            }
        }
    }

    // Rejection Dialog with reason
    if (rejectingDeposit != null) {
        val dep = rejectingDeposit!!
        var reasonText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectingDeposit = null },
            icon = { Icon(Icons.Rounded.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reject Deposit?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to reject ৳${"%,.2f".format(dep.amount)} claimed by ${dep.displayNameWithRoom}?"
                    )
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for rejection (optional)") },
                        placeholder = { Text("e.g. Transaction ID not found in bank statement") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectDeposit(dep.id, reasonText.takeIf { it.isNotBlank() })
                        rejectingDeposit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingDeposit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Read-only Receipt Dialog (Immutable history)
    if (viewingReceiptDeposit != null) {
        val dep = viewingReceiptDeposit!!
        AlertDialog(
            onDismissRequest = { viewingReceiptDeposit = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deposit Receipt", fontWeight = FontWeight.Bold)
                    StatusBadge(
                        text = dep.status.name,
                        color = when (dep.status) {
                            DepositStatus.APPROVED -> SageGreen
                            DepositStatus.REJECTED -> MaterialTheme.colorScheme.error
                            DepositStatus.PENDING -> WarningAmber
                        }
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("৳${"%,.2f".format(dep.amount)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Terracotta)
                        }
                    }

                    ReceiptField("Student", dep.displayNameWithRoom)
                    ReceiptField("Payment Method", dep.paymentMethod.name)
                    ReceiptField("Transaction Reference", dep.transactionRef ?: "None")
                    ReceiptField("Submitted Time", DhakaDateUtils.formatDhakaDateHeader(dep.createdAt))
                    if (!dep.notes.isNullOrBlank()) {
                        ReceiptField("Notes / Reason", dep.notes)
                    }

                    Text(
                        text = "Deposits are permanent and immutable once reviewed.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewingReceiptDeposit = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = "৳${"%,.0f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun PendingDepositCard(
    deposit: Deposit,
    isLoading: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deposit.displayNameWithRoom,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Submitted: ${DhakaDateUtils.formatDhakaDateHeader(deposit.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "৳${"%,.2f".format(deposit.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Terracotta
                )
            }

            // Payment Details
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(text = deposit.paymentMethod.name, color = Terracotta)
                    }
                    Text(
                        text = "Trx: ${deposit.transactionRef ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!deposit.notes.isNullOrBlank()) {
                Text(
                    text = "Note: ${deposit.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons: Approve / Reject with Scoped Loading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }

                Button(
                    onClick = onApprove,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDepositRow(
    deposit: Deposit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deposit.displayNameWithRoom,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = deposit.status.name,
                        color = if (deposit.status == DepositStatus.APPROVED) SageGreen else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "• ${deposit.paymentMethod.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    deposit.transactionRef?.let {
                        Text(
                            text = "• $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳${"%,.2f".format(deposit.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (deposit.status == DepositStatus.APPROVED) SageGreen else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DhakaDateUtils.formatDhakaDateHeader(deposit.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReceiptField(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
