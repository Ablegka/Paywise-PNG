package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import com.example.ui.theme.PngGold
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditIncomeDialog(
    initialTransaction: TransactionEntity? = null,
    currencyFormat: String,
    defaultSalary: BigDecimal,
    onDismiss: () -> Unit,
    onSave: (amountStr: String, source: String, description: String, dateEpochDay: Long) -> Unit
) {
    var selectedSource by remember {
        mutableStateOf(initialTransaction?.incomeSource ?: FinanceCalculator.DEFAULT_INCOME_SOURCES.first())
    }
    var amountStr by remember {
        mutableStateOf(
            initialTransaction?.amount?.toPlainString()
                ?: if (selectedSource == "Fortnightly Salary") defaultSalary.toPlainString() else ""
        )
    }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var dateEpochDay by remember {
        mutableLongStateOf(initialTransaction?.dateEpochDay ?: LocalDate.now().toEpochDay())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null) "Record Income" else "Edit Income",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Income Source",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinanceCalculator.DEFAULT_INCOME_SOURCES.forEach { source ->
                        FilterChip(
                            selected = selectedSource == source,
                            onClick = {
                                selectedSource = source
                                if (source == "Fortnightly Salary" && amountStr.isBlank()) {
                                    amountStr = defaultSalary.toPlainString()
                                }
                            },
                            label = { Text(source) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("Amount ($currencyFormat)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Fortnight Pay, Market Sales)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_description_input")
                )

                DateSelectorRow(
                    epochDay = dateEpochDay,
                    onDateChange = { dateEpochDay = it }
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = FinanceCalculator.toMoney(amountStr)
                    if (parsed <= FinanceCalculator.ZERO) {
                        errorText = "Please enter an amount greater than 0.00"
                    } else {
                        onSave(amountStr, selectedSource, description, dateEpochDay)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_income_button")
            ) {
                Text("Save Income")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditExpenseDialog(
    categories: List<BudgetCategoryEntity>,
    initialTransaction: TransactionEntity? = null,
    preselectedCategory: BudgetCategoryEntity? = null,
    currencyFormat: String,
    onDismiss: () -> Unit,
    onSave: (amountStr: String, categoryId: Long?, categoryName: String, description: String, dateEpochDay: Long) -> Unit
) {
    val defaultCat = preselectedCategory
        ?: categories.find { it.id == initialTransaction?.categoryId || it.name == initialTransaction?.categoryName }
        ?: categories.firstOrNull()

    var selectedCategory by remember { mutableStateOf(defaultCat) }
    var amountStr by remember { mutableStateOf(initialTransaction?.amount?.toPlainString() ?: "") }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var dateEpochDay by remember {
        mutableLongStateOf(initialTransaction?.dateEpochDay ?: LocalDate.now().toEpochDay())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isFlexCategory = selectedCategory?.name?.contains("Flex", ignoreCase = true) == true ||
        selectedCategory?.name?.contains("Rait", ignoreCase = true) == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null) "Record Expense" else "Edit Expense",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory?.id == cat.id,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name) }
                        )
                    }
                }

                // Special Flex / Rait Kad Quick Options: K3, K5, K10, K20 (Section 6)
                if (isFlexCategory) {
                    Text(
                        text = "Flex / Rait Kad Voucher Options:",
                        style = MaterialTheme.typography.labelMedium,
                        color = PngGold,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinanceCalculator.FLEX_RAIT_KAD_OPTIONS.forEach { optionAmt ->
                            val label = "K${optionAmt.toInt()}"
                            val isSelected = FinanceCalculator.toMoney(amountStr) == optionAmt
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    amountStr = optionAmt.toPlainString()
                                    if (description.isBlank() || description.startsWith("Flex / Rait Kad K")) {
                                        description = "Flex / Rait Kad $label"
                                    }
                                    errorText = null
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.testTag("flex_option_${optionAmt.toInt()}")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("Expense Amount ($currencyFormat)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Market, PMV Fare, Wanpis)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_description_input")
                )

                DateSelectorRow(
                    epochDay = dateEpochDay,
                    onDateChange = { dateEpochDay = it }
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = FinanceCalculator.toMoney(amountStr)
                    if (parsed <= FinanceCalculator.ZERO) {
                        errorText = "Please enter an expense amount greater than 0.00"
                    } else {
                        val cat = selectedCategory
                        onSave(
                            amountStr,
                            cat?.id,
                            cat?.name ?: "Other Expenses",
                            description,
                            dateEpochDay
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingsActionDialog(
    savingsGoals: List<SavingsGoalEntity>,
    currencyFormat: String,
    onDismiss: () -> Unit,
    onCreateGoal: (name: String, targetAmountStr: String) -> Unit,
    onDeposit: (goal: SavingsGoalEntity, amountStr: String, description: String) -> Unit,
    onWithdraw: (goal: SavingsGoalEntity, amountStr: String, description: String) -> Unit
) {
    var mode by remember {
        mutableStateOf(if (savingsGoals.isEmpty()) "NEW_GOAL" else "DEPOSIT")
    }
    var selectedGoal by remember(savingsGoals) {
        mutableStateOf(savingsGoals.firstOrNull())
    }
    var goalName by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("2000.00") }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Savings Management", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (savingsGoals.isNotEmpty()) {
                        FilterChip(
                            selected = mode == "DEPOSIT",
                            onClick = {
                                mode = "DEPOSIT"
                                errorText = null
                            },
                            label = { Text("Add Savings") }
                        )
                        FilterChip(
                            selected = mode == "WITHDRAW",
                            onClick = {
                                mode = "WITHDRAW"
                                errorText = null
                            },
                            label = { Text("Withdraw") }
                        )
                    }
                    FilterChip(
                        selected = mode == "NEW_GOAL",
                        onClick = {
                            mode = "NEW_GOAL"
                            errorText = null
                        },
                        label = { Text("+ New Goal") }
                    )
                }

                if (mode == "NEW_GOAL") {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = {
                            goalName = it
                            errorText = null
                        },
                        label = { Text("Savings Goal Name (e.g. Emergency, School Fees)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("savings_goal_name_input")
                    )
                    OutlinedTextField(
                        value = targetAmountStr,
                        onValueChange = {
                            targetAmountStr = it
                            errorText = null
                        },
                        label = { Text("Target Amount ($currencyFormat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("savings_goal_target_input")
                    )
                } else {
                    Text(
                        text = "Select Savings Goal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        savingsGoals.forEach { goal ->
                            val pct = FinanceCalculator.calculateSavingsPercentage(goal.currentAmount, goal.targetAmount)
                            FilterChip(
                                selected = selectedGoal?.id == goal.id,
                                onClick = { selectedGoal = goal },
                                label = {
                                    Text("${goal.name} (${FinanceCalculator.formatCurrency(goal.currentAmount, currencyFormat)} - $pct%)")
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            errorText = null
                        },
                        label = {
                            Text(
                                if (mode == "DEPOSIT") "Deposit Amount ($currencyFormat)"
                                else "Withdrawal Amount ($currencyFormat)"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("savings_tx_amount_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Note / Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (mode) {
                        "NEW_GOAL" -> {
                            val target = FinanceCalculator.toMoney(targetAmountStr)
                            if (goalName.isBlank() || target <= FinanceCalculator.ZERO) {
                                errorText = "Enter a valid goal name and target > K0.00"
                            } else {
                                onCreateGoal(goalName, targetAmountStr)
                                onDismiss()
                            }
                        }
                        "DEPOSIT" -> {
                            val goal = selectedGoal
                            val amt = FinanceCalculator.toMoney(amountStr)
                            if (goal == null || amt <= FinanceCalculator.ZERO) {
                                errorText = "Enter a valid deposit amount > K0.00"
                            } else {
                                onDeposit(goal, amountStr, description)
                                onDismiss()
                            }
                        }
                        "WITHDRAW" -> {
                            val goal = selectedGoal
                            val amt = FinanceCalculator.toMoney(amountStr)
                            if (goal == null || amt <= FinanceCalculator.ZERO) {
                                errorText = "Enter a valid withdrawal amount > K0.00"
                            } else if (amt > goal.currentAmount) {
                                errorText = "Amount exceeds current goal balance (${FinanceCalculator.formatCurrency(goal.currentAmount, currencyFormat)})"
                            } else {
                                onWithdraw(goal, amountStr, description)
                                onDismiss()
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_savings_button")
            ) {
                Text(
                    when (mode) {
                        "NEW_GOAL" -> "Create Goal"
                        "DEPOSIT" -> "Deposit Savings"
                        else -> "Withdraw Savings"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaydayConfigDialog(
    currentPaydayEpochDay: Long,
    currentSalary: BigDecimal,
    currencyFormat: String,
    onDismiss: () -> Unit,
    onSave: (newEpochDay: Long, salaryStr: String, logSalaryNow: Boolean, notes: String) -> Unit
) {
    var selectedEpochDay by remember { mutableLongStateOf(currentPaydayEpochDay) }
    var salaryStr by remember { mutableStateOf(currentSalary.toPlainString()) }
    var logSalaryNow by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fortnightly Payday Setup", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pay Frequency: Every 14 Days (Fortnightly)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                DateSelectorRow(
                    label = "Reference Payday Date",
                    epochDay = selectedEpochDay,
                    onDateChange = { selectedEpochDay = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedEpochDay = FinanceCalculator.DEFAULT_PAYDAY.toEpochDay() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Default (17 Jul 2026)", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { selectedEpochDay = LocalDate.now().toEpochDay() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Today", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = salaryStr,
                    onValueChange = { salaryStr = it },
                    label = { Text("Fortnightly Salary ($currencyFormat)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payday_salary_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { logSalaryNow = !logSalaryNow }
                ) {
                    Checkbox(
                        checked = logSalaryNow,
                        onCheckedChange = { logSalaryNow = it }
                    )
                    Text(
                        text = "Start new pay period & credit salary to income now",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (logSalaryNow) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Pay Period Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedEpochDay, salaryStr, logSalaryNow, notes)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_payday_button")
            ) {
                Text("Save Payday")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DateSelectorRow(
    label: String = "Transaction Date",
    epochDay: Long,
    onDateChange: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDateChange(epochDay - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Previous Day")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = FinanceCalculator.formatDate(epochDay),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = { onDateChange(epochDay + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Next Day")
            }
        }
    }
}
