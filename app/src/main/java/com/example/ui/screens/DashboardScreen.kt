package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import com.example.ui.PayWiseUiState
import com.example.ui.components.IncomeVsExpensesChartCard
import com.example.ui.components.SpendingBreakdownChartCard
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.PngBrightEmerald
import com.example.ui.theme.PngDeepGreen
import com.example.ui.theme.PngForestGreen
import com.example.ui.theme.PngGold
import com.example.ui.theme.RobotoMonoFontFamily

@Composable
fun DashboardScreen(
    uiState: PayWiseUiState,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddSavingsClick: () -> Unit,
    onViewBudgetClick: () -> Unit,
    onConfigurePaydayClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onLoadDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fin = uiState.financialSummary
    val payday = uiState.paydaySummary
    val currency = uiState.settings.currencyFormat

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banking Balance & Payday Card
        item {
            HeroBalanceCard(
                availableBalance = FinanceCalculator.formatCurrency(fin.availableBalance, currency),
                totalIncome = FinanceCalculator.formatCurrency(fin.totalIncome, currency),
                totalExpenses = FinanceCalculator.formatCurrency(fin.totalExpenses, currency),
                totalSavings = FinanceCalculator.formatCurrency(fin.currentSavings, currency),
                remainingBudget = FinanceCalculator.formatCurrency(fin.remainingBudget, currency),
                nextPaydayText = FinanceCalculator.formatDate(payday.nextPayday.toEpochDay()),
                daysRemaining = payday.daysRemaining,
                currentPeriodText = "${FinanceCalculator.formatShortDate(payday.currentPeriodStart.toEpochDay())} – ${FinanceCalculator.formatDate(payday.currentPeriodEnd.toEpochDay())}",
                paydayProgress = payday.progressFraction,
                onConfigurePaydayClick = onConfigurePaydayClick
            )
        }

        // 2. Quick Actions Row
        item {
            QuickActionsRow(
                onAddIncomeClick = onAddIncomeClick,
                onAddExpenseClick = onAddExpenseClick,
                onAddSavingsClick = onAddSavingsClick,
                onViewBudgetClick = onViewBudgetClick
            )
        }

        // 3. Income vs Expenses Chart
        item {
            IncomeVsExpensesChartCard(
                totalIncome = fin.totalIncome,
                totalExpenses = fin.totalExpenses,
                currentSavings = fin.currentSavings,
                currencyFormat = currency
            )
        }

        // 4. Spending Breakdown Chart
        item {
            SpendingBreakdownChartCard(
                breakdowns = uiState.categoryBreakdowns,
                totalExpenses = fin.totalExpenses,
                currencyFormat = currency
            )
        }

        // 5. Savings Goals Progress Section
        item {
            SavingsProgressDashboardCard(
                savingsGoals = uiState.savingsGoals,
                currentSavingsTotal = fin.currentSavings,
                currencyFormat = currency,
                onManageSavingsClick = onAddSavingsClick
            )
        }

        // 6. Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.transactions.size} total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Transactions Recorded Yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start by adding your fortnightly salary, recording an expense, or loading clearly labeled sample PNG fortnight data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onAddIncomeClick) {
                                Text("Add Income")
                            }
                            OutlinedButton(
                                onClick = onLoadDemoData,
                                modifier = Modifier.testTag("load_demo_data_button")
                            ) {
                                Text("Load [DEMO] Sample")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.transactions.take(8),
                key = { it.id }
            ) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    currencyFormat = currency,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    availableBalance: String,
    totalIncome: String,
    totalExpenses: String,
    totalSavings: String,
    remainingBudget: String,
    nextPaydayText: String,
    daysRemaining: Long,
    currentPeriodText: String,
    paydayProgress: Float,
    onConfigurePaydayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Generated PNG Bilum & Finance Hero Illustration Background
            Image(
                painter = painterResource(id = R.drawable.img_hero_png_finance_1794299111297),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Deep Emerald Overlay for High-Contrast Readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PngDeepGreen.copy(alpha = 0.88f),
                                PngForestGreen.copy(alpha = 0.94f),
                                Color(0xFF05180E).copy(alpha = 0.97f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL AVAILABLE BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = PngGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = availableBalance,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("available_balance_text")
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .clickable(onClick = onConfigurePaydayClick)
                            .testTag("payday_badge_button")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Configure Payday",
                                    tint = PngGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (daysRemaining == 0L) "Payday Today!" else "$daysRemaining days left",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PngGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Next: $nextPaydayText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Pay Period Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pay Period: $currentPeriodText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Fortnightly (14d)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PngBrightEmerald
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { paydayProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PngGold,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 Summary Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricPill(
                        label = "Total Income",
                        value = totalIncome,
                        accentColor = IncomeGreenDark,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_total_income"
                    )
                    MetricPill(
                        label = "Total Expenses",
                        value = totalExpenses,
                        accentColor = ExpenseRedDark,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_total_expenses"
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricPill(
                        label = "Total Savings",
                        value = totalSavings,
                        accentColor = PngGold,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_total_savings"
                    )
                    MetricPill(
                        label = "Remaining Budget",
                        value = remainingBudget,
                        accentColor = PngBrightEmerald,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_remaining_budget"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddSavingsClick: () -> Unit,
    onViewBudgetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = "Add Income",
            icon = Icons.Default.AddCircleOutline,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onAddIncomeClick,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_income")
        )
        QuickActionButton(
            label = "Add Expense",
            icon = Icons.Default.RemoveCircleOutline,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onAddExpenseClick,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_expense")
        )
        QuickActionButton(
            label = "Add Savings",
            icon = Icons.Default.Savings,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onAddSavingsClick,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_savings")
        )
        QuickActionButton(
            label = "View Budget",
            icon = Icons.Default.PieChart,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            onClick = onViewBudgetClick,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_view_budget")
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SavingsProgressDashboardCard(
    savingsGoals: List<SavingsGoalEntity>,
    currentSavingsTotal: java.math.BigDecimal,
    currencyFormat: String,
    onManageSavingsClick: () -> Unit
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
                Column {
                    Text(
                        text = "Savings Goals & Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Saved: ${FinanceCalculator.formatCurrency(currentSavingsTotal, currencyFormat)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMonoFontFamily),
                        color = PngGold
                    )
                }
                FilledTonalButton(
                    onClick = onManageSavingsClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Manage Savings", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (savingsGoals.isEmpty()) {
                Text(
                    text = "No savings goals set yet. Tap 'Manage Savings' to set a target (e.g. K2,000 Emergency Fund) and track your percentage progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    savingsGoals.forEach { goal ->
                        val pct = FinanceCalculator.calculateSavingsPercentage(goal.currentAmount, goal.targetAmount)
                        val fraction = (pct / 100f).coerceIn(0f, 1f)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${FinanceCalculator.formatCurrency(goal.currentAmount, currencyFormat)} / ${FinanceCalculator.formatCurrency(goal.targetAmount, currencyFormat)} ($pct%)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = RobotoMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PngGold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PngGold,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    currencyFormat: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (amountPrefix, amountColor, badgeIcon) = when (transaction.type) {
        TransactionType.INCOME -> Triple("+", IncomeGreenDark, Icons.Default.ArrowDownward)
        TransactionType.EXPENSE -> Triple("-", ExpenseRedDark, Icons.Default.ArrowUpward)
        TransactionType.SAVINGS_DEPOSIT -> Triple("→", PngGold, Icons.Default.Savings)
        else -> Triple("←", PngBrightEmerald, Icons.Default.Savings)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.type == TransactionType.EXPENSE) {
                            getCategoryIcon(transaction.categoryName)
                        } else badgeIcon,
                        contentDescription = transaction.categoryName,
                        tint = amountColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.categoryName} • ${FinanceCalculator.formatDate(transaction.dateEpochDay)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$amountPrefix${FinanceCalculator.formatCurrency(transaction.amount, currencyFormat)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = amountColor
                )
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
