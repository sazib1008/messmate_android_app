package com.messmate.android.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.messmate.android.domain.model.Deposit
import com.messmate.android.domain.model.DepositStatus
import com.messmate.android.domain.model.Expense
import com.messmate.android.domain.model.PaymentMethod
import com.messmate.android.domain.model.displayName
import com.messmate.android.ui.common.EmptyState
import com.messmate.android.ui.common.MessMateButton
import com.messmate.android.ui.theme.SageGreen
import com.messmate.android.ui.theme.Terracotta
import java.util.Locale

@Composable
fun WalletScreen(
    state: StudentUiState,
    onSubmitDeposit: (Double, PaymentMethod, String?, String?, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTopUpDialog by remember { mutableStateOf(false) }
    var isExpenseLedgerExpanded by remember { mutableStateOf(false) }

    val mySummary = state.mySettlement
    val netBalance = mySummary?.netBalance ?: 0.0
    val totalDeposits = mySummary?.totalDeposits ?: state.deposits.filter { it.status == DepositStatus.APPROVED }.sumOf { it.amount }
    val totalCost = mySummary?.totalCost ?: 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Hero Net Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (netBalance >= 0) SageGreen else Terracotta
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = if (netBalance >= 0) "Surplus" else "Due",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "৳ ${String.format(Locale.US, "%.2f", netBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Top-up Button
                    Button(
                        onClick = { showTopUpDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = if (netBalance >= 0) SageGreen else Terracotta
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Top Up Deposit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Cycle & Cost Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cycle Settlement Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        state.activeCycle?.let { cycle ->
                            Text(
                                text = "Cycle #${cycle.cycleNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Terracotta,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    state.activeCycle?.let { cycle ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Day ${cycle.countedActiveDays} of ${cycle.targetActiveDays} • Ends ${cycle.scheduledEndDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    BreakdownRow(label = "Total Deposits", value = "+৳ ${String.format(Locale.US, "%.2f", totalDeposits)}", isPositive = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    BreakdownRow(label = "Meal Cost (${String.format(Locale.US, "%.1f", mySummary?.totalMemberUnits ?: 0.0)} units)", value = "-৳ ${String.format(Locale.US, "%.2f", mySummary?.mealCost ?: 0.0)}")
                    Spacer(modifier = Modifier.height(6.dp))
                    BreakdownRow(label = "Fixed Overhead Share", value = "-৳ ${String.format(Locale.US, "%.2f", mySummary?.fixedOverheadCost ?: 0.0)}")
                    Spacer(modifier = Modifier.height(6.dp))
                    BreakdownRow(label = "Individual Direct Costs", value = "-৳ ${String.format(Locale.US, "%.2f", mySummary?.individualDirectCost ?: 0.0)}")
                    Spacer(modifier = Modifier.height(6.dp))
                    BreakdownRow(label = "Ad-Hoc Special Costs", value = "-৳ ${String.format(Locale.US, "%.2f", mySummary?.adHocSpecialCost ?: 0.0)}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Charged", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "৳ ${String.format(Locale.US, "%.2f", totalCost)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Deposit History Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deposit History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.deposits.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.deposits.isEmpty()) {
            item {
                EmptyState(
                    title = "No deposits recorded",
                    description = "Submit your first deposit top-up above. The manager will review and approve it.",
                    icon = Icons.Rounded.AccountBalanceWallet
                )
            }
        } else {
            items(state.deposits, key = { it.id }) { deposit ->
                DepositItemCard(deposit = deposit)
            }
        }

        // Collapsible Expense Ledger Section (Strictly Read-Only)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpenseLedgerExpanded = !isExpenseLedgerExpanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = Terracotta)
                            Column {
                                Text(
                                    text = "Mess Expense Ledger (Read-Only)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.expenses.size} expenses recorded by manager",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpenseLedgerExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpenseLedgerExpanded) "Collapse" else "Expand"
                        )
                    }

                    AnimatedVisibility(visible = isExpenseLedgerExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                            if (state.expenses.isEmpty()) {
                                Text(
                                    text = "No expenses recorded for this mess cycle yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.expenses.forEach { expense ->
                                    ExpenseItemRow(expense = expense)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Top-up Deposit Dialog
    if (showTopUpDialog) {
        TopUpDepositDialog(
            isSubmitting = state.isSubmittingDeposit,
            onDismiss = { showTopUpDialog = false },
            onSubmit = { amount, method, ref, notes ->
                onSubmitDeposit(amount, method, ref, notes) { success ->
                    if (success) {
                        showTopUpDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun BreakdownRow(label: String, value: String, isPositive: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isPositive) SageGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DepositItemCard(deposit: Deposit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "৳ ${String.format(Locale.US, "%.2f", deposit.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DepositStatusBadge(status = deposit.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${deposit.paymentMethod.displayName} • ${deposit.depositDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!deposit.transactionRef.isNullOrBlank()) {
                    Text(
                        text = "Ref: ${deposit.transactionRef}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun DepositStatusBadge(status: DepositStatus) {
    val (bgColor, textColor, text) = when (status) {
        DepositStatus.APPROVED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Approved")
        DepositStatus.PENDING -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Pending")
        DepositStatus.REJECTED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Rejected")
    }

    Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ExpenseItemRow(expense: Expense) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${expense.category.displayName} • ${expense.expenseDate.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "৳ ${String.format(Locale.US, "%.2f", expense.amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TopUpDepositDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Double, PaymentMethod, String?, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("1000") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.BKASH) }
    var transactionRef by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isMethodDropdownOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Top Up Deposit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Submit deposit details for manager approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { isMethodDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Payment Method", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = selectedMethod.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = isMethodDropdownOpen,
                        onDismissRequest = { isMethodDropdownOpen = false }
                    ) {
                        PaymentMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayName) },
                                onClick = {
                                    selectedMethod = method
                                    isMethodDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transaction Ref
                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("Transaction Reference (optional)") },
                    placeholder = { Text("e.g. TrxID / Receipt #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    MessMateButton(
                        text = "Submit",
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                onSubmit(
                                    amount,
                                    selectedMethod,
                                    transactionRef.takeIf { it.isNotBlank() },
                                    notes.takeIf { it.isNotBlank() }
                                )
                            }
                        },
                        isLoading = isSubmitting,
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
