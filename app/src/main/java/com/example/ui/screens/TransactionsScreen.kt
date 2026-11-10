package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import com.example.ui.HistoryTimePeriod
import com.example.ui.PayWiseUiState
import com.example.ui.components.IncomeVsExpensesChartCard
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.PngGold
import com.example.ui.theme.RobotoMonoFontFamily
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    uiState: PayWiseUiState,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = uiState.settings.currencyFormat
    var selectedPeriod by remember { mutableStateOf(HistoryTimePeriod.ALL) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME, SAVINGS
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val today = LocalDate.now()
    val todayEpoch = today.toEpochDay()
    val weekStartEpoch = today.minusDays(6).toEpochDay()
    val fortnightStartEpoch = uiState.paydaySummary.currentPeriodStart.toEpochDay()
    val fortnightEndEpoch = uiState.paydaySummary.currentPeriodEnd.toEpochDay()
    val monthStartEpoch = today.withDayOfMonth(1).toEpochDay()

    val filteredTransactions = remember(
        uiState.transactions,
        selectedPeriod,
        selectedTypeFilter,
        selectedCategoryFilter,
        searchQuery
    ) {
        uiState.transactions.filter { tx ->
            val matchesPeriod = when (selectedPeriod) {
                HistoryTimePeriod.ALL -> true
                HistoryTimePeriod.DAILY -> tx.dateEpochDay == todayEpoch
                HistoryTimePeriod.WEEKLY -> tx.dateEpochDay in weekStartEpoch..todayEpoch
                HistoryTimePeriod.FORTNIGHTLY -> tx.dateEpochDay in fortnightStartEpoch..fortnightEndEpoch
                HistoryTimePeriod.MONTHLY -> tx.dateEpochDay >= monthStartEpoch
            }
            val matchesType = when (selectedTypeFilter) {
                "EXPENSE" -> tx.type == TransactionType.EXPENSE
                "INCOME" -> tx.type == TransactionType.INCOME
                "SAVINGS" -> tx.type == TransactionType.SAVINGS_DEPOSIT || tx.type == TransactionType.SAVINGS_WITHDRAWAL
                else -> true
            }
            val matchesCat = selectedCategoryFilter == null ||
                tx.categoryName.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.categoryName.contains(searchQuery, ignoreCase = true)

            matchesPeriod && matchesType && matchesCat && matchesQuery
        }
    }

    val filteredIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }
            .fold(FinanceCalculator.ZERO) { acc, tx -> acc.add(FinanceCalculator.toMoney(tx.amount)) }
    }
    val filteredExpenses = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }
            .fold(FinanceCalculator.ZERO) { acc, tx -> acc.add(FinanceCalculator.toMoney(tx.amount)) }
    }
    val filteredSavings = remember(filteredTransactions) {
        val dep = filteredTransactions.filter { it.type == TransactionType.SAVINGS_DEPOSIT }
            .fold(FinanceCalculator.ZERO) { acc, tx -> acc.add(FinanceCalculator.toMoney(tx.amount)) }
        val wd = filteredTransactions.filter { it.type == TransactionType.SAVINGS_WITHDRAWAL }
            .fold(FinanceCalculator.ZERO) { acc, tx -> acc.add(FinanceCalculator.toMoney(tx.amount)) }
        dep.subtract(wd).max(FinanceCalculator.ZERO)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Record Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Financial History",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily, Weekly, Fortnightly & Monthly Records",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onAddIncomeClick) {
                        Text("+ Income")
                    }
                    Button(onClick = onAddExpenseClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense")
                    }
                }
            }
        }

        // 2. Time Period Filter Chips (All, Daily, Weekly, Fortnightly, Monthly)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryTimePeriod.entries) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) }
                    )
                }
            }
        }

        // 3. Transaction Type & Category Filters
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search description or category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All Types",
                            "EXPENSE" to "Expenses",
                            "INCOME" to "Income",
                            "SAVINGS" to "Savings"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTypeFilter == key,
                                onClick = { selectedTypeFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filter by Category:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All") }
                        )
                        uiState.categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.name,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == cat.name) null else cat.name
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Filtered Summary & Chart
        item {
            IncomeVsExpensesChartCard(
                totalIncome = filteredIncome,
                totalExpenses = filteredExpenses,
                currentSavings = filteredSavings,
                currencyFormat = currency
            )
        }

        // 5. Filtered Transaction Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Matching Records (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Net Expense: ${FinanceCalculator.formatCurrency(filteredExpenses, currency)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ExpenseRedDark
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Matching Transactions",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Try switching the time period filter or add a new income/expense record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredTransactions,
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
