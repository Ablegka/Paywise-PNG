package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetAllocationEntity
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.SavingsGoalEntity
import com.example.domain.CategoryBreakdown
import com.example.domain.FinanceCalculator
import com.example.ui.PayWiseUiState
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.parseHexColor
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.PngBrightEmerald
import com.example.ui.theme.PngGold
import com.example.ui.theme.RobotoMonoFontFamily

@Composable
fun BudgetScreen(
    uiState: PayWiseUiState,
    onAddAllocation: (name: String, plannedAmountStr: String, category: BudgetCategoryEntity) -> Unit,
    onToggleAllocationPaid: (BudgetAllocationEntity, Boolean) -> Unit,
    onDeleteAllocation: (BudgetAllocationEntity) -> Unit,
    onAddCategory: (name: String, budgetAmountStr: String) -> Unit,
    onUpdateCategory: (BudgetCategoryEntity, newName: String, newBudgetStr: String) -> Unit,
    onDeleteCategory: (BudgetCategoryEntity) -> Unit,
    onQuickAddExpenseForCategory: (BudgetCategoryEntity, String?) -> Unit,
    onOpenSavingsDialog: () -> Unit,
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = uiState.settings.currencyFormat
    val fin = uiState.financialSummary

    var showAddAllocationDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<BudgetCategoryEntity?>(null) }
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Fortnightly Budget Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fortnightly Budget Plan",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Period: ${FinanceCalculator.formatShortDate(uiState.paydaySummary.currentPeriodStart.toEpochDay())} – ${FinanceCalculator.formatDate(uiState.paydaySummary.currentPeriodEnd.toEpochDay())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.testTag("add_custom_category_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Category")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Budgeted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceCalculator.formatCurrency(fin.totalBudgetPlanned, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceCalculator.formatCurrency(fin.totalExpenses, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ExpenseRedDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceCalculator.formatCurrency(fin.remainingBudget, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (fin.remainingBudget >= FinanceCalculator.ZERO) PngBrightEmerald else ExpenseRedDark
                            )
                        }
                    }
                }
            }
        }

        // 2. Budget Checkbox System (Section 10)
        item {
            BudgetCheckboxSectionCard(
                allocations = uiState.budgetAllocations,
                currencyFormat = currency,
                onAddItemClick = { showAddAllocationDialog = true },
                onTogglePaid = onToggleAllocationPaid,
                onDeleteAllocation = onDeleteAllocation
            )
        }

        // 3. Budget Categories Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Categories (${uiState.categoryBreakdowns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap category for history",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Category Breakdown Cards (including Flex / Rait Kad K3, K5, K10, K20 buttons)
        items(
            items = uiState.categoryBreakdowns,
            key = { it.category.id }
        ) { breakdown ->
            CategoryBudgetCard(
                breakdown = breakdown,
                currencyFormat = currency,
                isExpanded = expandedCategoryId == breakdown.category.id,
                onToggleExpand = {
                    expandedCategoryId = if (expandedCategoryId == breakdown.category.id) null else breakdown.category.id
                },
                onEditCategory = { editingCategory = breakdown.category },
                onDeleteCategory = { onDeleteCategory(breakdown.category) },
                onQuickAddExpense = { presetAmount ->
                    onQuickAddExpenseForCategory(breakdown.category, presetAmount)
                }
            )
        }

        // 5. Savings Goals Section
        item {
            SavingsGoalsManagementCard(
                savingsGoals = uiState.savingsGoals,
                currencyFormat = currency,
                onOpenSavingsDialog = onOpenSavingsDialog,
                onDeleteSavingsGoal = onDeleteSavingsGoal
            )
        }
    }

    if (showAddAllocationDialog && uiState.categories.isNotEmpty()) {
        AddBudgetAllocationDialog(
            categories = uiState.categories,
            currencyFormat = currency,
            onDismiss = { showAddAllocationDialog = false },
            onSave = { name, amountStr, category ->
                onAddAllocation(name, amountStr, category)
            }
        )
    }

    if (showAddCategoryDialog) {
        AddOrEditCategoryDialog(
            initialCategory = null,
            currencyFormat = currency,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, budgetStr ->
                onAddCategory(name, budgetStr)
            }
        )
    }

    editingCategory?.let { cat ->
        AddOrEditCategoryDialog(
            initialCategory = cat,
            currencyFormat = currency,
            onDismiss = { editingCategory = null },
            onSave = { name, budgetStr ->
                onUpdateCategory(cat, name, budgetStr)
            }
        )
    }
}

@Composable
private fun BudgetCheckboxSectionCard(
    allocations: List<BudgetAllocationEntity>,
    currencyFormat: String,
    onAddItemClick: () -> Unit,
    onTogglePaid: (BudgetAllocationEntity, Boolean) -> Unit,
    onDeleteAllocation: (BudgetAllocationEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_checkbox_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Payday Budget Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Check an item as paid to auto-deduct from balance & record expense. Uncheck to reverse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddItemClick,
                    modifier = Modifier.testTag("add_checklist_item_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Plan Item")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (allocations.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No planned budget checklist items yet. Tap 'Plan Item' before payday to list your upcoming expenses (e.g. Market K100, Dinau K50, Rait Kad K10).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allocations.forEach { item ->
                        Surface(
                            color = if (item.isPaid) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = item.isPaid,
                                        onCheckedChange = { checked ->
                                            onTogglePaid(item, checked)
                                        },
                                        modifier = Modifier.testTag("allocation_checkbox_${item.id}")
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (item.isPaid) TextDecoration.LineThrough else TextDecoration.None,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.categoryName} • ${if (item.isPaid) "PAID & DEDUCTED" else "PLANNED"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.isPaid) PngBrightEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = FinanceCalculator.formatCurrency(item.plannedAmount, currencyFormat),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = RobotoMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (item.isPaid) PngBrightEmerald else PngGold
                                    )
                                    IconButton(onClick = { onDeleteAllocation(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Planned Item",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    breakdown: CategoryBreakdown,
    currencyFormat: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onQuickAddExpense: (presetAmount: String?) -> Unit
) {
    val cat = breakdown.category
    val accentColor = parseHexColor(cat.colorHex)
    val isFlexRaitKad = cat.name.contains("Flex", ignoreCase = true) || cat.name.contains("Rait", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(cat.name, cat.iconName),
                            contentDescription = cat.name,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Budget: ${FinanceCalculator.formatCurrency(cat.budgetAmount, currencyFormat)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditCategory) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Category",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!cat.isDefault) {
                        IconButton(onClick = onDeleteCategory) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Custom Category",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Category History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${FinanceCalculator.formatCurrency(breakdown.spentAmount, currencyFormat)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMonoFontFamily),
                    color = ExpenseRedDark
                )
                Text(
                    text = "Remaining: ${FinanceCalculator.formatCurrency(breakdown.remainingAmount, currencyFormat)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (breakdown.remainingAmount >= FinanceCalculator.ZERO) IncomeGreenDark else ExpenseRedDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { breakdown.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (breakdown.remainingAmount >= FinanceCalculator.ZERO) accentColor else ExpenseRedDark,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Special Quick-Select Row for Flex / Rait Kad (K3, K5, K10, K20) per Section 6
            if (isFlexRaitKad) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Record Flex / Rait Kad:",
                    style = MaterialTheme.typography.labelSmall,
                    color = PngGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceCalculator.FLEX_RAIT_KAD_OPTIONS.forEach { amt ->
                        OutlinedButton(
                            onClick = { onQuickAddExpense(amt.toPlainString()) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("K${amt.toInt()}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Expandable Category Transaction History (Section 6)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Transactions (${breakdown.transactions.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { onQuickAddExpense(null) }) {
                            Text("+ Add ${cat.name} Expense")
                        }
                    }

                    if (breakdown.transactions.isEmpty()) {
                        Text(
                            text = "No transactions recorded in ${cat.name} yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        breakdown.transactions.take(6).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${FinanceCalculator.formatShortDate(tx.dateEpochDay)} • ${tx.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "-${FinanceCalculator.formatCurrency(tx.amount, currencyFormat)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = RobotoMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = ExpenseRedDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalsManagementCard(
    savingsGoals: List<SavingsGoalEntity>,
    currencyFormat: String,
    onOpenSavingsDialog: () -> Unit,
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = PngGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Savings Goals",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(onClick = onOpenSavingsDialog) {
                    Text("Deposit / New Goal")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (savingsGoals.isEmpty()) {
                Text(
                    text = "Create a savings goal (e.g. K2,000 target) to track your progress separately from ordinary expenses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    savingsGoals.forEach { goal ->
                        val pct = FinanceCalculator.calculateSavingsPercentage(goal.currentAmount, goal.targetAmount)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Current: ${FinanceCalculator.formatCurrency(goal.currentAmount, currencyFormat)} / Goal: ${FinanceCalculator.formatCurrency(goal.targetAmount, currencyFormat)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMonoFontFamily),
                                            color = PngGold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = RobotoMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = PngBrightEmerald
                                        )
                                        IconButton(onClick = { onDeleteSavingsGoal(goal) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Goal",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PngGold,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBudgetAllocationDialog(
    categories: List<BudgetCategoryEntity>,
    currencyFormat: String,
    onDismiss: () -> Unit,
    onSave: (name: String, plannedAmountStr: String, category: BudgetCategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Planned Budget Item", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Item Name (e.g. Market Food, PMV Bus, Dinau)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allocation_name_input")
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("Planned Amount ($currencyFormat)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allocation_amount_input")
                )
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.id == cat.id,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name) }
                        )
                    }
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
                    val amt = FinanceCalculator.toMoney(amountStr)
                    if (name.isBlank() || amt <= FinanceCalculator.ZERO) {
                        errorText = "Enter an item name and amount > 0.00"
                    } else {
                        onSave(name, amountStr, selectedCategory)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_allocation_button")
            ) {
                Text("Add to Checklist")
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
private fun AddOrEditCategoryDialog(
    initialCategory: BudgetCategoryEntity?,
    currencyFormat: String,
    onDismiss: () -> Unit,
    onSave: (name: String, budgetAmountStr: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var budgetStr by remember { mutableStateOf(initialCategory?.budgetAmount?.toPlainString() ?: "100.00") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "Create Custom Category" else "Edit Category",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )
                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = {
                        budgetStr = it
                        errorText = null
                    },
                    label = { Text("Fortnightly Budget ($currencyFormat)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_budget_input")
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
                    if (name.isBlank()) {
                        errorText = "Category name cannot be empty"
                    } else {
                        onSave(name, budgetStr)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text("Save Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
