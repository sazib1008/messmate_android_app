package com.messmate.android.ui.manager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.messmate.android.domain.model.Expense
import com.messmate.android.domain.model.ExpenseCategory
import com.messmate.android.domain.model.MemberInfo
import com.messmate.android.ui.common.EmptyState
import com.messmate.android.ui.common.StatusBadge
import com.messmate.android.ui.theme.Terracotta
import com.messmate.android.ui.theme.WarningAmber
import com.messmate.android.util.DhakaDateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerExpensesScreen(
    state: ManagerUiState,
    onCategoryFilterChange: (ExpenseCategory?) -> Unit,
    onCreateExpense: (
        category: ExpenseCategory,
        title: String,
        amount: Double,
        date: String,
        receiptUrl: String?,
        notes: String?,
        targetMemberId: String?,
        participantIds: List<String>,
        onSuccess: () -> Unit
    ) -> Unit,
    onUpdateExpense: (
        id: String,
        category: ExpenseCategory,
        title: String,
        amount: Double,
        date: String,
        receiptUrl: String?,
        notes: String?,
        targetMemberId: String?,
        participantIds: List<String>,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val filteredExpenses = remember(state.expenses, state.selectedExpenseCategory) {
        if (state.selectedExpenseCategory == null) {
            state.expenses
        } else {
            state.expenses.filter { it.category == state.selectedExpenseCategory }
        }
    }

    val totalFilteredAmount = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Terracotta,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add Expense") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedExpenseCategory == null,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("All (${state.expenses.size})") }
                    )
                }
                items(ExpenseCategory.values()) { category ->
                    val isSelected = state.selectedExpenseCategory == category
                    val count = state.expenses.count { it.category == category }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryFilterChange(if (isSelected) null else category) },
                        label = { Text("${category.name.replace("_", " ")} ($count)") }
                    )
                }
            }

            // Category Summary Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.selectedExpenseCategory != null)
                            state.selectedExpenseCategory!!.name.replace("_", " ")
                        else "Total Expenses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "৳${"%,.2f".format(totalFilteredAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Terracotta
                    )
                }
            }

            // List of Expenses
            if (filteredExpenses.isEmpty()) {
                EmptyState(
                    title = "No expenses found",
                    description = if (state.selectedExpenseCategory != null)
                        "No expenses recorded under ${state.selectedExpenseCategory?.name} category."
                    else "Click '+ Add Expense' to record your first expense.",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        val isDeleting = state.deletingExpenseIds.contains(expense.id)
                        ExpenseItemCard(
                            expense = expense,
                            isDeleting = isDeleting,
                            onEdit = { editingExpense = expense },
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddDialog) {
        ExpenseFormDialog(
            title = "Add Expense",
            members = state.members,
            recentExpenses = state.expenses,
            isLoading = state.isCreatingExpense,
            onDismiss = { showAddDialog = false },
            onSave = { category, title, amount, date, receiptUrl, notes, targetMemberId, participantIds ->
                onCreateExpense(
                    category, title, amount, date, receiptUrl, notes, targetMemberId, participantIds
                ) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Expense Dialog
    if (editingExpense != null) {
        val expense = editingExpense!!
        ExpenseFormDialog(
            title = "Edit Expense",
            initialCategory = expense.category,
            initialTitle = expense.title,
            initialAmount = expense.amount,
            initialDate = expense.expenseDate,
            initialReceiptUrl = expense.receiptUrl,
            initialNotes = expense.notes,
            initialTargetMemberId = expense.targetMemberId,
            initialParticipantIds = expense.participantIds,
            members = state.members,
            recentExpenses = state.expenses,
            isLoading = false,
            onDismiss = { editingExpense = null },
            onSave = { category, title, amount, date, receiptUrl, notes, targetMemberId, participantIds ->
                onUpdateExpense(
                    expense.id, category, title, amount, date, receiptUrl, notes, targetMemberId, participantIds
                ) {
                    editingExpense = null
                }
            }
        )
    }

    // Delete Expense Confirmation Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Expense?") },
            text = {
                Text(
                    text = "Are you sure you want to delete '${exp.title}' (৳${"%,.2f".format(exp.amount)})? This will recalculate the cycle finances."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(exp.id)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    expense: Expense,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = DhakaDateUtils.formatDhakaDateHeader(expense.expenseDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "৳${"%,.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Terracotta
                )
            }

            // Category & Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = expense.category.name.replace("_", " "),
                    color = when (expense.category) {
                        ExpenseCategory.MEAL_VARIABLE -> Terracotta
                        ExpenseCategory.FIXED_OVERHEAD -> Color(0xFF3B82F6)
                        ExpenseCategory.INDIVIDUAL_DIRECT -> Color(0xFF8B5CF6)
                        ExpenseCategory.AD_HOC_SPECIAL -> Color(0xFFEC4899)
                        ExpenseCategory.OTHER -> Color(0xFF10B981)
                        else -> Terracotta
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        enabled = !isDeleting,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = !isDeleting,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Target or participant info if applicable
            if (expense.category == ExpenseCategory.INDIVIDUAL_DIRECT && expense.targetMemberName != null) {
                Text(
                    text = "Target: ${expense.targetMemberName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expense.participantIds.isNotEmpty()) {
                Text(
                    text = "Split between ${expense.participantIds.size} participant(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseFormDialog(
    title: String,
    initialCategory: ExpenseCategory = ExpenseCategory.MEAL_VARIABLE,
    initialTitle: String = "",
    initialAmount: Double = 0.0,
    initialDate: String = DhakaDateUtils.todayDateString(),
    initialReceiptUrl: String? = null,
    initialNotes: String? = null,
    initialTargetMemberId: String? = null,
    initialParticipantIds: List<String> = emptyList(),
    members: List<MemberInfo>,
    recentExpenses: List<Expense>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        category: ExpenseCategory,
        title: String,
        amount: Double,
        date: String,
        receiptUrl: String?,
        notes: String?,
        targetMemberId: String?,
        participantIds: List<String>
    ) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var titleText by remember { mutableStateOf(initialTitle) }
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var dateText by remember { mutableStateOf(initialDate) }
    var notesText by remember { mutableStateOf(initialNotes ?: "") }
    var targetMemberId by remember { mutableStateOf(initialTargetMemberId) }
    var participantIds by remember { mutableStateOf(initialParticipantIds.toSet()) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showSoftWarningDialog by remember { mutableStateOf(false) }

    // Average calculation for soft-confirmation warning
    val avgExpense = remember(recentExpenses) {
        if (recentExpenses.isNotEmpty()) recentExpenses.map { it.amount }.average() else 1000.0
    }

    fun validateAndProceed() {
        var isValid = true
        if (titleText.trim().length < 3) {
            titleError = "Title must be at least 3 characters"
            isValid = false
        } else {
            titleError = null
        }

        val amt = amountText.toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            amountError = "Enter a valid positive amount"
            isValid = false
        } else {
            amountError = null
        }

        if (!isValid) return

        val finalAmount = amt!!
        // Soft confirmation warning rule: > ৳50,000 or > 10x average expense
        val isUnusuallyLarge = finalAmount > 50000 || (recentExpenses.size >= 3 && finalAmount > avgExpense * 10)
        if (isUnusuallyLarge) {
            showSoftWarningDialog = true
        } else {
            onSave(
                category,
                titleText.trim(),
                finalAmount,
                dateText.trim(),
                null,
                notesText.takeIf { it.isNotBlank() },
                targetMemberId,
                participantIds.toList()
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("Title / Description *") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (amountError != null) amountError = null
                    },
                    label = { Text("Amount (৳) *") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Field (Dhaka timezone default)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Expense Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                Text("Category *", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExpenseCategory.values().forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { category = cat }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = category == cat,
                                onClick = { category = cat }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Inline help text for OTHER category
                if (category == ExpenseCategory.OTHER) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Other expenses split only among selected participants — leave empty to split equally",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Target Member Picker for INDIVIDUAL_DIRECT
                if (category == ExpenseCategory.INDIVIDUAL_DIRECT) {
                    Text("Target Member (required for direct charge) *", style = MaterialTheme.typography.labelMedium)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        members.forEach { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { targetMemberId = m.userId }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = targetMemberId == m.userId,
                                    onClick = { targetMemberId = m.userId }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = m.displayNameWithRoom,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Participant Multi-select for AD_HOC_SPECIAL or OTHER
                if (category == ExpenseCategory.AD_HOC_SPECIAL || category == ExpenseCategory.OTHER) {
                    Text("Select Participants (Optional)", style = MaterialTheme.typography.labelMedium)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        members.forEach { m ->
                            val isChecked = participantIds.contains(m.userId)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        participantIds = if (isChecked) {
                                            participantIds - m.userId
                                        } else {
                                            participantIds + m.userId
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        participantIds = if (checked) {
                                            participantIds + m.userId
                                        } else {
                                            participantIds - m.userId
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = m.displayNameWithRoom,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndProceed() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Expense")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )

    // Soft-confirmation Warning Dialog
    if (showSoftWarningDialog) {
        val amt = amountText.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { showSoftWarningDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningAmber) },
            title = { Text("Unusually High Amount") },
            text = {
                Text(
                    text = "The amount ৳${"%,.2f".format(amt)} looks unusually high compared to typical mess expenses. Are you sure this amount is correct?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSoftWarningDialog = false
                        onSave(
                            category,
                            titleText.trim(),
                            amt,
                            dateText.trim(),
                            null,
                            notesText.takeIf { it.isNotBlank() },
                            targetMemberId,
                            participantIds.toList()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    Text("Yes, Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoftWarningDialog = false }) {
                    Text("Edit Amount")
                }
            }
        )
    }
}
